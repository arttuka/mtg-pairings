#!/usr/bin/env bash

set -euo pipefail

echo "Starting PostgreSQL"
docker-compose up -d

echo "Applying migrations"
lein migrate

echo "All done!"
