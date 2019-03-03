(ns mtg-pairings-server.styles.table
  (:require [garden.def :refer [defstyles]]
            [garden.units :refer [px percent]]
            [mtg-pairings-server.styles.variables :as variables]))

(defstyles pairings-table
  [:table.pairings-table
   [:th
    [:&.players
     {:padding-left (px 30)}]]
   [:.table
    {:width      (px 100)
     :text-align :center}]
   [:.points :.result
    {:width      (px 60)
     :text-align :center}]])

(defstyles standings-table
  [:table.standings-table
   [:.points :.rank
    {:width      (px 50)
     :text-align :center}]
   [:.omw :.pgw :.ogw
    {:width      (px 70)
     :text-align :center}]])

(defstyles pods-table
  [:table.pods-table
   [:.pod :.seat
    {:width      (px 100)
     :text-align :center}]
   [:th
    [:&.player
     {:padding-left (px 30)}]]])

(defstyles styles
  [:table
   {:border-spacing 0
    :width          (percent 100)}
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
     {:background-color :white}]]]
  pairings-table
  standings-table
  pods-table)
