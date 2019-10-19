(ns mtg-pairings-server.styles.organizer
  (:require [garden.def :refer [defstyles]]
            [garden.selectors :refer [nth-child &]]
            [garden.units :refer [px vh vw percent]]
            [mtg-pairings-server.styles.common :refer [color ellipsis-overflow]]))

(defstyles seatings
  [:.organizer-seatings
   [:.seating
    ellipsis-overflow]])

(defstyles table
  [:.row
   {:line-height (px 22)
    :height      (px 24)
    :width       (px 470)
    :font-size   (px 17)
    :border      {:style :solid
                  :color (color :light-grey)
                  :width (px 1)}}]
  [:.table-number :.pod-number :.seat-number
   {:display     :inline-block
    :width       (px 40)
    :font-size   (px 20)
    :font-weight 700
    :text-align  :center
    :color       (color :dark-grey)}])

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
   [:.row
    [(& (nth-child "even"))
     {:background-color (color :light-grey)}]]
   table
   seatings
   clock]
  deck-construction)
