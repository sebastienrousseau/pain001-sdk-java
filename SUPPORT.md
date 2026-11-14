<!-- SPDX-License-Identifier: Apache-2.0 OR MIT -->

# Getting support

## Read first

- [README.md](README.md): install, quick start, configuration and the
  method list.
- [`examples/`](examples/): one runnable program per method, each run
  against a real server in CI.
- [The user manual](https://sebastienrousseau.github.io/pain001-sdk-java/):
  the docs, the architecture and the Javadoc in one place.
- Server behaviour (what a field means, why a row is refused) is
  documented in the [pain001 core](https://github.com/sebastienrousseau/pain001).

## Questions and bugs

Open an [issue](https://github.com/sebastienrousseau/pain001-sdk-java/issues/new/choose)
with the SDK version, your Java version, the server's version (from
`client.health().version()`), a minimal reproducer and the full
exception. Never paste real payment data or API keys; the conformance
kit's synthetic fixtures reproduce most problems.

If the server, not the client, gives the wrong answer, report it to
[pain001](https://github.com/sebastienrousseau/pain001/issues).

## Security

Do not open a public issue. Follow [SECURITY.md](SECURITY.md).

## Supported versions

| Version | Supported | Notes |
| :--- | :--- | :--- |
| 0.0.88 | Yes | Current release; tested against pain001 0.0.87 |
| Older | No | No earlier version was published |

Only the latest `0.0.x` gets fixes while the project is pre-1.0. Support
is best effort by a single maintainer ([GOVERNANCE.md](GOVERNANCE.md)).
