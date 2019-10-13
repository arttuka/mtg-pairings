(ns mtg-pairings-server.components.pairings.table
  (:require [mtg-pairings-server.util.material-ui :as mui-util]))

(defn table-styles [{:keys [palette] :as theme}]
  {:table        {:line-height               "24px"
                  (mui-util/on-mobile theme) {:width "100%"}}
   :table-header {:line-height "36px"}
   :table-row    {"&:nth-child(odd)" {:background-color (get-in palette [:primary :100])}}})
