(ns mtg-pairings-server.components.link-button
  (:require [reagent-material-ui.core.button :refer [button]]
            [reagent-material-ui.styles :refer [with-styles]]))

(def link-button ((with-styles (fn [{:keys [palette]}]
                                 {:outlined {:color  (get-in palette [:text :primary])
                                             :border "1px solid rgba(0, 0, 0, 0.23)"}}))
                  button))
