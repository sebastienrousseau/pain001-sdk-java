<!-- SPDX-License-Identifier: Apache-2.0 OR MIT -->

# Architecture decision records

Decisions that will be questioned later, with the reason they were
made. Each record is immutable once accepted; a later decision that
changes one supersedes it with a new record.

| ADR | Decision | Status |
| :--- | :--- | :--- |
| [0001](0001-thin-client-over-the-server.md) | A thin client over `pain001 serve`, proven by core's conformance kit | Accepted |
| [0002](0002-one-runtime-dependency.md) | `java.net.http` and Jackson databind, nothing else at runtime | Accepted |
| [0003](0003-error-model.md) | Unchecked exceptions: API error, refusal, no document, transport | Accepted |
| [0004](0004-opt-in-retries.md) | Retries are opt-in and `GET` only | Accepted |
