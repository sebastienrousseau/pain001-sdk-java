<!-- SPDX-License-Identifier: Apache-2.0 OR MIT -->

# Security Policy

## Supported versions

This SDK follows the [`pain001`](https://github.com/sebastienrousseau/pain001)
suite cadence. It has no release yet; the first will be 0.0.87. While
pre-1.0, the latest released `0.0.x` receives security fixes; older
`0.0.x` versions do not.

| Version | Status | Receives security fixes? |
| :--- | :--- | :--- |
| Unreleased (`0.0.87-SNAPSHOT`) | In development | Not yet released |

## Reporting a vulnerability

**Do not open a public issue for security reports.**

Use one of the following private channels, in this order of
preference:

1. **GitHub Private Vulnerability Reporting**
   <https://github.com/sebastienrousseau/pain001-sdk-java/security/advisories/new>
   (the maintainers are notified automatically; do this first)
2. **Email**: `security@pain001.com` (PGP available on request)

**Acknowledgement**: within 48 hours. **Triage**: within 7 days.

## Security posture

### Scope

The SDK is an HTTP client. It serialises the arguments you pass to JSON,
sends them to the base URL you configure, and parses the JSON response.
It parses no XML, writes no files and logs nothing.

### Threat model

| Surface | How it's handled |
| :--- | :--- |
| **Payment data in logs** | The client never logs request or response bodies; exception messages name only the status, method and path. |
| **API key exposure** | Sent only in the `Authorization` header to the configured base URL; redirects are not followed, so the key is never forwarded to another host. Use https off loopback. |
| **Path injection** | `projectFace` accepts only a plain face name (letters, digits, `_`, `-`); `request` requires a path starting with `/`. |
| **Hostile server responses** | Parsed with Jackson into a tree; a body that is not JSON is kept as a string, never evaluated. Size is bounded only by the server you trust. |
| **Dependency CVEs** | One runtime dependency, Jackson databind, pinned to an exact version and watched by Dependabot. |

### Cryptography status

The SDK implements no cryptography. TLS is the JDK's `HttpClient`.
