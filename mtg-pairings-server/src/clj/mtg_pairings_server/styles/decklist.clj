(ns mtg-pairings-server.styles.decklist
  (:require [garden.def :refer [defstyles]]
            [garden.units :refer [px]]
            [mtg-pairings-server.util.mobile :refer [when-desktop when-mobile]]))

(defstyles styles
  [:#decklist-submit
   {:position :relative}
   (when-desktop
    [:&
     {:max-width (px 880)
      :margin    "0 auto"}]
    [:.deck-table-container
     {:width          "50%"
      :display        :inline-block
      :vertical-align :top}])
   [:.deck-table
    [:th.quantity :td.quantity
     {:width (px 72)}]
    [:th.error :td.error
     {:width (px 48)}]]])
