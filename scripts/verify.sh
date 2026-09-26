#!/usr/bin/env bash
set -euo pipefail

project_dir="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$project_dir"

case "${1:-backend}" in
  backend)
    ./gradlew --no-daemon :backend:check :backend:bootJar
    ;;
  contract)
    ./gradlew --no-daemon :backend:openApiValidate :backend:checkFrontendApi
    ;;
  frontend)
    cd frontend
    npm ci
    npm run format:check
    npm run lint
    npm run test:ci
    npm run build
    ;;
  all)
    "$project_dir/scripts/verify.sh" backend
    "$project_dir/scripts/verify.sh" contract
    "$project_dir/scripts/verify.sh" frontend
    ;;
  *)
    printf 'Usage: %s [backend|contract|frontend|all]\n' "$0" >&2
    exit 2
    ;;
esac
