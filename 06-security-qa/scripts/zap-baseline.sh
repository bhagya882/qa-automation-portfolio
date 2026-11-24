#!/usr/bin/env bash
# OWASP ZAP baseline (passive) scan of an OWNED/AUTHORISED target only.
#   Usage: 06-security-qa/scripts/zap-baseline.sh <target-url>
#   e.g.   06-security-qa/scripts/zap-baseline.sh http://localhost:7070/search?q=test
# Start the target first (from repo root):
#   java -jar sample-app/target/sample-app-1.0.0-all.jar            # secure mode
#   java -jar sample-app/target/sample-app-1.0.0-all.jar --insecure # vulnerable lab
# Reports: 06-security-qa/target/zap/zap-report.{html,json,md}
# Exit code: 0 = no FAIL-level rule triggered (warnings allowed); non-zero = FAIL rule hit or scan error.
set -euo pipefail

TARGET="${1:?usage: zap-baseline.sh <target-url>}"
MODULE_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
OUT="$MODULE_DIR/target/zap"
IMAGE="${ZAP_IMAGE:-ghcr.io/zaproxy/zaproxy:stable}"

case "$TARGET" in
  http://localhost*|http://127.0.0.1*|https://localhost*) ;;
  *) echo "Refusing to scan non-local target '$TARGET' unless ZAP_AUTHORISED_TARGET=1 is set (written permission required)." >&2
     [[ "${ZAP_AUTHORISED_TARGET:-0}" == "1" ]] || exit 3 ;;
esac

command -v docker >/dev/null || { echo "docker is required" >&2; exit 3; }
mkdir -p "$OUT"
cp "$MODULE_DIR/zap/rules.tsv" "$OUT/rules.tsv"
chmod 777 "$OUT" 2>/dev/null || true   # container runs as user 'zap'

NETWORK_ARGS=()
if [[ "$(uname -s)" == "Linux" ]]; then
  NETWORK_ARGS=(--network host)        # CI runners: container shares host network, localhost works
else
  TARGET="${TARGET/localhost/host.docker.internal}"   # Docker Desktop (macOS/Windows)
  TARGET="${TARGET/127.0.0.1/host.docker.internal}"
fi

echo "ZAP baseline scan of $TARGET -> $OUT"
set +e
docker run --rm ${NETWORK_ARGS[@]+"${NETWORK_ARGS[@]}"} -v "$OUT":/zap/wrk/:rw "$IMAGE" \
  zap-baseline.py -t "$TARGET" -c rules.tsv -m 2 \
  -r zap-report.html -J zap-report.json -w zap-report.md \
  -I   # -I: do not fail on WARN, only on FAIL-level rules
rc=$?
set -e
echo "ZAP exit code: $rc (0=pass, 1=FAIL rules triggered, 3=error)"
exit $rc
