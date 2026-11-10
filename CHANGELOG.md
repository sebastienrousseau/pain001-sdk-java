<!-- SPDX-License-Identifier: Apache-2.0 OR MIT -->

# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to the pain001 suite's `0.0.x` versioning: each
release moves by `0.0.1`, in lockstep with the suite. The first release
will be 0.0.86.

## [Unreleased]

### Added

- `Pain001Client`, a typed client for the pain001 REST API built on
  `java.net.http.HttpClient`: `health`, `validate`, `generate`, `check`,
  `faces`, `projectFace`, and the raw `request` the conformance runner
  uses. Configured with a builder (base URL, optional API key sent as a
  bearer token, timeout). No retries, no logging of bodies.
- Response records (`HealthResponse`, `ValidationResponse`,
  `GenerateResponse`, `RowError`, `CheckResponse`, `CheckFinding`, `FaceInfo`,
  `FaceResponse`, `LossReport`, `ApiResponse`), `MessageType` constants
  for the 13 message types, and `Pain001ApiException` carrying the HTTP
  status and the parsed `detail`.
- `conformance/`, the SDK conformance kit vendored byte for byte from
  core, pinned by `conformance/SHA256SUMS`; a JUnit runner that executes
  every case through `request`, typed tests per method, and unit tests
  against a stub HTTP server.
- `examples/GenerateCreditTransfer.java`, which writes a pain.001.001.09
  file from the kit's credit-transfer fixture.
- CI on Java 17 and 21 with PMD complexity ceilings, a conformance job
  against `pain001 serve` 0.0.85 from hash-pinned requirements, DCO and
  Markdown lint, and the suite's community files.
