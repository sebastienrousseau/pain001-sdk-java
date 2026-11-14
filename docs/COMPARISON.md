<!-- SPDX-License-Identifier: Apache-2.0 OR MIT -->

# Ecosystem comparison

The suite's five SDKs share one method surface and run core's conformance kit against a live `pain001 serve` in CI. Core's CI also generates a Python client from the OpenAPI document, which it imports but does not run against the kit.

| Project | Language (minimum) | Conformance kit in CI | Types | Runtime dependencies | Call model |
| :--- | :---: | :---: | :---: | :--- | :--- |
| pain001-sdk-go | Go 1.26 | Yes | Hand-written | None (standard library) | Blocking, `context.Context` |
| **pain001-sdk-java** | **Java 17** | **Yes** | **Hand-written** | **Jackson databind** | **Blocking** |
| pain001-sdk-rust | Rust 1.85 | Yes | Hand-written (serde) | ureq, serde, serde_json, thiserror | Blocking |
| pain001-sdk-dotnet | .NET 10 | Yes | Hand-written | None | async/await, `CancellationToken` |
| pain001-sdk-cpp | C++17 | Yes | Hand-written | libcurl, nlohmann/json | Blocking |
| openapi-generator client (core CI artifact) | Python | No, CI imports it only | Generated | Generator's | Blocking |
| Raw HTTP | Any | No | None | Your HTTP client | Yours |

## Evidence

Each row's claims come from the repository's manifest (language floor,
dependencies) and its CI workflow (the conformance job).

| Project | Manifest | Conformance job |
| :--- | :--- | :--- |
| pain001-sdk-go | [`go.mod`](https://github.com/sebastienrousseau/pain001-sdk-go/blob/main/go.mod) | [`ci.yml`](https://github.com/sebastienrousseau/pain001-sdk-go/blob/main/.github/workflows/ci.yml) |
| **pain001-sdk-java** | [`pom.xml`](https://github.com/sebastienrousseau/pain001-sdk-java/blob/main/pom.xml) (`maven.compiler.release` 17, `jackson-databind` the only runtime dependency) | [`ci.yml`](https://github.com/sebastienrousseau/pain001-sdk-java/blob/main/.github/workflows/ci.yml), job `conformance`: every case, none skipped |
| pain001-sdk-rust | [`Cargo.toml`](https://github.com/sebastienrousseau/pain001-sdk-rust/blob/main/Cargo.toml) | [`ci.yml`](https://github.com/sebastienrousseau/pain001-sdk-rust/blob/main/.github/workflows/ci.yml) |
| pain001-sdk-dotnet | [`Pain001.Sdk.csproj`](https://github.com/sebastienrousseau/pain001-sdk-dotnet/blob/main/src/Pain001.Sdk/Pain001.Sdk.csproj) | [`ci.yml`](https://github.com/sebastienrousseau/pain001-sdk-dotnet/blob/main/.github/workflows/ci.yml) |
| pain001-sdk-cpp | [`CMakeLists.txt`](https://github.com/sebastienrousseau/pain001-sdk-cpp/blob/main/CMakeLists.txt) | [`ci.yml`](https://github.com/sebastienrousseau/pain001-sdk-cpp/blob/main/.github/workflows/ci.yml) |
| openapi-generator client | [pain001 `sdk.yml`](https://github.com/sebastienrousseau/pain001/blob/main/.github/workflows/sdk.yml) | None: generated and imported, not run against the kit |

## This SDK's row

- **Java 17**: `maven.compiler.release` is 17 and the enforcer requires
  Java 17 or newer; CI builds and tests on Temurin 17 and 21.
- **Conformance kit in CI**: the `conformance` job installs the
  hash-pinned server, runs every case of `conformance/cases.json`
  through `Pain001Client.request`, fails if any case is skipped, then
  runs the typed tests and every example.
- **Hand-written types**: Java records with tolerant decoders, not
  generated from `openapi.json`.
- **Jackson databind**: the only runtime dependency
  ([ADR 0002](adr/0002-one-runtime-dependency.md)).
- **Blocking**: each method blocks the calling thread until the answer
  or the timeout; the client is thread-safe, so callers parallelise with
  their own executor.

No comparison with Java ISO 20022 libraries is made: they build or parse
messages in-process, which this client deliberately does not do.
