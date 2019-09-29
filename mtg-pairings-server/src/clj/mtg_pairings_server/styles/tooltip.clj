(ns mtg-pairings-server.styles.tooltip
  (:require [garden.def :refer [defstyles]]
            [garden.selectors :refer [& hover]]
            [garden.units :refer [px percent vw]]
            [mtg-pairings-server.styles.common :refer [color]]))

(defstyles styles
  [:.tooltip-container
   {:position :relative
    :display  :inline-block}
   [:.tooltip
    {:visibility       :hidden
     :background-color (color :transparent-grey)
     :color            :white
     :width            :max-content
     :max-width        (vw 100)
     :padding          (px 6)
     :border-radius    (px 6)
     :line-height      :normal
     :position         :absolute
     :top              (percent 100)
     :left             (percent 50)
     :z-index          2}]
   [(& hover)
    [:.tooltip
     {:visibility :visible}]]])
