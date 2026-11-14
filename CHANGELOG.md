<!-- SPDX-License-Identifier: Apache-2.0 OR MIT -->

# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to the pain001 suite's `0.0.x` versioning: each
release moves by `0.0.1`, in lockstep with the suite. The first release
is 0.0.88, the first published to Maven Central.

## [Unreleased]

### Highlights

- **On Maven Central**: the first release, `com.pain001:pain001-sdk`
  0.0.88, signed, with a CycloneDX SBOM, `SHA256SUMS` and build
  provenance.
- **Opt-in retries**: `RetryPolicy` retries `GET` calls on connection
  failures, 429 and 503, honouring `Retry-After`; `POST` is never
  retried.
- **A returned `generate` always has the XML**: a 2xx without it now
  raises `NoDocumentException` instead of passing silently.
- **Proven, measured and documented**: 100 % line and branch coverage
  enforced, a runnable example and JMH benchmarks for every method, and
  a user manual with the Javadoc.

### Changed

- The Maven release-signing public key is committed as
  `MAVEN-SIGNING-KEY.asc` and documented in `docs/packaging.md`;
  `KEYS.asc` stays the SSH allowed-signers list for commits and tags.
- The version is 0.0.88 everywhere (`pom.xml`, now a release version,
  `CITATION.cff`, `SECURITY.md`, `SUPPORT.md`, the install snippets);
  `scripts/verify-release-versions` fails CI on any mismatch.
- The fuzz tests and their seeds moved from `src/test/` to `src/fuzz/`
  (added as test roots, so Maven runs them as before). OpenSSF
  Scorecard's Fuzzing check skips every path under `src/test/`, so it
  scored 0 although the Jazzer tests ran on every build.
- The CI conformance job runs `make regression`: the full gate, every
  kit case (none skipped), every example and the benchmark smoke run
  against a server on a free loopback port. Every workflow has a
  concurrency group.

- Tests run on JUnit 6.1.3 (was 5.14.4); it needs Java 17, the
  build's existing floor, and no test source changed. Jackson moves
  to 2.23.0 (was 2.22.4). `.mvn/checksums` re-recorded with
  `make lock`.

### Added

- `User-Agent: pain001-sdk-java/<version>` on every request, and the
  public constant `Pain001Client.VERSION`.
- JMH benchmarks (profile `bench`, `src/bench/`): request building and
  response decoding for every typed method, and a loopback round trip
  for every method; `make bench`, and a smoke run in CI.
  `docs/BENCHMARKS.md` has the method, environment and results.
- JaCoCo coverage gate at 100 % line and branch in `mvn verify`.
- The release pipeline (`.github/workflows/release.yml`): on tag
  `v0.0.N` it verifies the tag and versions, builds reproducible jars,
  sources and Javadoc jars, a CycloneDX SBOM and `SHA256SUMS`, attests
  provenance, publishes to Maven Central through the Central Portal
  plugin with GPG signatures, and writes the GitHub release; a
  `workflow_dispatch` dry run publishes nothing. `make release-dry-run`
  builds the same bundle locally.
- An API breakage check (japicmp, `make api-check`) against the last
  release on Maven Central, a no-op until the first one.
- A user manual (MkDocs Material, `make manual`) with the Javadoc
  (doclint all, warnings fail) under `/api/`, deployed to GitHub Pages
  from `main`; `docs/ARCHITECTURE.md`, `docs/COMPARISON.md`,
  `docs/BENCHMARKS.md`, `docs/ROADMAP.md`, `docs/packaging.md`, four
  ADRs, `DEVELOPMENT.md`, `GOVERNANCE.md` and `SUPPORT.md`.
- `make regression` (`scripts/regression.sh`): installs the pinned
  server when `PAIN001_SERVER` is unset, runs everything CI runs
  against it, and always stops it.
- REUSE compliance (`REUSE.toml`, `LICENSES/`), codespell and an
  offline lychee link check in the docs-lint workflow;
  `.editorconfig`, `.pre-commit-config.yaml` and a devcontainer.
- One runnable example per part of the API in `examples/`: `Health`,
  `Validate`, `Check`, `Faces`, `ProjectFace`, `RawRequest`,
  `ErrorHandling`, `Timeouts` and `ApiKey`, beside
  `GenerateCreditTransfer`. `make examples` runs them all against
  `PAIN001_BASE_URL`, and the CI conformance job does so on every push.
- `RetryPolicy`, an opt-in retry policy set with
  `Pain001Client.Builder.retryPolicy`. It retries `GET` calls only
  (`health`, `faces`, and `request` with `GET`) after a connection
  failure, a timeout, or HTTP 429 or 503, up to `maxAttempts` (default
  3), with exponential backoff and jitter (200 ms initial, 5 s maximum
  by default). A `Retry-After` header is honoured exactly, and when it
  asks for longer than `maxBackoff` the answer is returned rather than
  retried early. `POST` is never retried. Without a policy the client
  sends every request once, as before.
- A README demo, `.github/demo.gif`, rendered by `make demo` from
  `.github/demo.tape`: the example program against `pain001 serve` on
  loopback. `make demo` takes the server from `PAIN001_SERVER` or
  `PATH` (see `CONTRIBUTING.md`).
- `examples/GenerateCreditTransfer.java` now also calls `health` and
  `validate` before `generate`, printing one line per step.
- Fuzz tests with Jazzer 0.30.0 (`jazzer-junit`, test scope, running on
  JUnit 6.1.3): `ResponseDecodingFuzzTest` fuzzes the response-body
  parse and every record decoder, and `ClientResponseFuzzTest` fuzzes
  every typed call against a loopback server answering an arbitrary
  status and body, checking the documented exceptions, `detail` and
  message. Their seeds run in `make check`; `make fuzz` fuzzes, and a
  new `Fuzz` workflow fuzzes each target for 60 seconds on every pull
  request. `.mvn/checksums` re-recorded with `make lock`.
- `GenerationRefusedException`, a subclass of `Pain001ApiException`
  that `generate` raises when the server refuses the data (HTTP 422
  with `success: false`, from pain001 0.0.87). Its `response()` is the
  decoded `GenerateResponse`, so the caller gets the row errors and
  scheme violations; `detail()` is JSON null because that body has no
  `detail`. A 200 from `generate` now always carries the XML.
- Conformance kit 2 from pain001 0.0.87: 14 cases, adding
  `generate-refused-rows`, `generate-refused-scheme`,
  `validate-bad-lei` and `validate-chaps-purpose-required`, and the
  fixtures they read.

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
  against `pain001 serve` 0.0.86 from hash-pinned requirements, DCO and
  Markdown lint, and the suite's community files.

### Fixed

- `generate` returned a `GenerateResponse` without the XML when a 2xx
  answer had `success` not true or no `xml`, which is what a server
  older than pain001 0.0.87 sends for a refused file; its Javadoc
  promised the XML. It now raises the new `NoDocumentException`, an
  `UncheckedIOException` (a broken reply, not a refusal), as the Go SDK
  returns `ErrNoDocument` and the C++ SDK throws `NoDocument`. A
  `ClientResponseFuzzTest` seed pins it.
