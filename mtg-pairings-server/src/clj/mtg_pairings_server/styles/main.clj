(ns mtg-pairings-server.styles.main
  (:require [garden.core :as garden]
            [garden.def :refer [defstyles]]
            [garden.stylesheet :refer [at-import]]
            [garden.units :refer [px percent]]
            [mtg-pairings-server.styles.common :refer [color when-desktop when-mobile when-print]]
            [mtg-pairings-server.styles.decklist :as decklist]
            [mtg-pairings-server.styles.organizer :as organizer]
            [mtg-pairings-server.styles.tooltip :as tooltip]))

(defstyles base
  (at-import "https://fonts.googleapis.com/css?family=Lato:700")
  (at-import "https://fonts.googleapis.com/css?family=Roboto:400,500,700")
  (when-print
   ["@page"
    {:size "A4"}])
  [:html
   {:-webkit-text-size-adjust "100%"
    :-ms-text-size-adjust     "100%"}]
  [:html :body
   {:margin  0
    :padding 0
    :border  0}]
  [:*
   {:font-family "Roboto, sans-serif"
    :box-sizing  :border-box}
   [:&:before :&:after
    {:box-sizing :border-box}]])

(defstyles mobile
  (when-mobile
   [:.hidden-mobile
    {:display "none !important"}])
  (when-desktop
   [:.hidden-desktop
    {:display "none !important"}]))


(defstyles main
  base
  decklist/styles
  organizer/styles
  tooltip/styles
  mobile)

(def css (garden/css main))
