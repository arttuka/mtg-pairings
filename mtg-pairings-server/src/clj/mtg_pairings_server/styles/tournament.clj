(ns mtg-pairings-server.styles.tournament
  (:require [garden.def :refer [defstyles]]
            [garden.units :refer [px percent]]
            [mtg-pairings-server.styles.common :refer [when-desktop when-mobile]]))

(defstyles mobile-filters
  [:.mobile-filters
   [:.organizer-filter :.date-filter :.player-filter
    {:width         (percent 100)
     :margin-bottom (px 8)}]
   [:.date-filter
    {:width (percent 100)}]
   [:.player-filter
    {:width   (percent 100)
     :padding {:left  (px 6)
               :right (px 6)}}]
   [:.filter-button
    {:width "calc(100vw - 32px)"}]])

(defstyles desktop-filters
  [:.desktop-filters
   {:margin {:left  (px 16)
             :right (px 16)
             :top   (px 16)}}
   [:.filter
    {:display        :inline-block
     :margin-right   (px 10)
     :vertical-align :top}]
   [:.organizer-filter
    {:width        (px 256)
     :margin-right (px 10)}]
   [:.player-filter
    {:width   (px 220)
     :padding {:left  (px 10)
               :right (px 10)}}]
   [:.date-filter
    {:width (px 280)}]
   [:.filter-button
    {:margin-top (px 8)}]])

(defstyles filters
  [:.filters
   [:.filter
    {:position :relative}
    [:.filter-label
     {:position  :absolute
      :top       0
      :left      0
      :color     "#b3b3b3"
      :font-size (px 12)}]]
   [:.player-filter
    {:height      (px 48)
     :padding-top (px 20)}]
   [:.date-filter
    {:height      (px 48)
     :padding-top (px 16)}
    [:.date-picker
     {:position :relative
      :display  :inline-block}]
    [:.separator
     {:display    :inline-block
      :text-align :center
      :width      (px 20)}]]]
  mobile-filters
  desktop-filters)

(defstyles styles
  [:#tournaments :div#main-container
   [:.newest-header :.no-active
    {:margin-left (px 16)}]
   [:.tournament
    [:.tournament-row
     {:margin-bottom (px 16)}
     (when-mobile
      [:.tournament-button
       {:width "calc(50vw - 16px) !important"}]
      [:.tournament-button-wide
       {:width "calc(100vw - 32px) !important"}])]]
   [:.pager
    {:margin (px 16)}
    (when-mobile
     [:.page-button
      {:width     "calc((100vw - 32px)/9) !important"
       :min-width "calc((100vw - 32px)/9) !important"}])]]
  filters)
