#!/usr/bin/env bash
# Builds android-tree-sdk's :treesdk module and publishes it to your local Maven
# repository (~/.m2/repository), which is where this app resolves it from — no
# JitPack token needed.
#
# Usage:  ./tools/publish_local_sdk.sh [path-to-android-tree-sdk]
set -euo pipefail

SDK_PATH="${1:-${ANDROID_TREE_SDK_PATH:-$(cd "$(dirname "$0")/../../.." && pwd)/android-tree-sdk}}"

if [ ! -f "$SDK_PATH/settings.gradle.kts" ]; then
  echo "Can't find android-tree-sdk at: $SDK_PATH" >&2
  echo "Pass the path explicitly:  ./tools/publish_local_sdk.sh /path/to/android-tree-sdk" >&2
  exit 1
fi

echo "Publishing :treesdk from $SDK_PATH to mavenLocal…"
cd "$SDK_PATH"
./gradlew :treesdk:publishToMavenLocal

echo
echo "Done. Artifact:"
ls -la "$HOME/.m2/repository/com/github/DRUApp/android-tree-sdk/treesdk/main-SNAPSHOT/" 2>/dev/null \
  || echo "(nothing at ~/.m2/repository/com/github/DRUApp/android-tree-sdk/treesdk/main-SNAPSHOT — check the Gradle output above)"
