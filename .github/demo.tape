# SPDX-License-Identifier: Apache-2.0 OR MIT
#
# Rendered by `make demo` (vhs, https://github.com/charmbracelet/vhs) from
# the repository root. The hidden preamble starts `pain001 serve` on
# 127.0.0.1 in a throwaway directory and waits for /api/v1/health; the
# recording then runs examples/GenerateCreditTransfer.java (compiled by
# `make demo` beforehand) against it: health, validate and generate of the
# conformance kit's synthetic credit-transfer CSV, then the head of the
# XSD-checked pain.001.001.09 file. Nothing leaves the loopback interface.
# `make demo` exports DEMO_ROOT, DEMO_CP and PAIN001_SERVER.
Output .github/demo.gif

Set Shell "bash"
Set FontSize 14
Set Width 1200
Set Height 640
Set Padding 20
Set Theme "Catppuccin Mocha"

Hide
Type `cd "$(mktemp -d /tmp/pain001-demo.XXXX)" && export PS1='$ ' PAIN001_BASE_URL=http://127.0.0.1:18767`
Enter
Type `mkdir -p conformance/fixtures && cp "$DEMO_ROOT"/conformance/fixtures/credit-transfer.pain.001.001.09.csv conformance/fixtures/ && cp "$DEMO_ROOT"/examples/GenerateCreditTransfer.java .`
Enter
Type `"$PAIN001_SERVER" serve --host 127.0.0.1 --port 18767 > server.log 2>&1 & SERVER_PID=$!`
Enter
Type `for _ in $(seq 1 60); do curl -fsS "$PAIN001_BASE_URL/api/v1/health" > /dev/null 2>&1 && break; sleep 1; done; clear`
Enter
Wait@90s /^\$$/
Show

Type "grep -n 'client\.' GenerateCreditTransfer.java"
Sleep 500ms
Enter
Wait@30s /^\$$/
Sleep 3s

Type `java -cp "$DEMO_CP" GenerateCreditTransfer credit-transfer.xml`
Sleep 500ms
Enter
Wait@60s /^\$$/
Sleep 4s

Type "head -n 14 credit-transfer.xml | expand -t 2"
Sleep 500ms
Enter
Wait@30s /^\$$/
Sleep 5s

Hide
Type `kill "$SERVER_PID"`
Enter
