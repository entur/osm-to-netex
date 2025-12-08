#!/bin/bash
# Script to update version in pom.xml using Maven
# Usage: ./update-version.sh <new-version>

set -e

if [ -z "$1" ]; then
  echo "Usage: $0 <new-version>"
  exit 1
fi

NEW_VERSION="$1"

echo "Updating version to: $NEW_VERSION"

# Update version using Maven versions plugin
# -DprocessAllModules=true ensures all modules in multi-module projects are updated
mvn -B versions:set -DnewVersion="$NEW_VERSION" -DgenerateBackupPoms=false -DprocessAllModules=true

# Verify the change
CURRENT_VERSION=$(mvn help:evaluate -Dexpression=project.version -q -DforceStdout)
echo "Version updated to: $CURRENT_VERSION"

if [ "$CURRENT_VERSION" != "$NEW_VERSION" ]; then
  echo "Error: Version update failed"
  exit 1
fi

echo "Version update successful"