<!-- SPDX-License-Identifier: Apache-2.0 OR MIT -->

# ADR 0003: The error model

- Status: Accepted
- Date: 2026-11-14

## Context

A typed call can fail four ways: the server answers with an error status;
the server refuses the payment data (since pain001 0.0.87, `generate`
answers HTTP 422 with `success: false` and the findings); a 2xx answer
from `generate` carries no document (a server older than 0.0.87 answers a
refusal with 200); or no answer arrives. Callers need to tell these
apart, and an exception that reaches a log must never carry payment data.

## Decision

All exceptions are unchecked.

| Exception | When | Carries |
| :--- | :--- | :--- |
| `Pain001ApiException` | any non-2xx | `status()`, the parsed `detail()` |
| `GenerationRefusedException` (extends the above) | `generate` answered 422 with `success: false` | `response()`: the decoded findings |
| `NoDocumentException` (extends `UncheckedIOException`) | a 2xx from `generate` without the XML | `status()` |
| `java.io.UncheckedIOException` | connection failure, timeout, interrupt | the `IOException` cause |

Every message names only the status, method and path, never request or
response content. `request(method, path, body)` raises only for transport
failures; it returns any status. The same four cases exist in every SDK
of the suite (Go `*APIError`, `*RefusalError`, `ErrNoDocument`; C++
`ApiError`, `GenerationRefused`, `NoDocument`).

## Consequences

- `catch (Pain001ApiException e)` still catches a refusal; callers that
  want the findings catch `GenerationRefusedException` first.
- A returned `GenerateResponse` always holds the XML.
- Checked exceptions were rejected: every call can fail on the network,
  and wrapping every call site adds no information.
