(ns mtg-pairings-server.util.material-ui
  (:require [cljsjs.material-ui]
            [cljs-react-material-ui.core :refer [get-mui-theme]]
            [cljs-react-material-ui.reagent :as ui]
            [oops.core :refer [oget]]
            [mtg-pairings-server.styles.common :refer [palette]]))

(def theme (get-mui-theme
            {:palette palette}))

(defn text-field [props]
  (let [original-on-change (:on-change props)
        on-change (fn [event]
                    (original-on-change (oget event "target" "value")))]
    [ui/text-field (assoc props :on-change on-change)]))
