#!/usr/bin/env bash
set -euxo pipefail

lein clean
lein test
lein kibit
lein eastwood
lein cljfmt check
lein clean
npm run build
mkdir -p resources/public/js
cp target/public/js/*.js resources/public/js/
cp target/public/js/manifest.edn resources
lein uberjar

version=$(git log --pretty=format:'%h' -n 1)
tag=arttuka/pairings:$version
docker build . -t $tag --pull
docker tag $tag arttuka/pairings:latest
