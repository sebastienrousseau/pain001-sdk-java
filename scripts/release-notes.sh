#!/usr/bin/env bash
# SPDX-License-Identifier: Apache-2.0 OR MIT
#
# Writes the GitHub release notes for VERSION to stdout, in the suite's
# shape: Highlights, What's Changed, Checksums, Full Changelog.
#
#   scripts/release-notes.sh 0.0.88 dist/SHA256SUMS > notes.md
#
# Highlights are the only hand-written part: the bullets under
# "### Highlights" in the CHANGELOG section for VERSION (or
# [Unreleased] while it is being prepared), 2 to 4 of them. What's
# Changed lists the pull requests merged into main since the previous
# tag (gh api; needs GH_TOKEN). The notes must not contain an em dash.
set -euo pipefail

cd "$(dirname "$0")/.."
version=${1:?usage: release-notes.sh VERSION SHA256SUMS}
sums=${2:?usage: release-notes.sh VERSION SHA256SUMS}
repo=${GITHUB_REPOSITORY:-sebastienrousseau/pain001-sdk-java}
tag="v$version"

section=$(awk -v v="$version" '
  /^## \[/ { inside = ($0 ~ "^## \\[" v "\\]" || $0 ~ /^## \[Unreleased\]/); next }
  inside { print }' CHANGELOG.md)
highlights=$(printf '%s\n' "$section" |
  awk '/^### Highlights/ { on = 1; next } /^### / { on = 0 } on && NF' | sed 's/^- /* /')
count=$(printf '%s\n' "$highlights" | grep -c '^\* ' || true)
if [[ $count -lt 2 || $count -gt 4 ]]; then
  echo "release-notes: CHANGELOG needs 2 to 4 '### Highlights' bullets for $version (found $count)" >&2
  exit 1
fi

# The previous tag, if any, bounds the pull requests and the compare link.
previous=$(git tag --list 'v0.0.*' --sort=-version:refname | grep -vx "$tag" | head -n 1 || true)
since=""
if [[ -n "$previous" ]]; then
  since=$(git log -1 --format=%cI "$previous")
fi
changes=$(gh api --paginate "repos/$repo/pulls?state=closed&base=main&per_page=100" \
  --jq ".[] | select(.merged_at != null) | select(\"$since\" == \"\" or .merged_at > \"$since\")
        | \"* \(.title) by @\(.user.login) in \(.html_url)\"")
if [[ -n "$previous" ]]; then
  full="https://github.com/$repo/compare/$previous...$tag"
else
  full="https://github.com/$repo/commits/$tag"
fi

notes=$(
  printf '## Highlights ⭐️\n\n%s\n\n' "$highlights"
  printf "## What's Changed\n\n%s\n\n" "$changes"
  printf '## Checksums\n\nSHA-256 of every asset attached to this release:\n\n```text\n%s\n```\n\n' "$(cat "$sums")"
  printf '**Full Changelog**: %s\n' "$full"
)
if printf '%s' "$notes" | grep -q '—'; then
  echo "release-notes: the notes contain an em dash" >&2
  exit 1
fi
printf '%s\n' "$notes"
