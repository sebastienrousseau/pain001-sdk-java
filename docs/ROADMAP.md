<!-- SPDX-License-Identifier: Apache-2.0 OR MIT -->

# Roadmap

The SDK follows the pain001 suite: each suite release is a release of
this client, at the same version. What comes next is driven by what the
server adds and what the conformance kit pins.

## 0.0.88 (this release)

- First publication to Maven Central as `com.pain001:pain001-sdk`,
  signed, with a CycloneDX SBOM, SHA256SUMS and build provenance.
- Opt-in `RetryPolicy` for `GET` calls; `NoDocumentException` for a 2xx
  `generate` without XML; a `User-Agent` naming the SDK version.
- 100 % line and branch coverage enforced, JMH benchmarks for every
  method, one example per method, a rendered manual.

## Next

- **Typed async jobs.** `/api/v1/generate/async`, `/api/v1/status/{id}`
  and `/api/v1/download/{id}` are reachable only through `request`. They
  become typed methods once the kit has cases for them.
- **API breakage check against a release.** japicmp is wired and starts
  comparing once 0.0.88 is on Maven Central.
- **Java floor.** The floor stays at Java 17 while it is a supported LTS
  for the major distributions; a raise is announced one release ahead in
  the changelog ([policy](POLICIES.md)).

## Not planned

- Payment logic in Java (schemas, rulebooks, XML generation): that is
  the server's job ([ADR 0001](adr/0001-thin-client-over-the-server.md)).
- Retrying `POST` calls automatically ([ADR 0004](adr/0004-opt-in-retries.md)).
