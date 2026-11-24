#!/usr/bin/env bash
# OWASP Dependency-Check (OWASP Top 10 A06: Vulnerable and Outdated Components) across ALL modules.
# Runs from the repo root and writes target/dependency-check/dependency-check-report.{html,json}.
# Fails (non-zero) when any dependency has a CVE with CVSS >= 7.
#
# NVD_API_KEY: request a free key at https://nvd.nist.gov/developers/request-an-api-key and export it
# (CI: repository secret). Without it the first NVD download is extremely slow / rate-limited.
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
cd "$ROOT"

if [[ -z "${NVD_API_KEY:-}" ]]; then
  echo "WARNING: NVD_API_KEY is not set; the NVD database download may take a very long time." >&2
fi

./mvnw -B org.owasp:dependency-check-maven:12.1.9:aggregate \
  -DnvdApiKey="${NVD_API_KEY:-}" \
  -DfailBuildOnCVSS=7 \
  -Dformats=HTML,JSON \
  -DoutputDirectory="$ROOT/target/dependency-check" \
  -DsuppressionFile="$ROOT/06-security-qa/dependency-check-suppressions.xml"

echo "Report: $ROOT/target/dependency-check/dependency-check-report.html"
