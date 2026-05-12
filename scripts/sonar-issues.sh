#!/usr/bin/env bash
# Fetch unresolved SonarQube issues for the FinRisk project (JSON + short text).
#
# With forced authentication enabled, set a user token (not password):
#   My Account → Security → Generate token
#   export SONAR_TOKEN='squ_...'
#
# With anonymous browse enabled, omit SONAR_TOKEN.
#
# Usage:
#   export SONAR_URL='http://localhost:9000'   # optional
#   export SONAR_PROJECT_KEY='finrisk-joint-project'  # optional
#   bash scripts/sonar-issues.sh
set -euo pipefail

SONAR_URL="${SONAR_URL:-http://localhost:9000}"
SONAR_PROJECT_KEY="${SONAR_PROJECT_KEY:-finrisk-joint-project}"

url="${SONAR_URL}/api/issues/search?componentKeys=${SONAR_PROJECT_KEY}&resolved=false&ps=100&additionalFields=_all"

if [[ -n "${SONAR_TOKEN:-}" ]]; then
  out="$(curl -sS -u "${SONAR_TOKEN}:" "$url")"
else
  out="$(curl -sS "$url")"
fi

# JSON must be argv[1]: a pipe + heredoc would give the heredoc to Python as stdin
# (consuming stdin for the script), so json.load(sys.stdin) would never see curl output.
python3 - "$out" <<'PY'
import json, sys
try:
    data = json.loads(sys.argv[1])
except json.JSONDecodeError as e:
    print("Invalid JSON from SonarQube:", e, file=sys.stderr)
    sys.exit(2)
total = data.get("total", 0)
print(f"Unresolved issues: {total}\n")
for issue in data.get("issues", []):
    sev = issue.get("severity", "?")
    typ = issue.get("type", "?")
    rule = issue.get("rule", "")
    msg = issue.get("message", "")
    comp = issue.get("component", "").split(":")[-1]
    line = issue.get("line", "")
    loc = f"{comp}:{line}" if line else comp
    print(f"[{sev}] {typ} {loc}")
    print(f"    {rule}")
    print(f"    {msg}\n")
PY
