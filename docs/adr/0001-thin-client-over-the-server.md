<!-- SPDX-License-Identifier: Apache-2.0 OR MIT -->

# ADR 0001: A thin client over the server

- Status: Accepted
- Date: 2026-11-10 (recorded 2026-11-14)

## Context

pain001's value is the payment logic: the ISO 20022 schemas, the MDR and
scheme rulebooks, identifier checks and the projections onto other
standards. It lives in the Python core and is exposed over HTTP by
`pain001 serve`. A Java team wants to call it with types, not to
re-implement it.

## Decision

`pain001-sdk-java` is a client and nothing else. It holds no schemas and
runs no validation: it serialises arguments to JSON, sends them to the
configured server, and maps the answers to records. Correctness is the
server's; agreement between the server and every SDK is proven by the
conformance kit that core publishes in `conformance/`, vendored here byte
for byte (pinned by `conformance/SHA256SUMS`) and run against a live
`pain001 serve` in CI.

## Consequences

- One engine, so a fix or a new rule in core reaches Java users without
  an SDK release, as long as the wire format holds.
- The SDK needs a running server; it cannot work offline. This is stated
  in the README's "When not to use" section.
- The SDK moves in lockstep with the suite. CI runs the vendored kit
  against the latest released core, pinned by hash in
  `ci/server-requirements.txt`.
