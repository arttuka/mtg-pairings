#!/bin/bash

set -euo pipefail

mkdir -p resources/public/js
mkdir -p resources/public/css

jssha=$(sha1sum target/js/prod-main.js)
jsfile="js/prod-main.${jssha:0:8}.js"
cp target/js/prod-main.js "resources/public/$jsfile"

csssha=$(sha1sum target/public/css/main.min.css)
cssfile="css/main.${csssha:0:8}.css"
cp target/public/css/main.min.css "resources/public/$cssfile"

echo "{:main-css \"/$cssfile\"
 :main-js  \"/$jsfile\"}
" > resources/config.edn
