(ns mtg-pairings-server.util.material-ui
  (:require [oops.core :refer [oget]]))

(defn get-theme [component]
  (js->clj (oget component "context" "muiTheme") :keywordize-keys true))
