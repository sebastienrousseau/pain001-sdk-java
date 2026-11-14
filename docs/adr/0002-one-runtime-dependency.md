<!-- SPDX-License-Identifier: Apache-2.0 OR MIT -->

# ADR 0002: One runtime dependency

- Status: Accepted
- Date: 2026-11-10 (recorded 2026-11-14)

## Context

A payments library is pulled into regulated services, where every
transitive dependency is something to audit and patch. Java 17's
`java.net.http.HttpClient` covers HTTP/1.1, TLS, timeouts and redirects;
the JDK has no JSON parser.

## Decision

Transport is the JDK's `HttpClient`. JSON is Jackson databind, the one
runtime dependency, kept behind the package-private `Json` class; the
public API exposes Jackson only as `JsonNode` for the parts of a response
whose shape the server owns (row values, scheme violations, face
payloads). Test and build tools (JUnit, Jazzer, JaCoCo, PMD, JMH) never
reach the published artifact's runtime scope; JMH is only in the `bench`
profile.

## Consequences

- `jackson-databind` (with `jackson-core` and `jackson-annotations`) is
  the whole dependency tree; Dependabot and the OSV-Scanner audit watch
  it.
- Replacing Jackson would change the public `JsonNode` types, so it is a
  breaking change and would need its own ADR.
- Every artifact Maven resolves, plugins included, is checked against
  `.mvn/checksums/`.
