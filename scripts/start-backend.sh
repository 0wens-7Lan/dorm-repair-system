#!/usr/bin/env bash
# AI-assisted: environment validation; actual credentials stay outside source control.
set -euo pipefail
cd "$(dirname "$0")/../backend"
: "${JWT_SECRET:?Set JWT_SECRET to a random value containing at least 32 bytes}"
if [[ -n "${JAVA_HOME:-}" ]]; then export PATH="$JAVA_HOME/bin:$PATH"; fi
if ! command -v mvn >/dev/null; then
  echo 'Install Maven 3.8+ and put mvn on PATH.' >&2; exit 1
fi
mvn verify
exec java -jar target/dorm-repair-backend-1.0.0.jar
