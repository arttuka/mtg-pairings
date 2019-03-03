(ns mtg-pairings-server.styles.table
  (:require [garden.def :refer [defstyles]]
            [garden.units :refer [px percent]]
            [mtg-pairings-server.styles.util :refer [ellipsis-overflow when-mobile when-desktop]]
            [mtg-pairings-server.styles.variables :as variables]))

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
      ellipsis-overflow)])])

(defstyles standings-table
  [:table.standings-table
   [:.points :.omw :.pgw :.ogw
    {:text-align :center}]
   [:.rank
    {:text-align  :center
     :font-weight 700
     :color       (variables/color :dark-grey)}]
   (when-desktop
    [:.player
     {:min-width (px 300)}]
    [:.points :.rank
     {:width (px 50)}]
    [:.omw :.pgw :.ogw
     {:width (px 70)}])
   (when-mobile
    [:.points :.rank
     {:width (px 40)}]
    [:.player
     (merge
      {:max-width "calc(100vw - 262px)"}
      ellipsis-overflow)]
    [:.omw :.pgw :.ogw
     {:width     (px 50)
      :font-size (px 14)}])])

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
   [:th
    {:text-align :left
     :position   :relative}]
   [:th :td
    {:padding         0
     :border-collapse :collapse
     :font-size       (px 16)
     :line-height     (px 24)}]
   [:tr
    [:&.even
     {:background-color (variables/color :light-grey)}]
    [:&.odd
     {:background-color :white}]]
   (when-mobile
    [:&
     {:width (percent 100)}])]
  pairings-table
  seatings-table
  standings-table
  pods-table)
