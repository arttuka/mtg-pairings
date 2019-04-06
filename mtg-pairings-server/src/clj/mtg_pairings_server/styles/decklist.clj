(ns mtg-pairings-server.styles.decklist
  (:require [garden.def :refer [defstyles]]
            [garden.units :refer [px]]
            [mtg-pairings-server.util.mobile :refer [when-desktop when-mobile]]))

(defstyles submit
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

(defstyles organizer
  [:#decklist-organizer-tournaments
   {:margin "12px 24px"}
   [:.tournaments
    [:th.date :td.date
     {:width (px 130)}]
    [:th.deadline :td.deadline
     {:width (px 160)}]
    [:th.decklists :td.decklists
     {:width (px 135)}]
    [:.tournament-link
     {:color       :black
      :display     :block
      :height      (px 48)
      :line-height (px 48)}]]])

(defstyles styles
  organizer
  submit)
