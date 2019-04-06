(ns mtg-pairings-server.util.material-ui
  (:require [cljsjs.material-ui]
            [cljs-react-material-ui.reagent :as ui]
            [oops.core :refer [oget]]))

(defn get-theme [component]
  (js->clj (oget component "context" "muiTheme") :keywordize-keys true))

(defn text-field [props]
  (let [original-on-change (:on-change props)
        on-change (fn [event]
                    (original-on-change (oget event "target" "value")))]
    [ui/text-field (assoc props :on-change on-change)]))
