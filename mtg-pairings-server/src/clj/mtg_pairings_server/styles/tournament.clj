(ns mtg-pairings-server.styles.tournament
  (:require [garden.def :refer [defstyles]]
            [garden.color :refer [rgba]]
            [garden.units :refer [px percent]]
            [mtg-pairings-server.styles.util :refer [when-desktop when-mobile]]
            [mtg-pairings-server.styles.variables :as variables]))

(defstyles filters
  [:.filters
   {:margin {:left  (px 10)
             :right (px 10)}}
   (when-mobile
    [:.filter
     {:width (percent 100)}])
   (when-desktop
    [:.filter
     {:display      :inline-block
      :margin-right (px 10)}])
   [:.filter-button
    {:position       :relative
     :vertical-align :bottom
     :margin         {:left   (px 16)
                      :bottom (px 8)}}]
   [:.filter
    {:position       :relative
     :vertical-align :bottom}
    [:.filter-label
     {:position  :absolute
      :top       0
      :left      0
      :color     (rgba 0 0 0 0.3)
      :font-size (px 12)}]
    [:&.date-filter
     {:width       (px 280)
      :height      (px 60)
      :padding-top (px 8)}]
    [:&.player-filter
     {:width       (px 200)
      :height      (px 60)
      :padding-top (px 24)}]]
   [:.date-picker
    {:position :relative
     :display  :inline-block}]])

(defstyles styles
  [:#tournaments :div#main-container
   [:.tournament
    [:.tournament-row
     {:margin-bottom (px 10)}]]
   [:.pager
    {:margin {:left  (px 10)
              :right (px 10)}}]]
  filters)
