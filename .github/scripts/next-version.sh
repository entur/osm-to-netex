#!/bin/bash
# Script to calculate next version
# Usage: ./next-version.sh <current-version> <increment-type>
# increment-type: major, minor, patch, or specific version

set -e

if [ -z "$1" ]; then
  echo "Usage: $0 <current-version> [major|minor|patch|<specific-version>]"
  exit 1
fi

CURRENT_VERSION="$1"
INCREMENT_TYPE="${2:-patch}"

# Remove -SNAPSHOT if present
BASE_VERSION="${CURRENT_VERSION%-SNAPSHOT}"

# Split version into parts
IFS='.' read -ra VERSION_PARTS <<< "$BASE_VERSION"
MAJOR="${VERSION_PARTS[0]:-0}"
MINOR="${VERSION_PARTS[1]:-0}"
PATCH="${VERSION_PARTS[2]:-0}"

case "$INCREMENT_TYPE" in
  major)
    MAJOR=$((MAJOR + 1))
    MINOR=0
    PATCH=0
    NEXT_VERSION="$MAJOR.$MINOR.$PATCH-SNAPSHOT"
    ;;
  minor)
    MINOR=$((MINOR + 1))
    PATCH=0
    NEXT_VERSION="$MAJOR.$MINOR.$PATCH-SNAPSHOT"
    ;;
  patch)
    PATCH=$((PATCH + 1))
    NEXT_VERSION="$MAJOR.$MINOR.$PATCH-SNAPSHOT"
    ;;
  *)
    # Assume it's a specific version
    if [[ "$INCREMENT_TYPE" =~ ^[0-9]+\.[0-9]+\.[0-9]+.*$ ]]; then
      NEXT_VERSION="$INCREMENT_TYPE"
      # Add -SNAPSHOT if not present
      if [[ ! "$NEXT_VERSION" =~ -SNAPSHOT$ ]]; then
        NEXT_VERSION="$NEXT_VERSION-SNAPSHOT"
      fi
    else
      echo "Error: Invalid increment type or version format: $INCREMENT_TYPE"
      exit 1
    fi
    ;;
esac

echo "$NEXT_VERSION"