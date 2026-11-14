<!-- SPDX-License-Identifier: Apache-2.0 OR MIT -->

<p align="center">
  <img src="https://cloudcdn.pro/pain001/v1/logos/pain001.svg" alt="pain001-sdk-java logo" width="128" />
</p>

<h1 align="center">pain001-sdk-java</h1>

<p align="center">
  A typed Java client for the pain001 REST API, which generates and validates ISO 20022 pain.001 and pain.008 payment files.
</p>

<p align="center">
  <a href="https://github.com/sebastienrousseau/pain001-sdk-java/actions"><img src="https://img.shields.io/github/actions/workflow/status/sebastienrousseau/pain001-sdk-java/ci.yml?style=for-the-badge&logo=github" alt="Build" height="28" /></a>
  <a href="https://central.sonatype.com/artifact/com.pain001/pain001-sdk"><img src="https://img.shields.io/maven-central/v/com.pain001/pain001-sdk?style=for-the-badge&color=fc8d62&logo=apachemaven" alt="Registry" height="28" /></a>
  <a href="https://sebastienrousseau.github.io/pain001-sdk-java/api/"><img src="https://img.shields.io/badge/docs-javadoc-blue?style=for-the-badge&labelColor=555555&logo=readthedocs" alt="Docs" height="28" /></a>
  <a href="https://scorecard.dev/viewer/?uri=github.com/sebastienrousseau/pain001-sdk-java"><img src="https://img.shields.io/ossf-scorecard/github.com/sebastienrousseau/pain001-sdk-java?style=for-the-badge&label=OpenSSF%20Scorecard&logo=openssf" alt="OpenSSF Scorecard" height="28" /></a>
  <a href="LICENSE"><img src="https://img.shields.io/badge/license-Apache--2.0%20OR%20MIT-blue.svg?style=for-the-badge" alt="License: Apache-2.0 OR MIT" height="28" /></a>
  <a href="https://github.com/sebastienrousseau/pain001-sdk-java/blob/main/docs/POLICIES.md"><img src="https://img.shields.io/badge/Java-17%2B-93450a.svg?style=for-the-badge&logo=openjdk" alt="Java 17 or newer" height="28" /></a>
</p>

<p align="center">
  <img src="https://raw.githubusercontent.com/sebastienrousseau/pain001-sdk-java/main/.github/demo.gif" alt="The GenerateCreditTransfer example calling a local pain001 server: health check, validation of the synthetic credit-transfer CSV, generation of a pain.001.001.09 file, then the first lines of that XML" width="100%" />
</p>

---

## Contents

**Getting started**

- [Install](#install): Maven Central (`com.pain001:pain001-sdk`), Gradle, or from source
- [Requirements](#requirements): toolchain floor, platforms
- [Quick Start](#quick-start): generate a pain.001 file from a CSV in one call

**The pain001-sdk-java ecosystem**

- [The pain001-sdk-java ecosystem](#the-pain001-sdk-java-ecosystem): the pain001 server, its sibling SDKs and the shared conformance kit

**Library reference**

- [Capabilities at a glance](#capabilities-at-a-glance): the current surface by theme
- [Ecosystem comparison](#ecosystem-comparison): short matrix; full table at [`docs/COMPARISON.md`](docs/COMPARISON.md)
- [Benchmarks](#benchmarks): headline numbers; full table at [`docs/BENCHMARKS.md`](docs/BENCHMARKS.md)
- [Features](#features): the client's methods and types
- [Configuration](#configuration): base URL, API key, timeout, retries
- [Examples](#examples): runnable example index

**Operational**

- [When not to use pain001-sdk-java](#when-not-to-use-pain001-sdk-java): limitations
- [Development](#development): make targets, fuzzing, CI
- [Security](#security): guarantees and compliance
- [Documentation](#documentation): all reference docs
- [Stability guarantees](#stability-guarantees): SemVer axis, output stability, minimum toolchain discipline
- [License](#license)

---

## Install

### As a Java library

```xml
<dependency>
  <groupId>com.pain001</groupId>
  <artifactId>pain001-sdk</artifactId>
  <version>0.0.88</version>
</dependency>
```

Gradle (Kotlin DSL):

```kotlin
implementation("com.pain001:pain001-sdk:0.0.88")
```

0.0.88 is the first release published to Maven Central, in lockstep
with the pain001 suite. Each release is signed with GPG, ships a
CycloneDX SBOM and `SHA256SUMS` on its GitHub release, and carries a
build provenance attestation ([`docs/packaging.md`](docs/packaging.md)).

From source, into your local Maven repository:

```sh
git clone https://github.com/sebastienrousseau/pain001-sdk-java
cd pain001-sdk-java
./mvnw -B install
```

The one runtime dependency is Jackson databind (`com.fasterxml.jackson.core:jackson-databind`).

---

## Requirements

- Java 17 or newer; CI builds and tests on Temurin 17 and 21 on Linux
  ([toolchain policy](docs/POLICIES.md)).
- Maven 3.9.0 or newer to build from source (`./mvnw` brings 3.10.0).
- A running pain001 server (`pain001 serve`, from `pip install 'pain001[api]'`)
  to call. This SDK is a client: the generation and validation happen in
  the server. CI tests against pain001 0.0.87, the latest released core.

---

## Quick Start

```java
import com.pain001.sdk.GenerateResponse;
import com.pain001.sdk.GenerationRefusedException;
import com.pain001.sdk.Pain001Client;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;

Pain001Client client = Pain001Client.builder()
    .baseUrl("http://127.0.0.1:8000")
    .timeout(Duration.ofSeconds(30))
    .build();

String csv = Files.readString(Path.of("payments.csv"));
try {
    GenerateResponse result = client.generate(csv, "payments.csv", "pain.001.001.09");
    Files.writeString(Path.of("payments.xml"), result.xml());
} catch (GenerationRefusedException refused) {
    GenerateResponse findings = refused.response();
    findings.validationErrors().forEach(e -> System.err.println(e.field() + ": " + e.message()));
    findings.schemeViolations().forEach(v -> System.err.println(v.path("rule").asText()));
}
```

Start the server first (`pain001 serve --host 127.0.0.1 --port 8000`).
`generate` posts the file's text to `/api/v1/ui/generate` and returns the
XML. When the server refuses the data (rows that fail validation, or a
scheme rule), it answers HTTP 422 and `generate` raises
`GenerationRefusedException`, whose `response()` holds the findings.
Any other rejection raises `Pain001ApiException` with the HTTP status and
the server's `detail`.

---

## The pain001-sdk-java ecosystem

pain001 has one engine, the Python server, and thin typed clients per
language. Every SDK vendors the same conformance kit from core
(`conformance/`) and runs every case through its own client in CI, so
the clients agree on what the server does.

| Component | Purpose | Use case |
| :--- | :--- | :--- |
| [`pain001`](https://github.com/sebastienrousseau/pain001) | Generates and validates ISO 20022 payment files; `pain001 serve` is the REST API this SDK calls | The engine behind every client |
| **`pain001-sdk-java`** | Typed Java client for the REST API | JVM services that produce or check payment files |
| [`pain001-sdk-go`](https://github.com/sebastienrousseau/pain001-sdk-go), [`-rust`](https://github.com/sebastienrousseau/pain001-sdk-rust), [`-dotnet`](https://github.com/sebastienrousseau/pain001-sdk-dotnet), [`-cpp`](https://github.com/sebastienrousseau/pain001-sdk-cpp) | Typed clients for the same API, proven by the same kit | Services in those languages |
| [`pain001-mcp`](https://github.com/sebastienrousseau/pain001-mcp) | MCP server exposing pain001 validation, generation and inspection | Agent workflows over payment files |
| [`pain001-lsp`](https://github.com/sebastienrousseau/pain001-lsp) | Language server for pain001 input files | Editor support |

---

## Capabilities at a glance

| Area | Capability | Status |
| :--- | :--- | :--- |
| Health | `health()`: server status and version | Supported |
| Upload files | `validate(...)` and `generate(...)` for CSV, JSON and JSONL payment files, with optional scheme rulebooks | Supported |
| Documents | `check(...)`: XSD, MDR, scheme and Schematron rungs on a pain.001 or pain.008 document | Supported |
| Faces | `faces()` and `projectFace(...)`: project a pain.001 document onto Berlin Group, ISO 2018 names or UK Open Banking, with a loss report | Supported |
| Raw calls | `request(method, path, body)`: any endpoint, any status | Supported |
| Errors | `Pain001ApiException` with `status()` and the parsed `detail()`; `GenerationRefusedException` with the refused generation's findings | Supported |
| Async jobs | `/api/v1/generate/async`, `/api/v1/status/{id}`, `/api/v1/download/{id}` | Reachable through `request` only |
| Retries | `RetryPolicy`: opt-in retries of `GET` calls on connection failures, 429 and 503, honouring `Retry-After` | Supported, off by default |

---

## Ecosystem comparison

The suite's five SDKs share one method surface and run core's conformance kit against a live `pain001 serve` in CI. Core's CI also generates a Python client from the OpenAPI document, which it imports but does not run against the kit.

| Project | Language (minimum) | Conformance kit in CI | Types |
| :--- | :---: | :---: | :---: |
| pain001-sdk-go | Go 1.26 | Yes | Hand-written |
| **pain001-sdk-java** | **Java 17** | **Yes** | **Hand-written** |
| pain001-sdk-rust | Rust 1.85 | Yes | Hand-written (serde) |
| pain001-sdk-dotnet | .NET 10 | Yes | Hand-written |
| pain001-sdk-cpp | C++17 | Yes | Hand-written |
| openapi-generator client (core CI artifact) | Python | No, CI imports it only | Generated |
| Raw HTTP | Any | No | None |

See [`docs/COMPARISON.md`](docs/COMPARISON.md) for the evidence and complete matrix.

---

## Benchmarks

JMH measures every public method two ways: the client's own cost with no
network (building each request, decoding each response, from the kit's
fixtures) and a full round trip to `pain001 serve` on loopback. The
client's share is microseconds; the time a caller waits is the server's.
These were taken on a shared, heavily loaded machine, so treat them as
rough.

| Scenario | Result | Environment |
| :--- | ---: | :--- |
| Build the `validate` request (5-row kit CSV) | 17.0 µs ± 9.6 | Apple A18 Pro, Temurin 21.0.13, JMH 1.37 |
| Decode the `generate` answer (4.5 KB) | 27.0 µs ± 12.5 | same |
| Decode the `projectFace` answer (5.6 KB) | 58.2 µs ± 21.2 | same |
| `health()` round trip | 4.9 ms ± 6.6 | same, pain001 0.0.87 on 127.0.0.1 |
| `validate(...)` round trip | 26.8 ms ± 51.7 | same |
| `generate(...)` round trip | 375 ms ± 543 | same |

See [`docs/BENCHMARKS.md`](docs/BENCHMARKS.md) for methodology and full results.

---

## Features

- **One client, immutable and thread-safe.** `Pain001Client` wraps one
  `java.net.http.HttpClient` (HTTP/1.1, no redirects followed) and sends
  `User-Agent: pain001-sdk-java/<version>` (`Pain001Client.VERSION`).
- **Typed responses as records:** `HealthResponse`, `ValidationResponse`,
  `GenerateResponse` (row errors as `RowError`: field, message, value), `CheckResponse` (with `CheckFinding`), `FaceInfo`,
  `FaceResponse` (with `LossReport` and the target payload as a Jackson
  `JsonNode`), and `ApiResponse` for raw calls.
- **Message types as strings,** with `MessageType` constants for the 13
  types the server lists at this version, so a newer server's types work
  without a new SDK.
- **Errors carry the server's words.** A non-2xx response raises
  `Pain001ApiException`: `status()` is the HTTP status and `detail()` the
  parsed `detail`, a string for most errors and an array for HTTP 422.
  A refused `generate` (HTTP 422, `success: false`) raises the subclass
  `GenerationRefusedException`, whose `response()` is the decoded
  `GenerateResponse` with its `validationErrors()` and
  `schemeViolations()`. The exception message names only the status,
  method and path.
- **Transport failures** (connection refused, timeout) raise
  `java.io.UncheckedIOException`. A 2xx from `generate` without the XML
  (a server older than 0.0.87 answers a refusal with 200) raises its
  subclass `NoDocumentException`, so a returned `GenerateResponse`
  always carries the document.
- **Retries are opt-in and `GET` only.** `RetryPolicy` retries `GET`
  calls on connection failures, timeouts, 429 and 503, with exponential
  backoff and jitter, and honours `Retry-After`. Without one, each
  request is sent once.
- **No logging.** The client never logs request or response bodies;
  they hold payment data.

---

## Configuration

| Builder method | Default | Effect |
| :--- | :--- | :--- |
| `baseUrl(String)` | required | The server, for example `http://127.0.0.1:8000`; http or https, no query or fragment; a trailing slash is ignored |
| `apiKey(String)` | none | Sent as `Authorization: Bearer <key>` |
| `timeout(Duration)` | 30 seconds | Connect timeout and per-request timeout |
| `retryPolicy(RetryPolicy)` | none | Retries `GET` calls (`health`, `faces`, `request` with `GET`) on connection failures, timeouts, 429 and 503; never `POST` |

---

## Examples

Each file in [`examples/`](examples/) is a runnable program for one
part of the API. `make examples` compiles them against the built SDK and
runs every one against the server at `PAIN001_BASE_URL`; CI does the
same in its conformance job, so they cannot rot.

| Example | Shows |
| :--- | :--- |
| [`Health.java`](examples/Health.java) | `health()`: server status and version |
| [`Validate.java`](examples/Validate.java) | `validate(...)`: a valid file, a scheme violation (`uk-chaps`), and a 400 for a bad IBAN |
| [`GenerateCreditTransfer.java`](examples/GenerateCreditTransfer.java) | `health`, `validate` then `generate(...)`: writes `target/credit-transfer.xml` (the README demo) |
| [`Check.java`](examples/Check.java) | `check(...)`: XSD and MDR rungs on an existing document |
| [`Faces.java`](examples/Faces.java) | `faces()`: the target standards and their bindings |
| [`ProjectFace.java`](examples/ProjectFace.java) | `projectFace(...)`: Berlin Group projection and its loss report |
| [`RawRequest.java`](examples/RawRequest.java) | `request(...)`: any endpoint, any status, no exception |
| [`ErrorHandling.java`](examples/ErrorHandling.java) | `GenerationRefusedException`, `Pain001ApiException`, `NoDocumentException` and `UncheckedIOException`, in catch order |
| [`Timeouts.java`](examples/Timeouts.java) | `timeout(...)` and `RetryPolicy` against an unreachable port, then the real server |
| [`ApiKey.java`](examples/ApiKey.java) | `apiKey(...)`: the bearer token, read from `PAIN001_API_KEY` |

---

## When not to use pain001-sdk-java

- **You cannot run the pain001 server.** Every call goes to it; nothing
  works offline.
- **You need the async job endpoints as typed methods.** They are
  reachable only through `request`.
- **You need `POST` calls retried.** `RetryPolicy` retries `GET` calls
  only; `validate`, `generate`, `check` and `projectFace` are sent once.
  Wrap them yourself if a duplicate request is harmless to you.
- **You are on Java 11 or older.** The floor is Java 17.

---

## Development

```bash
git clone https://github.com/sebastienrousseau/pain001-sdk-java
cd pain001-sdk-java
make check          # versions, format, file size, mvn verify (tests, 100 % coverage, PMD), examples compile
make regression     # installs the pinned server, then the gate, conformance kit, examples, bench smoke
make bench          # JMH: client-side, plus loopback with PAIN001_BASE_URL set
make fuzz           # each Jazzer target for 60 s
make manual         # MkDocs strict build with the Javadoc
make docs-lint      # markdownlint, codespell, REUSE, lychee
```

`make check` runs everything CI's build job runs, without a server:
`javac -Xlint:all -Werror`, the unit tests and fuzz seeds, JaCoCo at
100 % line and branch coverage, and PMD with the suite's complexity
ceilings (cyclomatic 10, cognitive 15, 60 statements per method).
`make regression` adds a real `pain001 serve` on a free loopback port,
installed from the hash-pinned `ci/server-requirements.txt`, and is what
the CI conformance job runs. Every gate and how to reproduce it is in
[`DEVELOPMENT.md`](DEVELOPMENT.md).

---

## Security

Report a vulnerability privately first, through GitHub's private
vulnerability reporting (see below); never in a public issue.

The client sends what you give it to the server you configure, over
http or https, and does not follow redirects, so an API key never
reaches another host. It writes no files, logs nothing, and keeps
request and response content out of exception messages. Use https and
an API key for any server that is not on loopback. The test data in
`conformance/` is synthetic.

Responses are untrusted input. Two Jazzer fuzz targets
(`src/fuzz/java/`) feed arbitrary bytes to the response decoding and
answer every typed call with an arbitrary status and body; their
committed seeds, including every past finding, replay in every build,
and CI fuzzes each target for 60 seconds on every pull request. The
project is not in OSS-Fuzz. Dependencies are pinned by SHA-256
(`.mvn/checksums/`) and audited with OSV-Scanner on every push.

---

## Documentation

[User manual](https://sebastienrousseau.github.io/pain001-sdk-java/) ·
[API reference (Javadoc)](https://sebastienrousseau.github.io/pain001-sdk-java/api/) ·
[Developer docs](DEVELOPMENT.md) ·
[Ecosystem map](docs/COMPARISON.md)

Also: [Architecture](docs/ARCHITECTURE.md) ·
[Decisions](docs/adr/README.md) ·
[Benchmarks](docs/BENCHMARKS.md) ·
[Packaging](docs/packaging.md) ·
[Roadmap](docs/ROADMAP.md) ·
[Policies](docs/POLICIES.md) ·
[Conformance kit](conformance/README.md) ·
[Changelog](CHANGELOG.md) ·
[Governance](GOVERNANCE.md) ·
[Support](SUPPORT.md)

---

## Stability guarantees

The project is pre-1.0 and versions advance in `0.0.1` steps, in lockstep
with the pain001 suite: SDK 0.0.N vendors the conformance kit from core
0.0.N. The breaking axis is the public API of `com.pain001.sdk` (method
names and signatures, record components, exception types and their
hierarchy); the changelog calls out any change to it, and a japicmp
check in CI compares each build with the last release on Maven Central.
The client sends the request shapes the kit pins; a change to what it
sends or to which exception a given answer raises is a breaking change
even when no signature moves. The Java floor moves only as described in
the [toolchain policy](docs/POLICIES.md), announced in the changelog.

---

## License

This project is dual-licensed under either:

- Apache License, Version 2.0 ([`LICENSE-APACHE`](LICENSE-APACHE) or <https://www.apache.org/licenses/LICENSE-2.0>)
- MIT License ([`LICENSE-MIT`](LICENSE-MIT) or <https://opensource.org/licenses/MIT>)

at your option. Dependencies keep their own licences.
