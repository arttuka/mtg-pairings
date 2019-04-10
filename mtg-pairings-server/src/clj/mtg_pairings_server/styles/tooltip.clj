(ns mtg-pairings-server.styles.tooltip
  (:require [garden.def :refer [defstyles]]
            [garden.selectors :refer [& hover]]
            [garden.units :refer [px percent]]
            [mtg-pairings-server.styles.common :refer [color]]))

(defstyles styles
  [:.tooltip-container
   {:position :relative
    :display  :inline-block}
   [:.tooltip
    {:visibility       :hidden
     :background-color (color :transparent-grey)
     :color            :white
     :padding          (px 6)
     :border-radius    (px 6)
     :position         :absolute
     :top              (percent 100)
     :left             (percent 50)
     :transform        "translate(-50%, 6px)"
     :z-index          2}]
   [(& hover)
    [:.tooltip
     {:visibility :visible}]]])
