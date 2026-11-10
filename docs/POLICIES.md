<!-- SPDX-License-Identifier: Apache-2.0 OR MIT -->

# Policies

## Toolchain

- The language floor is **Java 17** (`maven.compiler.release` 17). CI
  builds and tests on Temurin 17 and 21.
- The floor moves only in a release whose changelog says so; the
  maintainer decides when.
- Maven 3.6.3 or newer builds the project; every plugin is pinned in
  `pom.xml`, so the build does not change with the local Maven version.

## Versioning

- Versions are `0.0.x` and move by `0.0.1`, in lockstep with the pain001
  suite. SDK 0.0.N vendors the conformance kit from core 0.0.N.

## Dependencies

- One runtime dependency: Jackson databind, pinned to an exact version.
  JUnit Jupiter is test-only. Dependabot proposes updates weekly.
