#!/usr/bin/env bash
# SPDX-License-Identifier: Apache-2.0 OR MIT
#
# The full regression run, the same steps as CI: every local gate
# (make check), the conformance kit and typed tests, every example and
# the benchmark smoke run, against a pain001 server this script starts
# on a free loopback port and always stops.
#
#   make regression                                  # installs the pinned server
#   PAIN001_SERVER=/path/to/bin/pain001 make regression
#
# Without PAIN001_SERVER it installs ci/server-requirements.txt
# (hash-pinned) into a temporary venv with uv, or python3 -m venv and
# pip when uv is absent. Nothing is left behind: the venv, the server's
# working directory and the server process go when the script exits,
# whether it passes or fails. Safe to run again.
set -euo pipefail

cd "$(dirname "$0")/.."
MAKE=${MAKE:-make}
work=$(mktemp -d "${TMPDIR:-/tmp}/pain001-regression.XXXXXX")
server_pid=""

cleanup() {
  if [[ -n "$server_pid" ]] && kill -0 "$server_pid" 2> /dev/null; then
    kill "$server_pid" 2> /dev/null || true
    wait "$server_pid" 2> /dev/null || true
  fi
  rm -r -f -- "$work"
}
trap cleanup EXIT INT TERM

step() { printf '\n==> %s\n' "$*"; }

install_server() {
  step "installing the pinned server from ci/server-requirements.txt"
  if command -v uv > /dev/null; then
    uv venv -q -p 3.12 "$work/venv"
    uv pip install -q --python "$work/venv/bin/python" --require-hashes -r ci/server-requirements.txt
  else
    python3 -m venv "$work/venv"
    "$work/venv/bin/python" -m pip install -q --require-hashes -r ci/server-requirements.txt
  fi
  PAIN001_SERVER="$work/venv/bin/pain001"
}

free_port() {
  python3 -I -c 'import socket; s = socket.socket(); s.bind(("127.0.0.1", 0)); print(s.getsockname()[1]); s.close()'
}

start_server() {
  local port url
  port=${PAIN001_PORT:-$(free_port)}
  url="http://127.0.0.1:$port"
  step "starting $("$PAIN001_SERVER" --version) on $url"
  (cd "$work" && exec "$PAIN001_SERVER" serve --host 127.0.0.1 --port "$port") > "$work/server.log" 2>&1 &
  server_pid=$!
  for _ in $(seq 1 60); do
    if curl -fsS "$url/api/v1/health" > /dev/null 2>&1; then
      export PAIN001_BASE_URL="$url"
      return 0
    fi
    if ! kill -0 "$server_pid" 2> /dev/null; then break; fi
    sleep 1
  done
  cat "$work/server.log" >&2
  echo "regression: the server did not become healthy" >&2
  return 1
}

if [[ -z "${PAIN001_SERVER:-}" ]]; then
  install_server
fi
[[ -x "$PAIN001_SERVER" ]] || { echo "regression: $PAIN001_SERVER is not executable" >&2; exit 1; }
start_server

step "every local gate (make check, with the live tests on)"
"$MAKE" check

step "conformance kit and typed tests"
"$MAKE" conformance
report=target/surefire-reports/TEST-com.pain001.sdk.ConformanceTest.xml
cases=$(python3 -I -c 'import json; print(len(json.load(open("conformance/cases.json"))["cases"]))')
grep -Eq "tests=\"$cases\"" "$report"
grep -Eq 'skipped="0"' "$report"
grep -Eq 'skipped="0"' target/surefire-reports/TEST-com.pain001.sdk.TypedApiTest.xml
echo "$cases conformance cases ran and passed, none skipped"

step "every example"
"$MAKE" examples
grep -q '<NbOfTxs>5</NbOfTxs>' target/credit-transfer.xml

step "benchmark smoke run (client-side and loopback)"
"$MAKE" bench-smoke

step "regression passed against $PAIN001_BASE_URL"
