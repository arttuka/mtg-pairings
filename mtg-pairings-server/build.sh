#!/usr/bin/env bash
set -euxo pipefail

lein clean
lein test
lein kibit
lein eastwood
lein cljfmt check
lein with-profile provided,prod do \
  clean, \
  fig:min pairings, \
  fig:min decklist, \
  buster, \
  uberjar

version=$(git log --pretty=format:'%h' -n 1)
tag=arttuka/pairings:$version
docker build . -t $tag --pull
docker tag $tag arttuka/pairings:latest
