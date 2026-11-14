<!-- SPDX-License-Identifier: Apache-2.0 OR MIT -->

# Contributing

Thanks for looking. This SDK is a thin client: it sends requests to the
pain001 REST API (`pain001 serve`) and maps the responses to Java
records. The payment logic lives in the server, in
[`pain001`](https://github.com/sebastienrousseau/pain001); change it
there.

## Before you open a pull request

Everything CI checks, you can run locally with a JDK (17 or newer);
`./mvnw` fetches the pinned Maven and checks its SHA-256:

```sh
make check            # whitespace format, file size, mvn -B verify, example compile
```

`mvn -B verify` compiles with `-Xlint:all -Werror`, runs the unit tests
(a stub HTTP server, no network) and fails on any PMD complexity
violation (cyclomatic 10, cognitive 15, 60 statements per method,
`config/pmd/complexity.xml`).

Maven refuses any dependency or plugin whose SHA-256 is not recorded in
`.mvn/checksums/` (see `.mvn/maven.config`). After changing a dependency
or plugin version, including a Dependabot bump, run `make lock`, review
the diff of `.mvn/checksums/`, and commit it with the change. `make
audit` builds a CycloneDX SBOM and checks it with
[OSV-Scanner](https://google.github.io/osv-scanner/), as CI does.

To run the conformance kit and the typed tests, start a server and point
the tests at it:

```sh
pip install 'pain001[api]==0.0.87'
pain001 serve --host 127.0.0.1 --port 8000 &
PAIN001_BASE_URL=http://127.0.0.1:8000 make conformance
PAIN001_BASE_URL=http://127.0.0.1:8000 make example
```

Without `PAIN001_BASE_URL` those tests skip, and say so.

## The README demo

`.github/demo.gif` is rendered from `.github/demo.tape` by `make demo`,
never recorded by hand. It needs [vhs](https://github.com/charmbracelet/vhs)
(with ttyd and ffmpeg) and a `pain001` server binary, taken from
`PAIN001_SERVER` or, if that is unset, from `PATH`; `make demo` stops
with an error when neither is there. Install the server the way CI
does, hash-pinned:

```sh
uv venv /tmp/pain001-srv
uv pip install --python /tmp/pain001-srv/bin/python --require-hashes -r ci/server-requirements.txt
PAIN001_SERVER=/tmp/pain001-srv/bin/pain001 make demo
```

The tape starts `pain001 serve` on `127.0.0.1:18767` in a temporary
directory, runs `examples/GenerateCreditTransfer.java` against it with
the kit's synthetic CSV, and stops the server. Nothing leaves the
loopback interface. Re-render the GIF in the same change whenever the
example's output changes.

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
- `ResponseDecodingFuzzTest` and `ClientResponseFuzzTest` (in `src/fuzz/java/`):
  [Jazzer](https://github.com/CodeIntelligenceTesting/jazzer) fuzz
  tests for what a server sends back. The first feeds arbitrary bytes
  to the lenient parse and every record decoder; the second answers
  every typed call from a loopback server with an arbitrary status and
  body, and checks that only `Pain001ApiException` (or
  `GenerationRefusedException`, or `NoDocumentException` for a 2xx
  `generate` without XML) escapes, with the status, the body's
  `detail`, and a message that never contains the body.

## Fuzzing

The fuzz tests live in `src/fuzz/java/` and their committed seeds in
`src/fuzz/resources/com/pain001/sdk/<Test>Inputs/`; the build adds both
as test roots, so `mvn verify` runs every seed as an ordinary
regression test. They are outside `src/test/` because OpenSSF
Scorecard's Fuzzing check ignores everything under `src/test/`. To fuzz:

```sh
make fuzz                                  # each target for 60 s
make fuzz FUZZ_TARGETS=ClientResponseFuzzTest FUZZ_TIME=10m
```

The generated corpus goes to `.cifuzz-corpus/` (ignored). Jazzer
always writes a finding as `crash-<sha1>` under
`src/test/resources/com/pain001/sdk/<Test>Inputs/<method>/` (the path
is fixed in Jazzer). Move it to the same path under
`src/fuzz/resources/` and commit it with the fix, so it stays a
regression test. CI fuzzes each target for 60 seconds on every pull
request and uploads any finding.

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

The version lives in `pom.xml`, `Pain001Client.VERSION`, `CITATION.cff`,
the `SECURITY.md` and `SUPPORT.md` tables and the install snippets, with
a `## [0.0.N]` heading in `CHANGELOG.md`; `scripts/verify-release-versions`
fails when any of them disagrees. **Versions increment by 0.0.1**, in lockstep
with the pain001 suite. `0.1.0` follows `0.0.999`, not `0.0.9`.

## Licence

Apache-2.0 OR MIT, at your option. By contributing you agree your work
is released under the same dual grant.
