#!/bin/bash

set -euo pipefail

mkdir -p resources/public/js
mkdir -p resources/public/css

pairingsjssha=$(sha1sum target/js/pairings-main.js)
pairingsjsfile="js/pairings-main.${pairingsjssha:0:8}.js"
cp target/js/pairings-main.js "resources/public/$pairingsjsfile"

decklistjssha=$(sha1sum target/js/decklist-main.js)
decklistjsfile="js/decklist-main.${decklistjssha:0:8}.js"
cp target/js/decklist-main.js "resources/public/$decklistjsfile"

csssha=$(sha1sum target/public/css/main.min.css)
cssfile="css/main.${csssha:0:8}.css"
cp target/public/css/main.min.css "resources/public/$cssfile"

echo "{:main-css    \"/$cssfile\"
 :pairings-js \"/$pairingsjsfile\"
 :decklist-js \"/$decklistjsfile\"}
" > resources/config.edn
