(ns mtg-pairings-server.styles.organizer
  (:require [garden.def :refer [defstyles]]
            [garden.units :refer [px vh vw percent]]
            [mtg-pairings-server.styles.util :refer [ellipsis-overflow]]
            [mtg-pairings-server.styles.variables :as variables]))

(defstyles pairings
  [:.organizer-pairings
   [:table-number
    {:display        :block
     :vertical-align :top}]
   [:.player
    (merge
     {:display       :inline-block
      :position      :relative
      :width         (px 214)
      :padding-right (px 25)}
     ellipsis-overflow)
    [:&.opponent
     {:color (variables/color :grey)}]]
   [:.points
    {:display    :inline-block
     :width      (px 25)
     :text-align :center
     :position   :absolute
     :right      0}]
   [:.bye
    {:background-color (variables/color :light-green)}]])

(defstyles pods
  [:.organizer-pods
   [:.seat
    ellipsis-overflow]])

(defstyles seatings
  [:.organizer-seatings
   [:.seating
    ellipsis-overflow]])

(defstyles standings
  [:.organizer-standings
   [:.row
    {:overflow :hidden}
    [:.rank :.player :.points :.omw :.pgw :.ogw
     {:display :inline-block}]
    [:.points :.omw :.pgw :.ogw
     {:text-align :center}]
    [:.rank
     {:text-align  :center
      :font-weight 700
      :color       (variables/color :dark-grey)}]
    [:.points :.rank
     {:width (px 40)}]
    [:.player
     (merge
      {:width (px 198)}
      ellipsis-overflow)]
    [:.omw
     {:width (px 70)}]
    [:.pgw :.ogw
     {:width     (px 60)
      :font-size (px 14)}]]])

(defstyles table
  [:.row
   {:line-height (px 22)
    :height      (px 24)
    :width       (px 470)
    :font-size   (px 17)
    :border      {:style :solid
                  :color (variables/color :light-grey)
                  :width (px 1)}}]
  [:.table-number :.pod-number :.seat-number
   {:display     :inline-block
    :width       (px 40)
    :font-size   (px 20)
    :font-weight 700
    :text-align  :center
    :color       (variables/color :dark-grey)}])

(defstyles clock
  [:.organizer-clock
   {:color       :green
    :font-size   (vw 32)
    :font-weight 700
    :text-align  :center
    :font-family "Lato, Helvetica, sans-serif"}
   [:&.timeout
    {:color :red}]])

(defstyles deck-construction
  [:#deck-construction
   [:.pod
    {:display   :inline-block
     :min-width (percent 30)
     :font-size (px 18)}
    [:.player
     {:line-height (px 24)}
     [:.table
      {:display :inline-block
       :width   (px 40)}]]]])

(defstyles styles
  [:#organizer-page
   {:position :relative}
   [:h2
    {:text-align :center
     :margin-top 0}]
   [:.column
    {:display        :flex
     :flex-direction :column
     :flex-wrap      :wrap
     :align-content  :space-around
     :align-items    :center
     :height         "calc(100vh - 112px)"}]
   [:&.no-menu
    [:.column
     {:height "calc(100vh - 46px)"}]]
   [:.even
    {:background-color (variables/color :light-grey)}]
   table
   pairings
   pods
   seatings
   standings
   clock]
  deck-construction)
