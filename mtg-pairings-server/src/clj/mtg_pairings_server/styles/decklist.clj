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
      :vertical-align :top}]
    [:#player-info
     [:.full-width
      {:width "100%"}]
     [:.half-width
      {:width   "calc(50% - 24px)"
       :display :inline-block}
      [:&.left
       {:margin-right (px 24)}]
      [:&.right
       {:margin-left (px 24)}]]])
   (when-mobile
    [:#player-info
     [:.full-width :.half-width
      {:width   "100%"
       :display :block}]])
   [:.intro
    [:.tournament-date :.tournament-name :.tournament-format
     {:font-weight :bold}]]
   [:h3
    {:margin-bottom 0}]
   [:.deck-table-container
    [:.deck-table
     [:th.quantity :td.quantity
      {:width (px 72)}]
     [:th.error :td.error
      {:width (px 48)}]]]])
