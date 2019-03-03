(ns mtg-pairings-server.styles.main
  (:require [garden.core :as garden]
            [garden.def :refer [defstyles]]
            [garden.stylesheet :refer [at-import]]
            [garden.units :refer [px percent]]
            [mtg-pairings-server.styles.bracket :as bracket]
            [mtg-pairings-server.styles.common :refer [color]]
            [mtg-pairings-server.styles.organizer :as organizer]
            [mtg-pairings-server.styles.table :as table]
            [mtg-pairings-server.styles.tournament :as tournament]
            [mtg-pairings-server.util.mobile :refer [when-desktop when-mobile]]))

(defstyles base
  (at-import "https://fonts.googleapis.com/css?family=Lato:700")
  (at-import "https://fonts.googleapis.com/css?family=Roboto:400,500,700")
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
    :color           (color :turquoise)}]
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

(defstyles main
  base
  own-tournaments
  table/styles
  bracket/styles
  organizer/styles
  tournament/styles
  mobile)

(def css (garden/css main))
