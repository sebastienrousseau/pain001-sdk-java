<!-- SPDX-License-Identifier: Apache-2.0 OR MIT -->

# ADR 0004: Opt-in retries, GET only

- Status: Accepted
- Date: 2026-11-14

## Context

Until 0.0.88 the client sent every request once. Behind a load balancer
or during a server restart, callers see connection failures and 503s
that a second attempt clears, and each wrote their own loop. A retry is
only safe when sending the request twice cannot do anything twice.

## Decision

`RetryPolicy`, set with `Pain001Client.Builder.retryPolicy`, is off by
default. With it, a `GET` (`health`, `faces`, `request` with `GET`) is
sent again after a connection failure or timeout, or an HTTP 429 or 503,
up to `maxAttempts` (default 3). The wait is exponential with jitter:
`d = min(maxBackoff, initialBackoff * 2^(n-1))`, then a random point in
`[d/2, d]` (defaults 200 ms and 5 s). A `Retry-After` header on 429 or 503
(seconds or an HTTP date) is waited exactly; when it asks for longer than
`maxBackoff` the client returns that answer instead of retrying early.
`POST` is never retried, whatever the policy.

## Consequences

- Behaviour without a policy is unchanged.
- `validate`, `generate`, `check` and `projectFace` are not retried even
  though the server does not store anything for them: the SDK cannot know
  what a proxy or a future server version does with a duplicate, so the
  caller decides.
- 502 and 504 are not retried; the spec for the suite names 429, 503 and
  connection errors, and every SDK retries the same set.
- Tests inject the sleeper, random source and clock, so every wait is
  checked exactly without sleeping.
