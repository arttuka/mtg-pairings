(ns mtg-pairings-server.styles.table
  (:require [garden.def :refer [defstyles]]
            [garden.selectors :refer [nth-child &]]
            [garden.units :refer [px percent]]
            [mtg-pairings-server.styles.common :refer [ellipsis-overflow color when-mobile when-desktop]]))

(defstyles pairings-table
  [:table.pairings-table
   [:th.players
    {:padding-left (px 30)}]
   [:.table :.points :.result
    {:text-align :center}]
   (when-desktop
    [:.table
     {:width (px 100)}]
    [:.points :.result
     {:width (px 60)}]
    [:.players :.players2
     {:min-width (px 300)}])
   (when-mobile
    [:th.table
     {:text-align :right}]
    [:.table
     {:width (px 65)}]
    [:.points :.result
     {:width (px 50)}]
    [:.players
     {:max-width "calc(100vw - 197px)"}]
    [:.player1 :.player2 :.team1-points :.team2-points :.team1-wins :.team2-wins
     (merge
      {:display :block
       :width   (percent 100)}
      ellipsis-overflow)]
    [:&.player-sorted
     [:.player2 :.team2-points :.team2-wins
      {:color (color :grey)}]])])

(defstyles seatings-table
  [:table.seatings-table
   [:th.player
    {:padding-left (px 30)}]
   [:.table
    {:text-align :center}]
   (when-desktop
    [:.table
     {:width (px 100)}]
    [:.player
     {:min-width (px 300)}])
   (when-mobile
    [:th.table
     {:text-align :right}]
    [:.table
     {:width (px 65)}]
    [:.player
     (merge
      {:max-width "calc(100vw - 97px)"}
      ellipsis-overflow)])])

(defstyles pods-table
  [:table.pods-table
   [:.pod :.seat
    {:text-align :center}]
   [:th.player
    {:padding-left (px 30)}]
   (when-desktop
    [:.pod :.seat
     {:width (px 100)}]
    [:.player
     {:min-width (px 300)}])
   (when-mobile
    [:.pod :.seat
     {:width (px 65)}]
    [:th.pod
     {:text-align :right}]
    [:.player
     (merge
      {:max-width "calc(100vw - 162px)"}
      ellipsis-overflow)])])

(defstyles styles
  [:table
   {:border-spacing 0}
   [:th :td
    {:padding         0
     :border-collapse :collapse}]]
  [:table.seatings-table :table.standings-table :table.pods-table
   [:th
    {:text-align :left
     :position   :relative}]
   [:th :td
    {:font-size       (px 16)
     :line-height     (px 24)}]
   [:tbody
    [:tr
     [(& (nth-child "odd"))
      {:background-color (color :light-blue)}]]]
   (when-mobile
    [:&
     {:width (percent 100)}])]
  seatings-table
  pods-table)
