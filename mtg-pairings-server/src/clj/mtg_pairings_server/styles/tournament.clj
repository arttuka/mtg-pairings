(ns mtg-pairings-server.styles.tournament
  (:require [garden.def :refer [defstyles]]
            [garden.units :refer [px percent]]
            [mtg-pairings-server.styles.common :refer [when-desktop when-mobile]]))

(defstyles styles
  [:#tournaments :div#main-container
   [:.newest-header :.no-active
    {:margin-left (px 16)}]
   [:.pager
    {:margin (px 16)}
    (when-mobile
     [:.page-button
      {:width     "calc((100vw - 32px)/9) !important"
       :min-width "calc((100vw - 32px)/9) !important"}])]])
