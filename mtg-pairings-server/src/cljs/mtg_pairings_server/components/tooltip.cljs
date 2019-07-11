(ns mtg-pairings-server.components.tooltip
  (:require [reagent.core :as reagent]))

(defn tooltip [props & children]
  (into [:div.tooltip-container
         (dissoc props :label)
         [:div.tooltip
          (:label props)]]
        children))
