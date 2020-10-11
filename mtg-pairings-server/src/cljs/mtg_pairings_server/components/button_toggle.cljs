(ns mtg-pairings-server.components.button-toggle
  (:require [reagent.core :as reagent :refer [atom]]
            [reagent-material-ui.core.button :refer [button]]
            [reagent-material-ui.core.button-group :refer [button-group]]
            [reagent-material-ui.styles :refer [with-styles]]
            [mtg-pairings-server.util.styles :refer [fade]]))

(defn inverted-styles [{:keys [palette]}]
  {:contained-primary {:background-color :white
                       :color            (get-in palette [:primary :dark])
                       "&:hover"         {:background-color (fade "#fff" 0.8)}}
   :outlined          {:color        :white
                       :border-color (fade "#fff" 0.9)
                       "&:hover"     {:border-color (fade "#fff" 0.8)}}})

(def inverted-button ((with-styles inverted-styles) button))

(defn switch-button [{:keys [label on-click selected? invert]}]
  [(if invert inverted-button button)
   {:on-click on-click
    :color    (if selected? :primary :inherit)
    :variant  (if selected? :contained :outlined)}
   label])

(defn button-toggle [{:keys [value options invert] :as props}]
  (into [button-group (merge {:full-width true
                              :variant    :outlined}
                             (dissoc props :value :options :invert))]
        (for [option options]
          (switch-button {:label     (:label option)
                          :on-click  (:on-click option)
                          :selected? (= value (:value option))
                          :invert    invert}))))
