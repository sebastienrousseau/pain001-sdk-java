<!-- SPDX-License-Identifier: Apache-2.0 OR MIT -->

# Architecture

`pain001-sdk-java` is a thin, typed client for the pain001 REST API
(`pain001 serve`). This page is the map for contributors: what each part
does, how a call flows, and where the guarantees are enforced.

## The shape

```text
caller ──► Pain001Client ──► java.net.http.HttpClient ──► pain001 serve
             │  typed method builds the JSON body (package-private helpers)
             │  request(): send, optional RetryPolicy for GET, parse leniently
             │  checked(): non-2xx ──► Pain001ApiException / GenerationRefusedException
             ▼
           records (HealthResponse, ValidationResponse, GenerateResponse, ...)
             ▲
           Json: the one place Jackson is used (parse, write, lenient field access)
```

There is no payment logic here (ADR [0001](adr/0001-thin-client-over-the-server.md)).
The server generates and validates; the SDK sends and maps.

## Source layout

| Path | What it holds |
| :--- | :--- |
| `src/main/java/com/pain001/sdk/Pain001Client.java` | The client, its builder, every typed method, the raw `request`, the retry loop |
| `src/main/java/com/pain001/sdk/RetryPolicy.java` | The opt-in retry policy: which answers are retried, the backoff, `Retry-After` |
| `src/main/java/com/pain001/sdk/Json.java` | Package-private Jackson wrapper: lenient parse, write, tolerant field readers |
| `src/main/java/com/pain001/sdk/*Response.java`, `RowError`, `CheckFinding`, `FaceInfo`, `LossReport` | Immutable records, each with a package-private `from(JsonNode)` decoder |
| `src/main/java/com/pain001/sdk/*Exception.java` | The error model (ADR [0003](adr/0003-error-model.md)) |
| `src/main/java/com/pain001/sdk/MessageType.java` | Constants for the 13 message types; methods take strings so newer types work |
| `src/test/java/` | Unit tests (stub servers on loopback), the kit runner, live tests |
| `src/fuzz/java/`, `src/fuzz/resources/` | Jazzer fuzz tests and their committed seeds |
| `src/bench/java/`, `src/bench/resources/` | JMH benchmarks (profile `bench`) and the recorded responses they decode |
| `examples/` | One runnable program per method, run against a server in CI |
| `conformance/` | Core's conformance kit, vendored byte for byte |

## A call, step by step

1. A typed method (`validate`, say) builds its JSON body with a
   package-private helper (`filePayload`, `checkPayload`, `facePayload`)
   and calls `request`.
2. `buildRequest` sets `Accept`, `Content-Type` when there is a body,
   `User-Agent: pain001-sdk-java/<version>`, the bearer token when an API
   key is configured, and the per-request timeout. Redirects are never
   followed, so the key never goes to another host.
3. `request` sends it. With a `RetryPolicy` and a `GET`, a connection
   failure, timeout, 429 or 503 is retried after the policy's wait
   (ADR [0004](adr/0004-opt-in-retries.md)); otherwise the first answer
   is final.
4. The body is parsed leniently: empty becomes JSON null, non-JSON (a
   proxy's HTML page) is kept as a JSON string.
5. A typed method raises on a non-2xx status; `generate` also raises
   `GenerationRefusedException` for a 422 refusal and
   `NoDocumentException` for a 2xx without XML. Then the record's
   `from` decoder maps the tree, defaulting absent fields rather than
   failing, so a newer server's extra fields do not break an older SDK.

## Where the guarantees are enforced

| Guarantee | Enforced by |
| :--- | :--- |
| The client agrees with the server | `ConformanceTest` runs every kit case against a live server in CI |
| Every typed method works end to end | `TypedApiTest` and `make examples` against a live server in CI |
| Hostile responses never escape the error contract | `ClientResponseFuzzTest`, `ResponseDecodingFuzzTest` (seeds in every build, 60 s fuzzing per PR) |
| 100 % line and branch coverage | JaCoCo `check` in `mvn verify` |
| Complexity ceilings | PMD (`config/pmd/complexity.xml`) in `mvn verify` |
| No warnings | `javac -Xlint:all -Werror` |
| Pinned supply chain | `.mvn/checksums/`, the enforcer plugin, actions pinned by SHA |
| Versions agree everywhere | `scripts/verify-release-versions` |

## Extending

A new server endpoint becomes a typed method only once the kit has a case
for it; until then `request` reaches it. Add the method, its record with
a `from` decoder, a unit test against a stub server, a live test in
`TypedApiTest`, an example, and a benchmark in both benchmark classes.
