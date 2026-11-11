<!-- SPDX-License-Identifier: Apache-2.0 OR MIT -->

# Policies

## Toolchain

- The language floor is **Java 17** (`maven.compiler.release` 17). CI
  builds and tests on Temurin 17 and 21.
- The floor moves only in a release whose changelog says so; the
  maintainer decides when.
- `./mvnw` runs Maven 3.10.0, checked against its SHA-256. Any Maven
  3.9.0 or newer also builds the project (the enforcer plugin rejects
  older ones, which cannot verify `.mvn/checksums/`). Every plugin is
  pinned in `pom.xml`, and the enforcer fails the build on an unpinned
  plugin or a dynamic dependency version.

## Versioning

- Versions are `0.0.x` and move by `0.0.1`, in lockstep with the pain001
  suite. SDK 0.0.N vendors the conformance kit from core 0.0.N.

## Dependencies

- One runtime dependency: Jackson databind, pinned to an exact version.
  JUnit Jupiter is test-only. Dependabot proposes updates weekly.
- Maven verifies every resolved artifact (dependencies and plugins)
  against the SHA-256 recorded in `.mvn/checksums/`, and fails on one
  that is missing or different. `make lock` re-records the file; a
  Dependabot bump needs that commit before its CI passes.
- CI audits the resolved dependencies with OSV-Scanner (`make audit`)
  on every push and weekly.
