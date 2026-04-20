#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
USECASE_DIR="$ROOT_DIR/../app/src/main/java/com/example/lifetogether/domain/usecase"

VIOLATIONS="$(rg -n "^import com\\.example\\.lifetogether\\.data\\." "$USECASE_DIR" || true)"

if [[ -n "$VIOLATIONS" ]]; then
  echo "Usecase boundary violations found (domain/usecase must not import com.example.lifetogether.data.*):"
  echo "$VIOLATIONS"
  exit 1
fi

echo "Usecase boundary check passed."
