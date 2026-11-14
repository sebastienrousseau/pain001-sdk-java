<!-- SPDX-License-Identifier: Apache-2.0 OR MIT -->

# Benchmarks

Two kinds, both with [JMH](https://github.com/openjdk/jmh) 1.37, for
every public method.

- **Client-side cost** (`ClientSideBenchmark`, no network): building
  each typed method's request (the JSON body, its serialisation, the
  `HttpRequest` with its headers) and decoding each typed method's
  response (parse plus record mapping). Inputs are the conformance kit's
  synthetic fixtures; the responses are what pain001 0.0.87 answered for
  them, recorded by `scripts/capture-bench-responses.sh` into
  `src/bench/resources/`.
- **Loopback round trip** (`LoopbackBenchmark`): one full call per
  method, plus the raw `request`, against `pain001 serve` on
  127.0.0.1. This is mostly the server's work; the client-side numbers
  above are the SDK's share of it.

`make bench` runs both (the loopback ones only with `PAIN001_BASE_URL`
set; they are skipped with a message otherwise) and writes
`target/jmh.txt`. CI smoke-runs every benchmark once per pull request
(`make bench-smoke`) to keep them compiling and running; nothing is
asserted on the numbers.

## Environment

| | |
| :--- | :--- |
| Machine | Apple A18 Pro, 6 cores, 8 GB, macOS 27.2 |
| JVM | Temurin 21.0.13+9 (OpenJDK 64-Bit Server VM), default flags |
| JMH | 1.37, average time, one thread |
| Server | pain001 0.0.87 (`pain001 serve`, one process) on 127.0.0.1 |
| Date | 2026-11-14 |

**Read these numbers as rough.** The machine was shared with other
heavy builds while they were taken: the load average was between 80
and 245 on 6 cores during the client-side run (it was not recorded for
the loopback run). The confidence intervals are wide, and some pairs
that do the same work (`buildValidate` and `buildGenerate`) differ by
more than the work explains. Re-run `make bench` on an idle machine
before quoting a number.

## Client-side cost

3 forks, 5 x 1 s warm-up and 10 x 1 s measurement iterations each
(30 samples per benchmark); score is the mean, error the 99.9 %
confidence interval.

| Benchmark | Mean (µs/op) | Error (µs/op) |
| :--- | ---: | ---: |
| `buildHealth` | 6.194 | ± 2.432 |
| `buildValidate` | 16.969 | ± 9.631 |
| `buildGenerate` | 46.192 | ± 19.500 |
| `buildCheck` | 171.845 | ± 75.407 |
| `buildFaces` | 7.498 | ± 3.288 |
| `buildProjectFace` | 145.208 | ± 48.511 |
| `decodeHealth` | 1.713 | ± 1.100 |
| `decodeValidate` | 2.386 | ± 1.267 |
| `decodeGenerate` | 27.020 | ± 12.548 |
| `decodeCheck` | 2.679 | ± 1.188 |
| `decodeFaces` | 2.654 | ± 0.614 |
| `decodeProjectFace` | 58.208 | ± 21.214 |
| `decodeRawRequest` | 75.940 | ± 29.825 |

`build*` for `validate` and `generate` serialise the 5-row kit CSV;
`check` and `projectFace` serialise the 4.4 KB kit XML. `decodeGenerate`
and `decodeRawRequest` parse the 4.5 KB generate answer (the raw one
without record mapping); `decodeProjectFace` the 5.6 KB Berlin Group
projection.

## Loopback round trip

1 fork, 3 x 1 s warm-up and 5 x 1 s measurement iterations
(`make bench` defaults); 5 samples per benchmark.

| Benchmark | Mean (ms/op) | Error (ms/op) |
| :--- | ---: | ---: |
| `health` | 4.912 | ± 6.638 |
| `validate` | 26.833 | ± 51.713 |
| `generate` | 374.956 | ± 542.977 |
| `check` | 37.159 | ± 74.779 |
| `faces` | 12.839 | ± 29.933 |
| `projectFace` | 51.476 | ± 94.871 |
| `rawRequest` (`GET /api/v1/health`) | 10.741 | ± 28.349 |

The client-side cost of every method is under 0.25 ms in these runs,
against 5 to 375 ms for the round trip: the time a caller sees is the
server's.

## Reproducing

```sh
make bench                                               # client-side only
PAIN001_BASE_URL=http://127.0.0.1:8000 make bench        # both kinds
make bench BENCH_OPTS="-f 3 -wi 5 -w 1s -i 10 -r 1s"     # the client-side settings above
```
