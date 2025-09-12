#!/usr/bin/env bash
set -euo pipefail

# Simple dead-class finder using jdeps
ROOT="$(cd "$(dirname "$0")/../.." && pwd)"
REPORT_DIR="$ROOT/tools/deadcode/report"
EXCLUDES="$ROOT/tools/deadcode/excludes.txt"
mkdir -p "$REPORT_DIR"

function analyze_module(){
  local module="$1"
  local classes="$ROOT/$module/target/classes"
  local report="$REPORT_DIR/dead-classes-$module.txt"
  if [[ ! -d "$classes" ]]; then
    echo "⚠  $module: pas de classes compilées (build d'abord)."
    return
  fi
  echo "→ Module $module"
  local defined=$(find "$classes" -name '*.class' | sed "s|$classes/||;s|/|.|g;s|\.class$||" | sed 's/\\$.*//')
  local referenced=$(jdeps -verbose:class -cp "$classes" -R "$classes" 2>/dev/null | grep '->' | awk '{print $3}' | sed 's/\\$.*//' | sort -u)
  : > "$report"
  for cls in $(echo "$defined" | sort -u); do
    if [[ -f "$EXCLUDES" ]] && grep -qx "$cls" "$EXCLUDES"; then continue; fi
    if ! grep -qx "$cls" <<< "$referenced"; then echo "$cls" >> "$report"; fi
  done
  if [[ ! -s "$report" ]]; then rm "$report"; echo "   ✅ Aucune classe orpheline détectée."; else echo "   ⚠  $(wc -l < "$report") classe(s) potentiellement non utilisée(s). Rapport: $report"; fi
}

analyze_module client
analyze_module backend

echo "🏁 Audit terminé."
