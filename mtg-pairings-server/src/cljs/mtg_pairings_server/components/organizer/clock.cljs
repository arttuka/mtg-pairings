(ns mtg-pairings-server.components.organizer.clock
  (:require [reagent.core :refer [atom]]
            [re-frame.core :refer [subscribe]]
            [reagent-material-ui.colors :as colors]
            [reagent-material-ui.styles :refer [with-styles]]
            [mtg-pairings-server.components.organizer.common :refer [column header row number-style player-style]]
            [mtg-pairings-server.subscriptions.pairings :as subs]))

(def clock-styles {:root    {:font-size   "32vw"
                             :font-weight :bold
                             :text-align  :center
                             :font-family "Lato, Helvetica, sans-serif"
                             :color       (:A700 colors/green)}
                   :timeout {:color (:A700 colors/red)}})

(defn clock* [props]
  (let [c (subscribe [::subs/organizer :clock])]
    (fn clock-render [{:keys [classes]}]
      [:div {:class [(:root classes)
                     (when (:timeout @c) (:timeout classes))]}
       (:text @c)])))

(def clock ((with-styles clock-styles) clock*))
