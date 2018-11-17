(ns mtg-pairings-server.material-ui.util
  (:require [oops.core :refer [oget]]))

(defn get-theme [component]
  (js->clj (oget component "context" "muiTheme") :keywordize-keys true))
