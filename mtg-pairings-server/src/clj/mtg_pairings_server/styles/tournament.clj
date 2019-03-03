(ns mtg-pairings-server.styles.tournament
  (:require [garden.def :refer [defstyles]]
            [garden.units :refer [px percent]]
            [mtg-pairings-server.util.mobile :refer [when-desktop when-mobile]]))

(defstyles mobile-filters
  [:.mobile-filters
   [:.filter
    [:.organizer-filter
     {:width      "100% !important"
      :margin-top "-14px"}]]
   [:.date-filter
    {:width "100% !important"}
    [:.date-picker
     [:>div
      [:>div:first-child
       {:width "calc(50vw - 26px) !important"}]]]]
   [:.player-filter
    {:width (percent 100)}
    [:.rc-slider
     {:width  "calc(100% - 14px)"
      :margin {:left  (px 7)
               :right (px 7)}}]]
   [:.filter-button
    {:width "calc(100vw - 32px) !important"}]])

(defstyles desktop-filters
  [:.desktop-filters
   {:margin {:left  (px 16)
             :right (px 16)}}
   [:.filter
    {:display        :inline-block
     :margin-right   (px 10)
     :vertical-align :bottom}]
   [:.player-filter
    {:width (px 200)}]
   [:.date-filter
    {:width (px 280)}]
   [:.filter-button
    {:position       :relative
     :vertical-align :bottom
     :margin         {:left   (px 16)
                      :bottom (px 8)}}]])

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
    {:height      (px 60)
     :padding-top (px 24)}]
   [:.date-filter
    {:height     (px 58)
     :margin-top (px 14)
     :padding-top (px 8)}
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
