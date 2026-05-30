#!/usr/bin/env bash
# bump-version.sh
# Computes semantic version bump rules based on commit type:
# - fix:, chore:, patch:    -> PATCH (right)
# - feat:                   -> MINOR (middle)
# - release:                -> MAJOR (left)
#
# Source of truth priority for current version:
# 1) latest git tag matching vX.Y.Z
# 2) gradle.properties version=X.Y.Z

set -euo pipefail

PROPERTIES_FILE="gradle.properties"

LATEST_TAG=$(git tag --list 'v[0-9]*.[0-9]*.[0-9]*' --sort=-version:refname | head -n 1 || true)

if [ -n "$LATEST_TAG" ]; then
  CURRENT_VERSION="${LATEST_TAG#v}"
else
  if [ ! -f "$PROPERTIES_FILE" ]; then
    echo "Missing $PROPERTIES_FILE and no version tag found (expected vX.Y.Z)"
    exit 1
  fi

  CURRENT_VERSION=$(grep -E "^version=" "$PROPERTIES_FILE" | cut -d'=' -f2 | tr -d '[:space:]' || true)
  if [ -z "$CURRENT_VERSION" ]; then
    echo "Missing version entry in $PROPERTIES_FILE and no version tag found"
    exit 1
  fi
fi

if ! echo "$CURRENT_VERSION" | grep -qE '^[0-9]+\.[0-9]+\.[0-9]+$'; then
  echo "Invalid semantic version detected: $CURRENT_VERSION"
  exit 1
fi

IFS='.' read -r MAJOR MINOR PATCH <<< "$CURRENT_VERSION"
echo "Current version: $CURRENT_VERSION"

RANGE="${1:-${GIT_COMMIT_RANGE:-}}"

if [ -z "$RANGE" ] && [ -n "${GITHUB_EVENT_BEFORE:-}" ] && [ -n "${GITHUB_SHA:-}" ]; then
  if [ "$GITHUB_EVENT_BEFORE" != "0000000000000000000000000000000000000000" ]; then
    RANGE="$GITHUB_EVENT_BEFORE..$GITHUB_SHA"
  fi
fi

if [ -z "$RANGE" ]; then
  RANGE="HEAD~1..HEAD"
fi

echo "Analysed range: $RANGE"

COMMITS=$(git log "$RANGE" --pretty=format:"%s" 2>/dev/null || true)

if [ -z "$COMMITS" ]; then
  echo "No commits found in range."
  if [ -n "${GITHUB_OUTPUT:-}" ]; then
    {
      echo "should_bump=false"
      echo "bump_type=none"
      echo "new_version=$CURRENT_VERSION"
    } >> "$GITHUB_OUTPUT"
  fi
  exit 0
fi

BUMP="none"

while IFS= read -r commit; do
  if echo "$commit" | grep -qiE '^chore\(version\): bump to '; then
    continue
  fi

  if echo "$commit" | grep -qiE '^release(\(.+\))?:'; then
    BUMP="major"
    break
  fi

  # Merge commit from a release/* branch: "Merge pull request #N from .../release/..."
  if echo "$commit" | grep -qiE '^Merge pull request #[0-9]+ from .+/release/'; then
    BUMP="major"
    break
  fi
done <<< "$COMMITS"

if [ "$BUMP" = "none" ]; then
  while IFS= read -r commit; do
    if echo "$commit" | grep -qiE '^chore\(version\): bump to '; then
      continue
    fi

    if echo "$commit" | grep -qiE '^feat(\(.+\))?:'; then
      BUMP="minor"
      break
    fi

    # Merge commit from a feat/* branch: "Merge pull request #N from .../feat/..."
    if echo "$commit" | grep -qiE '^Merge pull request #[0-9]+ from .+/feat/'; then
      BUMP="minor"
      break
    fi
  done <<< "$COMMITS"
fi

if [ "$BUMP" = "none" ]; then
  while IFS= read -r commit; do
    if echo "$commit" | grep -qiE '^chore\(version\): bump to '; then
      continue
    fi

    if echo "$commit" | grep -qiE '^(fix|chore|patch)(\(.+\))?:'; then
      BUMP="patch"
      break
    fi

    # Merge commit from a fix/chore/patch/refactor/* branch
    if echo "$commit" | grep -qiE '^Merge pull request #[0-9]+ from .+/(fix|chore|patch|refactor)/'; then
      BUMP="patch"
      break
    fi
  done <<< "$COMMITS"
fi

if [ "$BUMP" = "none" ]; then
  echo "No matching commit type found (release/feat/chore/patch/fix)."
  if [ -n "${GITHUB_OUTPUT:-}" ]; then
    {
      echo "should_bump=false"
      echo "bump_type=none"
      echo "new_version=$CURRENT_VERSION"
    } >> "$GITHUB_OUTPUT"
  fi
  exit 0
fi

case "$BUMP" in
  major)
    MAJOR=$((MAJOR + 1))
    MINOR=0
    PATCH=0
    ;;
  minor)
    MINOR=$((MINOR + 1))
    PATCH=0
    ;;
  patch)
    PATCH=$((PATCH + 1))
    ;;
esac

NEW_VERSION="$MAJOR.$MINOR.$PATCH"
echo "Bump type: $BUMP -> New version: $NEW_VERSION"

if [ -n "${GITHUB_OUTPUT:-}" ]; then
  {
    echo "should_bump=true"
    echo "bump_type=$BUMP"
    echo "new_version=$NEW_VERSION"
  } >> "$GITHUB_OUTPUT"
fi
