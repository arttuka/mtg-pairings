(ns mtg-pairings-server.components.button-toggle
  (:require [reagent.core :as reagent :refer [atom]]
            [reagent-material-ui.components :as ui]))

(defn switch-button [{:keys [label on-click selected?]}]
  [ui/button {:on-click on-click
              :color    (if selected? :primary :default)
              :variant  (if selected? :contained :outlined)}
   label])

(defn button-toggle [{:keys [value options] :as props}]
  (into [ui/button-group (merge {:full-width true
                                 :variant    :outlined}
                                (dissoc props :value :options))]
        (for [option options]
          (switch-button {:label     (:label option)
                          :on-click  (:on-click option)
                          :selected? (= value (:value option))}))))
