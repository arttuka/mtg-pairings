(ns mtg-pairings-server.styles.organizer
  (:require [garden.def :refer [defstyles]]
            [garden.selectors :refer [nth-child &]]
            [garden.units :refer [px vh vw percent]]
            [mtg-pairings-server.styles.common :refer [color ellipsis-overflow]]))

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
   {:position :relative}]
  deck-construction)
