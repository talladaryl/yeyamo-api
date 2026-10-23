#!/usr/bin/env bash
set -euo pipefail

workflow_services="$(sed -n '/^[[:space:]]*matrix:/,/^[[:space:]]*steps:/p' .github/workflows/build-images.yml \
  | sed -n 's/.*service:[[:space:]]*\([^,}[:space:]]*\).*/\1/p' | sort)"
override_services="$(sed -n '/^services:/,$p' docker-compose.production.images.yml \
  | sed -n 's/^  \([a-z0-9-]*\):$/\1/p' | sort)"

if ! diff -u <(printf '%s\n' "$workflow_services") <(printf '%s\n' "$override_services"); then
  echo "Image contract mismatch: workflow matrix and production image override differ." >&2
  exit 1
fi

echo "Image contract OK: workflow matrix matches production image override."
