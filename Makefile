# SPDX-License-Identifier: Apache-2.0 OR MIT
#
# `make check` runs every local gate CI runs: whitespace format, file
# size, the Maven build (javac -Xlint:all -Werror, unit tests, PMD
# complexity ceilings) and the example's compile. The conformance kit and
# the typed tests run inside `mvn verify` when PAIN001_BASE_URL is set,
# and skip otherwise.

MVN ?= mvn
JAVA_FILES := $(shell find src examples -name '*.java' | LC_ALL=C sort)
EXAMPLE_CP = target/classes:$$(cat target/classpath.txt)

.PHONY: help check format-check size verify build test lint conformance classpath example-compile example clean

help: ## List the targets
	@grep -E '^[a-z-]+:.*## ' $(MAKEFILE_LIST) | sed 's/:.*## /: /'

check: format-check size verify example-compile ## Every local gate

format-check: ## No tabs, no trailing spaces, LF endings, final newline
	@bad=0; \
	for f in $(JAVA_FILES) pom.xml config/pmd/complexity.xml; do \
	  if grep -n "$$(printf '\t')" "$$f"; then echo "tab in $$f"; bad=1; fi; \
	  if grep -nE ' +$$' "$$f"; then echo "trailing space in $$f"; bad=1; fi; \
	  if grep -q "$$(printf '\r')" "$$f"; then echo "CRLF in $$f"; bad=1; fi; \
	  if [ -n "$$(tail -c1 "$$f")" ]; then echo "no final newline in $$f"; bad=1; fi; \
	done; exit $$bad

size: ## Every Java file is at most 500 lines
	@bad=0; \
	for f in $(JAVA_FILES); do \
	  n=$$(wc -l < "$$f"); \
	  if [ "$$n" -gt 500 ]; then echo "$$f: $$n lines (limit 500)"; bad=1; fi; \
	done; exit $$bad

verify: ## Build, unit tests, PMD complexity (conformance when PAIN001_BASE_URL is set)
	$(MVN) -B verify

build: ## Compile only
	$(MVN) -B -DskipTests compile

test: ## Unit tests (live tests skip without PAIN001_BASE_URL)
	$(MVN) -B test

lint: ## javac warnings as errors and the PMD complexity ceilings
	$(MVN) -B -DskipTests compile pmd:check

conformance: ## The kit and the typed tests against PAIN001_BASE_URL
	@test -n "$$PAIN001_BASE_URL" || { echo "set PAIN001_BASE_URL to a running pain001 serve"; exit 1; }
	$(MVN) -B test -Dtest='ConformanceTest,TypedApiTest' -Dsurefire.failIfNoSpecifiedTests=false

classpath:
	$(MVN) -B -q dependency:build-classpath -Dmdep.outputFile=target/classpath.txt -Dmdep.includeScope=runtime

example-compile: classpath ## Compile examples/ against the built SDK
	javac --release 17 -Xlint:all -Werror -d target/example-classes -cp "$(EXAMPLE_CP)" examples/*.java

example: classpath ## Generate target/credit-transfer.xml through PAIN001_BASE_URL
	java -cp "$(EXAMPLE_CP)" examples/GenerateCreditTransfer.java

clean: ## Delete the Maven target directory
	$(MVN) -B -q clean
