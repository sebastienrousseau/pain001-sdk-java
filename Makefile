# SPDX-License-Identifier: Apache-2.0 OR MIT
#
# `make check` runs every local gate CI's build job runs: the version
# check, whitespace format, file size, the Maven build (javac -Xlint:all
# -Werror, unit tests, JaCoCo at 100 % line and branch, PMD complexity
# ceilings) and the examples' compile. The conformance kit and the typed
# tests run inside `mvn verify` when PAIN001_BASE_URL is set, and skip
# otherwise. The Jazzer fuzz tests run their committed seeds as
# regression tests there too; `make fuzz` fuzzes them. `make regression`
# runs all of it against a fresh server, as CI's conformance job does.
# DEVELOPMENT.md maps every CI job to its target.
#
# ./mvnw pins Maven 3.10.0 (SHA-256 checked) and .mvn/maven.config makes
# Maven refuse any artifact whose SHA-256 is not recorded in
# .mvn/checksums/. After a dependency or plugin change, `make lock`
# re-records them from a clean local repository; review the diff.

MVN ?= ./mvnw
OSV_SCANNER ?= osv-scanner
VHS ?= vhs
PAIN001_SERVER ?= $(shell command -v pain001)
LOCK_REPO ?= /tmp/pain001-sdk-java-lock-repo
TC := -Daether.artifactResolver.postProcessor.trustedChecksums
JAVA_FILES := $(shell find src examples -name '*.java' | LC_ALL=C sort)
EXAMPLE_CP = target/classes:$$(cat target/classpath.txt)
EXAMPLES := $(sort $(basename $(notdir $(wildcard examples/*.java))))
LOCK_PROFILES := bench,release
CENTRAL := https://repo1.maven.org/maven2/com/pain001/pain001-sdk
DOCS_PY ?= python3
FUZZ_TARGETS ?= ResponseDecodingFuzzTest ClientResponseFuzzTest
FUZZ_TIME ?= 60s

.PHONY: help check format-check size verify build test lint conformance classpath example-compile example examples bench-build bench bench-smoke demo fuzz lock sbom audit clean \
  versions regression api-check javadoc manual docs-lint release-dry-run

help: ## List the targets
	@grep -E '^[a-z-]+:.*## ' $(MAKEFILE_LIST) | sed 's/:.*## /: /'

check: versions format-check size verify example-compile ## Every local gate

versions: ## Every file states the version in pom.xml
	@scripts/verify-release-versions

regression: ## Gate, conformance kit, examples and benchmark smoke against a fresh server
	scripts/regression.sh

api-check: ## japicmp against the latest release on Maven Central (no-op before the first)
	@latest=$$(curl -fsS "$(CENTRAL)/maven-metadata.xml" 2>/dev/null | sed -n 's:.*<release>\(.*\)</release>.*:\1:p'); \
	if [ -z "$$latest" ]; then \
	  echo "api-check: no release of com.pain001:pain001-sdk on Maven Central yet; nothing to compare"; exit 0; fi; \
	mkdir -p target; jar="target/japicmp-baseline.jar"; \
	curl -fsSL -o "$$jar" "$(CENTRAL)/$$latest/pain001-sdk-$$latest.jar"; \
	want=$$(curl -fsSL "$(CENTRAL)/$$latest/pain001-sdk-$$latest.jar.sha256" | cut -c1-64); \
	got=$$(shasum -a 256 "$$jar" 2>/dev/null || sha256sum "$$jar"); \
	[ "$${got%% *}" = "$$want" ] || { echo "api-check: baseline $$latest does not match its .sha256"; exit 1; }; \
	echo "api-check: comparing with $$latest"; \
	$(MVN) -B -q -DskipTests package japicmp:cmp -Djapicmp.baseline.jar="$(CURDIR)/$$jar"

javadoc: ## The Javadoc (doclint all, warnings fail) into target/reports/apidocs
	$(MVN) -B -q -DskipTests compile javadoc:javadoc

manual: javadoc ## The user manual (MkDocs strict) with the Javadoc under site/api/
	$(DOCS_PY) -m mkdocs build --strict
	mkdir -p site/api && cp -R target/reports/apidocs/. site/api/

docs-lint: ## markdownlint, codespell, REUSE and lychee (offline) over the docs
	npx --yes markdownlint-cli2@0.23.3 "**/*.md"
	codespell
	reuse lint
	lychee --offline --no-progress --include-fragments --exclude-path target --exclude-path site --exclude-path conformance '**/*.md'

release-dry-run: ## The release build (jars, sources, Javadoc, SBOM, bundle), signed with GPG_KEYNAME if set, not published
	$(MVN) -B -Prelease deploy cyclonedx:makeBom -DoutputFormat=json \
	  -Dcentral.skipUpload=true -Dcentral.ignorePublished=false -s ci/dry-run-settings.xml $(if $(GPG_KEYNAME),-Dgpg.keyname=$(GPG_KEYNAME),-Dgpg.skip=true)

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

examples: example-compile ## Run every example against PAIN001_BASE_URL; fails on the first failure
	@test -n "$$PAIN001_BASE_URL" || { echo "set PAIN001_BASE_URL to a running pain001 serve"; exit 1; }
	@for e in $(EXAMPLES); do \
	  echo "== $$e"; \
	  java -cp "target/example-classes:$(EXAMPLE_CP)" "$$e" || { echo "example $$e failed"; exit 1; }; \
	done

BENCH_CP = target/test-classes:target/classes:$$(cat target/bench-classpath.txt)
BENCH_OPTS ?= -f 1 -wi 3 -w 1s -i 5 -r 1s
BENCH_SMOKE_OPTS := -f 1 -wi 0 -i 1 -r 100ms

bench-build: ## Compile the JMH benchmarks (src/bench, profile bench)
	$(MVN) -B -q -Pbench -DskipTests test-compile dependency:build-classpath \
	  -Dmdep.outputFile=target/bench-classpath.txt -Dmdep.includeScope=test

# The loopback benchmarks need PAIN001_BASE_URL; without it they are
# skipped with a message and only the client-side ones run.
bench-run = \
	if [ -n "$$PAIN001_BASE_URL" ]; then sel='Benchmark'; \
	else echo "PAIN001_BASE_URL unset: skipping LoopbackBenchmark (client-side only)"; sel='ClientSideBenchmark'; fi; \
	java -cp "$(BENCH_CP)" org.openjdk.jmh.Main "$$sel" $(1)

bench: bench-build ## Every benchmark, full run (BENCH_OPTS); results in target/jmh.txt
	@$(call bench-run,$(BENCH_OPTS) -rf text -rff target/jmh.txt)

bench-smoke: bench-build ## Every benchmark once, briefly: kept compiling and running, not asserted
	@$(call bench-run,$(BENCH_SMOKE_OPTS))

fuzz: ## Fuzz each Jazzer target for FUZZ_TIME (60s); findings land in its seed directory
	@for t in $(FUZZ_TARGETS); do \
	  JAZZER_FUZZ=1 JAZZER_MAX_DURATION=$(FUZZ_TIME) $(MVN) -B test -Dtest="$$t" || exit 1; \
	done

demo: build example-compile ## Render .github/demo.gif with vhs against PAIN001_SERVER (see CONTRIBUTING)
	@command -v $(VHS) > /dev/null || { echo "demo: vhs not found (https://github.com/charmbracelet/vhs)"; exit 1; }
	@test -n "$(PAIN001_SERVER)" && test -x "$(PAIN001_SERVER)" || { \
	  echo "demo: no pain001 server binary; set PAIN001_SERVER or put pain001 on PATH"; \
	  echo "      install it from ci/server-requirements.txt (see CONTRIBUTING.md)"; exit 1; }
	DEMO_ROOT="$(CURDIR)" PAIN001_SERVER="$(PAIN001_SERVER)" \
	  DEMO_CP="$(CURDIR)/target/example-classes:$(CURDIR)/target/classes:$$(cat target/classpath.txt)" \
	  $(VHS) .github/demo.tape

lock: ## Re-record .mvn/checksums from a clean local repository
	rm -rf .mvn/checksums $(LOCK_REPO)
	$(MVN) -B -q -Dmaven.repo.local=$(LOCK_REPO) $(TC).record=true -P$(LOCK_PROFILES) \
	  clean deploy pmd:check dependency:build-classpath cyclonedx:makeBom javadoc:javadoc japicmp:cmp \
	  -Dmdep.outputFile=target/classpath.txt -DoutputFormat=json -DincludeTestScope=true \
	  -Dgpg.skip=true -Dcentral.skipUpload=true -Dcentral.ignorePublished=false -Djapicmp.skip=true -s ci/dry-run-settings.xml
	rm -rf $(LOCK_REPO)

sbom: ## CycloneDX SBOM of the resolved dependencies (target/bom.json)
	$(MVN) -B -q cyclonedx:makeBom -DoutputFormat=json -DincludeTestScope=true

audit: sbom ## OSV-Scanner over the SBOM; fails on any known advisory
	@$(OSV_SCANNER) scan source -L target/bom.json > target/osv.txt 2>&1; rc=$$?; \
	cat target/osv.txt; \
	grep -Eq 'found [1-9][0-9]* packages' target/osv.txt || { echo "audit: no packages scanned"; exit 1; }; \
	exit $$rc

clean: ## Delete the Maven target directory
	$(MVN) -B -q clean
