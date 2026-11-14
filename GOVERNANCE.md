<!-- SPDX-License-Identifier: Apache-2.0 OR MIT -->

# Governance

How `pain001-sdk-java` is run, who decides, and what happens if its
maintainer is unavailable.

## Scope

A typed Java client for the pain001 REST API. Payment logic belongs to
the [`pain001`](https://github.com/sebastienrousseau/pain001) core and
is governed there; this repository decides only how Java callers reach
it ([ADR 0001](docs/adr/0001-thin-client-over-the-server.md)).

## Roles and responsibilities

| Role | Who | Responsible for |
| :--- | :--- | :--- |
| **Maintainer** | Sebastien Rousseau ([@sebastienrousseau](https://github.com/sebastienrousseau)), the only maintainer | Merging pull requests, cutting and signing releases, Maven Central publication, security response (`SECURITY.md`), keeping the SDK in lockstep with the suite |
| **Contributor** | Anyone with a merged pull request | Proposing changes with tests, reviewing others' changes |
| **User** | Everyone | Reporting bugs, asking questions, requesting features (`SUPPORT.md`) |

## Decisions

- **Day-to-day changes** (fixes, tests, docs, additive methods for
  endpoints the kit already covers) land as commits on the release
  branch `feat/v<next-version>` and reach `main` through its one pull
  request, once CI is green.
- **Significant changes** (a breaking API change, a new runtime
  dependency, a Java floor raise, a change to the error model) start as
  an issue and are recorded as an ADR in `docs/adr/`.
- **The version** moves by `0.0.1`, in lockstep with the suite; the
  maintainer decides when a release opens.

CI is the definition of done (`DEVELOPMENT.md`): coverage, complexity,
conformance and supply-chain checks are enforced by workflows, not by
review alone.

## Becoming a maintainer

Contribute reviewed changes, help triage issues, then ask. A second
maintainer with release rights is the single largest risk reduction this
project can make.

## Continuity plan

What happens to each asset if the maintainer becomes unavailable. Items
marked **Not yet arranged** have no continuity arrangement today.

| Asset | Today | Continuity |
| :--- | :--- | :--- |
| Source code | Apache-2.0 OR MIT, public history | Anyone can fork and continue it. |
| GitHub repository | Owned by the maintainer's account | **Not yet arranged:** no second owner or organisation. |
| Maven Central namespace `com.pain001` | Verified on the Central Portal by the maintainer | **Not yet arranged:** no second account has publish rights on the namespace. |
| Central Portal token and GPG signing key | Repository secrets set by the maintainer (`DEVELOPMENT.md` lists them) | **Not yet arranged:** no documented key escrow; a successor would publish under a new GPG key announced in `KEYS.asc`. |
| Tag-signing SSH key | Listed in `KEYS.asc` | A new maintainer signs with their own key, added to `KEYS.asc` in a reviewed change. |
| Release process | `DEVELOPMENT.md` and `.github/workflows/release.yml` (with a dry-run mode) | Anyone with repository and namespace rights can release from the documented process. |
| Domain `pain001.com` | Registered by the maintainer | Governed by the core project's plan ([`pain001` GOVERNANCE.md](https://github.com/sebastienrousseau/pain001/blob/main/GOVERNANCE.md)). |

## Conduct and security

Participation follows [`CODE_OF_CONDUCT.md`](CODE_OF_CONDUCT.md).
Vulnerabilities go through [`SECURITY.md`](SECURITY.md), never a public
issue.
