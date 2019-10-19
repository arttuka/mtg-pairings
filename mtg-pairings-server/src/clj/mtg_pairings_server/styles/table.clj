(ns mtg-pairings-server.styles.table
  (:require [garden.def :refer [defstyles]]
            [garden.selectors :refer [nth-child &]]
            [garden.units :refer [px percent]]
            [mtg-pairings-server.styles.common :refer [ellipsis-overflow color when-mobile when-desktop]]))

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
     {:width (percent 100)}])])
