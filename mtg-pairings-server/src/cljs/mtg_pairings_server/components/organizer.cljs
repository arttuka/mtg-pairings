(ns mtg-pairings-server.components.organizer
  (:require [reagent.core :refer [atom]]
            [re-frame.core :refer [dispatch subscribe]]
            [reagent-material-ui.components :as ui]
            [reagent-material-ui.icons.zoom-out-map :refer [zoom-out-map]]
            [goog.string :as gstring]
            [mtg-pairings-server.components.organizer.menu :refer [round-select]]
            [mtg-pairings-server.events.pairings :as events]
            [mtg-pairings-server.subscriptions.pairings :as subs]
            [mtg-pairings-server.util :refer [cls indexed]]
            [mtg-pairings-server.util.material-ui :refer [wrap-on-change]]
            [mtg-pairings-server.util.mtg :refer [bye? duplicate-pairings]]))

(defn clock []
  (let [c (subscribe [::subs/organizer :clock])]
    (fn clock-render []
      [:div.organizer-clock
       {:class (when (:timeout @c) "timeout")}
       (:text @c)])))
