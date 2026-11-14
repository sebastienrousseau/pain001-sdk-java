<!-- SPDX-License-Identifier: Apache-2.0 OR MIT -->

# Development

The single entry point for working on `pain001-sdk-java`: the toolchain,
every CI gate reproduced locally, the test layout and the release model.
[CONTRIBUTING.md](CONTRIBUTING.md) covers the pull-request etiquette, the
conformance kit and the README demo.

## Toolchain

| Tool | Version | Why |
| :--- | :--- | :--- |
| JDK | 17 or newer (CI: Temurin 17 and 21) | The build floor is `maven.compiler.release` 17 |
| Maven | 3.10.0 via `./mvnw` (SHA-256 checked) | Any 3.9.0+ works; older ones cannot verify `.mvn/checksums/` |
| Python | 3.12 with `uv` (or `venv` and `pip`) | The pain001 server for live tests, and MkDocs for the manual |
| GNU Make | any | The task runner |
| Optional | `osv-scanner`, `reuse`, `codespell`, `lychee`, `vhs` | The audit, licence, spelling, link and demo gates |

The [devcontainer](.devcontainer/devcontainer.json) has all of them and
runs `make check` when it is created.

## Every CI gate, locally

| CI job | Local command | What it checks |
| :--- | :--- | :--- |
| CI / Lint and format | `make format-check size lint` and `scripts/verify-release-versions` | No tabs, trailing spaces or CRLF; 500 lines per file; `javac -Xlint:all -Werror`; PMD complexity ceilings; every file states the same version |
| CI / Build and unit tests (Java 17, 21) | `make check` | All of the above plus `mvn verify`: unit tests, Jazzer seeds, JaCoCo 100 % line and branch, PMD; the examples compile |
| CI / Conformance kit against pain001 serve | `make regression` | The full run below |
| CI / Benchmarks smoke | `make bench-smoke` | Every JMH benchmark compiles and runs once; nothing is asserted |
| CI / API breakage | `make api-check` | japicmp against the last release on Maven Central; a no-op until one exists |
| Docs / Build the manual | `make manual` | MkDocs strict build and the Javadoc (`-Xdoclint:all`, warnings fail) |
| Docs lint | `make docs-lint` | markdownlint, codespell, REUSE (SPDX on every file), lychee over every Markdown link |
| Dependency audit | `make audit` | CycloneDX SBOM scanned by OSV-Scanner |
| Fuzz | `make fuzz` | Each Jazzer target for 60 s |
| CodeQL, Scorecard, DCO | (GitHub only) | Static analysis, supply-chain posture, `Signed-off-by` on every commit |
| Release (dry run) | `make release-dry-run` | The release artifacts, signed with a throwaway key, without publishing |

## Regression run

`make regression` runs `scripts/regression.sh`, which is what the CI
conformance job runs:

1. installs the pinned server from `ci/server-requirements.txt` into a
   temporary venv (skip it with `PAIN001_SERVER=/path/to/bin/pain001`);
2. starts it on a free loopback port and waits for `/api/v1/health`;
3. runs `make check` with the live tests on, `make conformance` (every
   kit case, none skipped), `make examples` and `make bench-smoke`;
4. stops the server and deletes the venv, whether the run passed or not.

It is idempotent: run it as often as you like.

## Tests

| Location | What | Needs a server |
| :--- | :--- | :--- |
| `src/test/java/.../Pain001ClientTest`, `BoundaryTest`, `NoDocumentTest` | Request shape, response mapping, error mapping, every guard's edge, against stub servers on loopback | No |
| `src/test/java/.../RetryPolicyTest` | Every retry rule, with an injected sleeper, random source and clock | No |
| `src/test/java/.../ConformanceKitTest`, `KitChecksumTest` | The kit runner, and `conformance/` matching `SHA256SUMS` | No |
| `src/test/java/.../ConformanceTest`, `TypedApiTest` | Every kit case and every typed method against `PAIN001_BASE_URL` | Yes (skipped without) |
| `src/fuzz/java/.../*FuzzTest` | Jazzer: response decoding and the error contract under arbitrary status and body; seeds in `src/fuzz/resources/` run in every build | No |
| `src/bench/java/` | JMH benchmarks (profile `bench`) | Loopback ones only |
| `examples/` | One program per method, each exiting non-zero on a wrong answer | Yes |

The fuzz tests live in `src/fuzz/`, not `src/test/`: Scorecard's
Fuzzing check skips every path under `src/test/`, so Jazzer tests there
are invisible to it. The build adds `src/fuzz/java` and
`src/fuzz/resources` as test roots, so Maven runs them the same way.

### Coverage

JaCoCo measures the unit tests alone (no server) and `mvn verify` fails
below **100 % line and 100 % branch** coverage of `src/main`. The HTML
report is `target/site/jacoco/index.html`. There are no exclusions: no
class, method or line is left out of the measurement.

### Complexity

PMD enforces the suite's ceilings on main and test code in `mvn verify`
(`config/pmd/complexity.xml`): cyclomatic 10 and cognitive 15 per method,
60 statements per method and 500 per class; `make size` enforces 500
lines per file. There is no baseline file because no method is over a
ceiling. Halstead difficulty (ceiling 30) has no maintained Java tool
in the build; it is not measured, and this is the one ceiling not
enforced in CI.

### Lint exceptions

One, in the `bench` profile only: the JMH annotation processor makes
javac's `processing` lint warn that nothing claimed the JUnit and Jazzer
annotations, so that single lint category is off for test sources when
`-Pbench` is active. Every other warning still fails the build.

## Benchmarks

`make bench` runs every benchmark in full (`BENCH_OPTS`, default one
fork, 3 x 1 s warm-up, 5 x 1 s measurement) and writes
`target/jmh.txt`; set `PAIN001_BASE_URL` for the loopback ones, which
are skipped with a message otherwise. `scripts/capture-bench-responses.sh`
re-records the response bodies the decoding benchmarks parse. Results
and method: [docs/BENCHMARKS.md](docs/BENCHMARKS.md).

## Dependencies and checksums

Maven refuses any artifact whose SHA-256 is not in `.mvn/checksums/`.
After changing any dependency or plugin, run `make lock` (it resolves
the main build and the `bench` and `release` profiles from an empty
local repository), spot-check a couple of the new hashes against
`https://repo1.maven.org/maven2/`, and explain the change in the commit
body.

## Release model

- Versions are `0.0.N`, moving by `0.0.1` in lockstep with the pain001
  suite; `scripts/verify-release-versions` fails when any file disagrees
  with `pom.xml`.
- Work for the next version happens on `feat/v<next-version>`, the only
  pull request into `main`.
- Preparing a release: rename `## [Unreleased]` in `CHANGELOG.md` to
  `## [0.0.N] - <date>`, merge, then push the signed annotated tag
  `v0.0.N` with the message `pain001-sdk-java v0.0.N`.
- The tag starts `.github/workflows/release.yml`: it verifies the
  versions and the tag, runs `mvn verify`, builds the jar, sources and
  Javadoc jars, the CycloneDX SBOM and `SHA256SUMS`, attests build
  provenance, writes the GitHub release (title `pain001-sdk-java 0.0.N`,
  Highlights from the changelog, What's Changed, Checksums, Full
  Changelog), then publishes to Maven Central from the `release`
  environment. Re-running it is safe: an existing release is updated,
  not duplicated, and a version already on Central is not uploaded
  again.
- `workflow_dispatch` with `dry_run: true` (the default) runs the same
  build and writes the notes as a workflow artifact, with a throwaway
  GPG key, and publishes nothing.

### What the maintainer must set up (once)

Nothing below is created by any workflow or agent.

| Where | Name | Value |
| :--- | :--- | :--- |
| Settings, Environments | `release` | Required reviewer: the maintainer; deployment branch rule: tags `v*` |
| `release` environment secret | `CENTRAL_TOKEN_USERNAME` | Central Portal user token name (central.sonatype.com, Account, Generate User Token) |
| `release` environment secret | `CENTRAL_TOKEN_PASSWORD` | Central Portal user token password |
| `release` environment secret | `GPG_PRIVATE_KEY` | ASCII-armoured private key that signs the Maven artifacts; its public key is on `keyserver.ubuntu.com` and `keys.openpgp.org` and committed as `MAVEN-SIGNING-KEY.asc` (`KEYS.asc` holds the SSH keys that sign commits and tags) |
| `release` environment secret | `GPG_PASSPHRASE` | That key's passphrase |
| Settings, Pages | Source: GitHub Actions | Lets `docs.yml` deploy the manual (`gh api -X POST repos/sebastienrousseau/pain001-sdk-java/pages -f build_type=workflow`) |

The `com.pain001` namespace is already verified on the Central Portal.
