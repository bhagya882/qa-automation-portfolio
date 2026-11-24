#!/usr/bin/env bash
# Downloads Sauce Labs' public "My Demo App" (Android) into 09-mobile/apps/ (gitignored).
# Source: https://github.com/saucelabs/my-demo-app-android/releases
set -euo pipefail
VERSION="${MDA_VERSION:-2.3.0}"
BUILD="${MDA_BUILD:-27}"
DIR="$(cd "$(dirname "$0")/.." && pwd)/apps"
mkdir -p "$DIR"
URL="https://github.com/saucelabs/my-demo-app-android/releases/download/${VERSION}/mda-${VERSION}-${BUILD}.apk"
OUT="$DIR/my-demo-app-android.apk"
echo "Downloading $URL"
# Resumable with retries: large GitHub release downloads can be reset by proxies mid-transfer
for attempt in $(seq 1 20); do
  if curl -fSL -C - --retry 3 --max-time 120 -o "$OUT" "$URL"; then
    break
  fi
  echo "attempt $attempt interrupted, resuming..."
done
unzip -tq "$OUT" >/dev/null || { echo "Downloaded file is not a valid APK" >&2; exit 1; }
echo "Saved $(du -h "$OUT" | cut -f1) to $OUT"
