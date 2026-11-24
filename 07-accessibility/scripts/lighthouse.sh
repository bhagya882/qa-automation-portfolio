#!/usr/bin/env bash
# Lighthouse accessibility audit (Chrome must be installed; Node 18+).
# Usage: scripts/lighthouse.sh [url] [out-dir]
set -euo pipefail
URL="${1:-https://www.saucedemo.com/}"
OUT="${2:-target/lighthouse}"
mkdir -p "$OUT"
NAME="$(echo "$URL" | sed -E 's#https?://##; s#[^A-Za-z0-9]+#_#g; s#_$##')"
npx --yes lighthouse@12.2.1 "$URL" \
  --only-categories=accessibility \
  --output=html --output=json \
  --output-path="$OUT/$NAME" \
  --chrome-flags="--headless=new --no-sandbox" \
  --quiet
echo "Accessibility score: $(node -e "console.log(Math.round(require('./$OUT/$NAME.report.json').categories.accessibility.score*100))")"
echo "Reports: $OUT/$NAME.report.html, $OUT/$NAME.report.json"
