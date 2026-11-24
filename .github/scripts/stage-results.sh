#!/usr/bin/env bash
# Copies test outputs of the given modules into ./ci-results/<module>/target/... (plus the shared
# target/allure-results) so every CI artifact has the same layout. The quality gate derives the module name from
# the folder directly above target/, so results of one module from several jobs (e.g. per browser) are summed.
#
# Usage: .github/scripts/stage-results.sh <module> [<module> ...]
set -euo pipefail

out=ci-results
mkdir -p "$out"
{
  echo "job=${GITHUB_JOB:-local} run=${GITHUB_RUN_ID:-local} sha=${GITHUB_SHA:-local}"
  echo "modules=$*"
} > "$out/MANIFEST.txt"

for module in "$@"; do
  src="$module/target"
  [ -d "$src" ] || { echo "::warning::$src does not exist (module did not run?)"; continue; }
  mkdir -p "$out/$module/target"
  # Keep reports/evidence, drop build outputs and dependencies
  rsync -a \
    --exclude 'classes/' --exclude 'test-classes/' --exclude 'generated-*' --exclude 'maven-*' \
    --exclude '*.jar' --exclude 'dependency*/' --exclude 'node_modules/' \
    "$src/" "$out/$module/target/"
done

if [ -d target/allure-results ]; then
  mkdir -p "$out/target"
  cp -R target/allure-results "$out/target/"
fi
echo "Staged:"; find "$out" -maxdepth 3 -type d | sort
