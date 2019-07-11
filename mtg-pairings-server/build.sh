#!/bin/bash
set -euo pipefail

version=$(git log --pretty=format:'%h' -n 1)
tag=arttuka/pairings:$version

docker build . -t $tag --network mtgsuomi-deployment_default --pull
docker tag $tag arttuka/pairings:latest
