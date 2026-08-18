#!/usr/bin/env bash

set -euo pipefail

PROJECT_ROOT="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
ENV_FILE="$PROJECT_ROOT/.env"

if [[ ! -f "$ENV_FILE" ]]; then
    echo "The required .env file was not found in the project root: $PROJECT_ROOT" >&2
    exit 1
fi

# Load the dotenv file into the environment inherited by Maven and Spring Boot.
set -a
# shellcheck disable=SC1090
source "$ENV_FILE"
set +a

cd "$PROJECT_ROOT"
exec "$PROJECT_ROOT/mvnw" spring-boot:run "$@"
