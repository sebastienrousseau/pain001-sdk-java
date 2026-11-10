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
  <a href="#documentation"><img src="https://img.shields.io/badge/docs-javadoc-blue?style=for-the-badge&labelColor=555555&logo=readthedocs" alt="Docs" height="28" /></a>
  <a href="https://scorecard.dev/viewer/?uri=github.com/sebastienrousseau/pain001-sdk-java"><img src="https://img.shields.io/ossf-scorecard/github.com/sebastienrousseau/pain001-sdk-java?style=for-the-badge&label=OpenSSF%20Scorecard&logo=openssf" alt="OpenSSF Scorecard" height="28" /></a>
  <a href="LICENSE"><img src="https://img.shields.io/badge/license-Apache--2.0%20OR%20MIT-blue.svg?style=for-the-badge" alt="License: Apache-2.0 OR MIT" height="28" /></a>
  <a href="https://github.com/sebastienrousseau/pain001-sdk-java/blob/main/docs/POLICIES.md"><img src="https://img.shields.io/badge/Java-17%2B-93450a.svg?style=for-the-badge&logo=openjdk" alt="Java 17 or newer" height="28" /></a>
</p>

<p align="center">
  No demo recording: this is a library with no terminal interface of its own, and every call needs a running server. <code>make example</code> is the runnable demonstration.
</p>

---

## Contents

**Getting started**

- [Install](#install): Maven coordinates (not yet on Maven Central)
- [Requirements](#requirements): toolchain floor, platforms
- [Quick Start](#quick-start): generate a pain.001 file from a CSV in one call

**The pain001-sdk-java ecosystem**

- [The pain001-sdk-java ecosystem](#the-pain001-sdk-java-ecosystem): the pain001 server, its sibling SDKs and the shared conformance kit

**Library reference**

- [Capabilities at a glance](#capabilities-at-a-glance): the current surface by theme
- [Ecosystem comparison](#ecosystem-comparison): what this client is and is not
- [Benchmarks](#benchmarks): none measured, and why
- [Features](#features): the client's methods and types
- [Configuration](#configuration): base URL, API key, timeout
- [Examples](#examples): runnable example index

**Operational**

- [When not to use pain001-sdk-java](#when-not-to-use-pain001-sdk-java): limitations
- [Development](#development): make targets, CI
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
  <version>0.0.86</version>
</dependency>
```

The package is not yet on Maven Central; the coordinates above are the
ones the first release (0.0.86, in lockstep with the pain001 suite) will
use. Until then, build it from source and install it into your local
Maven repository:

```sh
git clone https://github.com/sebastienrousseau/pain001-sdk-java
cd pain001-sdk-java
mvn -B install
```

The one runtime dependency is Jackson databind (`com.fasterxml.jackson.core:jackson-databind`).

---

## Requirements

- Java 17 or newer; CI builds and tests on Temurin 17 and 21 on Linux
  ([toolchain policy](docs/POLICIES.md)).
- Maven 3.6.3 or newer to build from source.
- A running pain001 server (`pain001 serve`, from `pip install 'pain001[api]'`)
  to call. This SDK is a client: the generation and validation happen in
  the server.

---

## Quick Start

```java
import com.pain001.sdk.GenerateResponse;
import com.pain001.sdk.Pain001Client;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;

Pain001Client client = Pain001Client.builder()
    .baseUrl("http://127.0.0.1:8000")
    .timeout(Duration.ofSeconds(30))
    .build();

String csv = Files.readString(Path.of("payments.csv"));
GenerateResponse result = client.generate(csv, "payments.csv", "pain.001.001.09");
Files.writeString(Path.of("payments.xml"), result.xml());
```

Start the server first (`pain001 serve --host 127.0.0.1 --port 8000`).
`generate` posts the file's text to `/api/v1/ui/generate` and returns the
XML; if the server rejects the input, it raises `Pain001ApiException`
with the HTTP status and the server's `detail`.

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
| Sibling SDKs (Go, Rust, .NET, C++) | Typed clients for the same API, proven by the same kit | Services in those languages |
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
| Errors | `Pain001ApiException` with `status()` and the parsed `detail()` | Supported |
| Async jobs | `/api/v1/generate/async`, `/api/v1/status/{id}`, `/api/v1/download/{id}` | Reachable through `request` only |
| Retries | Automatic retry of failed requests | Not provided, by design |

---

## Ecosystem comparison

This SDK is a client for the pain001 server, not a standalone ISO 20022
library: it holds no schemas and runs no validation itself. No
comparison with Java ISO 20022 libraries has been made.

| Project | Runs the ISO 20022 logic | Needs a pain001 server | Runtime dependencies |
| :--- | :---: | :---: | :---: |
| **pain001-sdk-java** | No (the server does) | Yes | Jackson databind |

There is no `docs/COMPARISON.md` yet, because there is no comparison to record.

---

## Benchmarks

None measured. Each call is one HTTP round trip to the server, so the
time is the server's work plus the network; the client adds JSON
serialisation of the request and response. Measure against your own
server and data.

| Scenario | Result | Environment |
| :--- | ---: | :--- |
| Not measured | n/a | n/a |

There is no `docs/BENCHMARKS.md` yet, because there are no results.

---

## Features

- **One client, immutable and thread-safe.** `Pain001Client` wraps one
  `java.net.http.HttpClient` (HTTP/1.1, no redirects followed).
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
  The exception message names only the status, method and path.
- **Transport failures** (connection refused, timeout) raise
  `java.io.UncheckedIOException`.
- **No retries and no logging.** The client never logs request or
  response bodies; they hold payment data.

---

## Configuration

| Builder method | Default | Effect |
| :--- | :--- | :--- |
| `baseUrl(String)` | required | The server, for example `http://127.0.0.1:8000`; http or https, no query or fragment; a trailing slash is ignored |
| `apiKey(String)` | none | Sent as `Authorization: Bearer <key>` |
| `timeout(Duration)` | 30 seconds | Connect timeout and per-request timeout |

---

## Examples

- [`examples/GenerateCreditTransfer.java`](examples/GenerateCreditTransfer.java):
  generates a pain.001.001.09 file from the conformance kit's synthetic
  credit-transfer CSV. Run `make example` with a server listening at
  `PAIN001_BASE_URL` (default `http://127.0.0.1:8000`); it writes
  `target/credit-transfer.xml`. CI runs it against a live server.

---

## When not to use pain001-sdk-java

- **You cannot run the pain001 server.** Every call goes to it; nothing
  works offline.
- **You need the async job endpoints as typed methods.** They are
  reachable only through `request`.
- **You need automatic retries.** The client sends each request once;
  wrap it in your own retry policy if your calls are idempotent.
- **You are on Java 11 or older.** The floor is Java 17.

---

## Development

```bash
git clone https://github.com/sebastienrousseau/pain001-sdk-java
cd pain001-sdk-java
make check                 # format, file size, mvn verify, example compile

# Conformance kit and typed tests against a live server
pip install 'pain001[api]==0.0.85'
pain001 serve --host 127.0.0.1 --port 8000 &
PAIN001_BASE_URL=http://127.0.0.1:8000 make conformance
```

`make check` runs a whitespace format check, the 500-line file limit and
`mvn -B verify`: javac with `-Xlint:all -Werror`, the unit tests (a stub
HTTP server, no network) and PMD with the suite's complexity ceilings
(cyclomatic 10, cognitive 15, 60 statements per method). The conformance
and typed tests skip unless `PAIN001_BASE_URL` is set. CI runs the build
on Java 17 and 21, then the full kit against `pain001 serve` installed
from the hash-pinned `ci/server-requirements.txt`. See
[`CONTRIBUTING.md`](CONTRIBUTING.md).

---

## Security

The client sends what you give it to the server you configure, over
http or https, and does not follow redirects. It writes no files, logs
nothing, and keeps the API key out of exception messages. Use https and
an API key for any server that is not on loopback. The test data in
`conformance/` is synthetic.

Report vulnerabilities according to [`SECURITY.md`](SECURITY.md).

---

## Documentation

[Javadoc in the sources](src/main/java/com/pain001/sdk/package-info.java) (no rendered site is published yet) ·
[Conformance kit](conformance/README.md) ·
[Developer guide](CONTRIBUTING.md) ·
[Policies](docs/POLICIES.md) ·
[Changelog](CHANGELOG.md) ·
[Agent rules](AGENTS.md)

---

## Stability guarantees

The project is pre-1.0 and versions advance in `0.0.1` steps, in lockstep
with the pain001 suite: SDK 0.0.N vendors the conformance kit from core
0.0.N and is tested against that server. The breaking axis is the public
API of `com.pain001.sdk` (method names, record components, exception
types); the changelog calls out any change to it. The Java floor moves
only as described in the [toolchain policy](docs/POLICIES.md).

---

## License

This project is dual-licensed under either:

- Apache License, Version 2.0 ([`LICENSE-APACHE`](LICENSE-APACHE) or <https://www.apache.org/licenses/LICENSE-2.0>)
- MIT License ([`LICENSE-MIT`](LICENSE-MIT) or <https://opensource.org/licenses/MIT>)

at your option. Dependencies keep their own licences.
