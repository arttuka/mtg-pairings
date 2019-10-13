(ns mtg-pairings-server.styles.main
  (:require [garden.core :as garden]
            [garden.def :refer [defstyles]]
            [garden.stylesheet :refer [at-import]]
            [garden.units :refer [px percent]]
            [mtg-pairings-server.styles.bracket :as bracket]
            [mtg-pairings-server.styles.common :refer [color when-desktop when-mobile when-print]]
            [mtg-pairings-server.styles.decklist :as decklist]
            [mtg-pairings-server.styles.organizer :as organizer]
            [mtg-pairings-server.styles.table :as table]
            [mtg-pairings-server.styles.tooltip :as tooltip]
            [mtg-pairings-server.styles.tournament :as tournament]))

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
    {:box-sizing :border-box}]]
  [:h1 :h2 :h3 :h4 :h5
   {:font-weight 500
    :line-height 1.1}]
  [:a
   {:text-decoration :none
    :color           (color :primary2-color)}]
  [:#main-container
   {:margin-bottom (px 10)}])

(defstyles mobile
  (when-mobile
   [:.hidden-mobile
    {:display "none !important"}])
  (when-desktop
   [:.hidden-desktop
    {:display "none !important"}]))

(defstyles own-tournaments
  [:#own-tournaments
   [:.mui-pairing
    [:.names
     {:display :inline-block}]
    [:.points
     {:float :right}]]])

(defstyles expandable-card
  [:.card-header-expandable
   {:cursor :pointer}
   [:.card-header-button
    {:transform  "rotate(0deg)"
     :transition "transform 300ms ease-in-out"}
    [:&.card-header-button-expanded
     {:transform "rotate(180deg)"}]]])

(defstyles main
  base
  own-tournaments
  expandable-card
  table/styles
  bracket/styles
  decklist/styles
  organizer/styles
  tooltip/styles
  tournament/styles
  mobile)

(def css (garden/css main))
