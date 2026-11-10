<!-- SPDX-License-Identifier: Apache-2.0 OR MIT -->

# Contributing

Thanks for looking. This SDK is a thin client: it sends requests to the
pain001 REST API (`pain001 serve`) and maps the responses to Java
records. The payment logic lives in the server, in
[`pain001`](https://github.com/sebastienrousseau/pain001); change it
there.

## Before you open a pull request

Everything CI checks, you can run locally with a JDK (17 or newer) and
Maven:

```sh
make check            # whitespace format, file size, mvn -B verify, example compile
```

`mvn -B verify` compiles with `-Xlint:all -Werror`, runs the unit tests
(a stub HTTP server, no network) and fails on any PMD complexity
violation (cyclomatic 10, cognitive 15, 60 statements per method,
`config/pmd/complexity.xml`).

To run the conformance kit and the typed tests, start a server and point
the tests at it:

```sh
pip install 'pain001[api]==0.0.85'
pain001 serve --host 127.0.0.1 --port 8000 &
PAIN001_BASE_URL=http://127.0.0.1:8000 make conformance
PAIN001_BASE_URL=http://127.0.0.1:8000 make example
```

Without `PAIN001_BASE_URL` those tests skip, and say so.

## What the tests are for

- `Pain001ClientTest`: request shape, response mapping and error mapping
  against a stub server.
- `ConformanceKitTest`: the kit runner itself (JSON Pointer, `$file`,
  every kind of failure).
- `KitChecksumTest`: `conformance/` is byte for byte what
  `conformance/SHA256SUMS` pins.
- `ConformanceTest`: every case in `conformance/cases.json` through
  `Pain001Client.request`, against a live server.
- `TypedApiTest`: one test per typed method, against a live server.

## The conformance kit

`conformance/` is vendored from core, byte for byte. **Do not edit it
here.** To move to a new kit, copy core's `conformance/` at the matching
version over this one, then regenerate the pins:

```sh
cd conformance
find . -type f ! -name SHA256SUMS | sed 's|^\./||' | LC_ALL=C sort \
  | xargs shasum -a 256 > SHA256SUMS
```

Update `ci/server-requirements.in` to the same core version and
regenerate `ci/server-requirements.txt` with the command in its header.

## Commits

Conventional commits, with a body that says why. Every commit is signed
off (`git commit -s`, see [DCO.txt](DCO.txt)).

## Versioning

The version lives in `pom.xml` and `CITATION.cff`, with a `## [0.0.N]`
heading in `CHANGELOG.md`. **Versions increment by 0.0.1**, in lockstep
with the pain001 suite. `0.1.0` follows `0.0.999`, not `0.0.9`.

## Licence

Apache-2.0 OR MIT, at your option. By contributing you agree your work
is released under the same dual grant.
