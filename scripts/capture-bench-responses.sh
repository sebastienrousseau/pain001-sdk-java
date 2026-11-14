#!/usr/bin/env sh
# SPDX-License-Identifier: Apache-2.0 OR MIT
#
# Records the server's answer to every typed call, made with the
# conformance kit's synthetic fixtures, as the response bodies the
# client-side decoding benchmarks parse (src/bench/resources/). Rerun it
# when the kit or the pinned server changes, and commit the result.
#
#   PAIN001_BASE_URL=http://127.0.0.1:8000 scripts/capture-bench-responses.sh
set -eu

cd "$(dirname "$0")/.."
: "${PAIN001_BASE_URL:?set PAIN001_BASE_URL to a running pain001 serve}"
out=src/bench/resources/com/pain001/sdk/bench
mkdir -p "$out"

post() { # name path python-expression-for-the-body
  python3 -I -c "import json,sys; f=lambda n: open('conformance/fixtures/'+n, encoding='utf-8').read(); print(json.dumps($3))" \
    | curl -fsS -H 'Content-Type: application/json' --data-binary @- "$PAIN001_BASE_URL$2" \
    | python3 -I -m json.tool --indent 2 > "$out/$1.json"
}
get() { # name path
  curl -fsS "$PAIN001_BASE_URL$2" | python3 -I -m json.tool --indent 2 > "$out/$1.json"
}

csv='f("credit-transfer.pain.001.001.09.csv")'
xml='f("credit-transfer.pain.001.001.09.xml")'
get health /api/v1/health
get faces /api/v1/faces
post validate /api/v1/ui/validate "{'filename': 'payments.csv', 'content': $csv, 'message_type': 'pain.001.001.09'}"
post generate /api/v1/ui/generate "{'filename': 'payments.csv', 'content': $csv, 'message_type': 'pain.001.001.09'}"
post check /api/v1/check "{'xml': $xml}"
post project-face /api/v1/faces/berlin_group "{'xml': $xml}"
ls -l "$out"
