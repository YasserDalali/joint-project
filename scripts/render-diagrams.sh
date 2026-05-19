#!/usr/bin/env bash
# Render the FinRisk PlantUML diagrams to PNG and SVG.
# Usage: bash scripts/render-diagrams.sh
set -euo pipefail

cd "$(dirname "$0")/.."

if ! command -v plantuml >/dev/null 2>&1; then
  echo "ERROR: 'plantuml' not found. Install with:  sudo apt install plantuml graphviz" >&2
  exit 1
fi

PUML=(
  plantuml/usecase.puml
  plantuml/activity-risk.puml
  plantuml/sequence-buy.puml
  plantuml/sequence-portfolio.puml
)

echo "Rendering PNG..."
plantuml -tpng "${PUML[@]}"
echo "Rendering SVG..."
plantuml -tsvg "${PUML[@]}"
echo "Done. Output:"
ls -la plantuml/*.png plantuml/*.svg
