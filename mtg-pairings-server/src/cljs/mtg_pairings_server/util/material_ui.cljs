(ns mtg-pairings-server.util.material-ui
  (:require [cljsjs.material-ui]
            [cljs-react-material-ui.core :refer [get-mui-theme]]
            [cljs-react-material-ui.reagent :as ui]
            [oops.core :refer [oget]]))

(def theme (get-mui-theme
            {:palette {:primary1-color      "#90caf9"
                       :primary2-color      "#5d99c6"
                       :primary3-color      "#c3fdff"
                       :accent1-color       "#ec407a"
                       :accent2-color       "#b4004e"
                       :accent3-color       "#ff77a9"
                       :picker-header-color "#5d99c6"}}))

(defn get-theme [component]
  (js->clj (oget component "context" "muiTheme") :keywordize-keys true))

(defn text-field [props]
  (let [original-on-change (:on-change props)
        on-change (fn [event]
                    (original-on-change (oget event "target" "value")))]
    [ui/text-field (assoc props :on-change on-change)]))
