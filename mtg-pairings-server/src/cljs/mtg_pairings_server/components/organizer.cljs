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

(defn seatings []
  (let [seatings (subscribe [::subs/organizer :seatings])
        tournament (subscribe [::subs/organizer :tournament])]
    (fn seatings-render []
      [:div.organizer-seatings
       [:h2 (str (:name @tournament) " - seatings")]
       [:div.column
        (for [s @seatings]
          ^{:key (:name s)}
          [:div.row.seating
           [:span.table-number (:table_number s)]
           [:span.name (:name s)]])]])))

(defn pods []
  (let [pods (subscribe [::subs/organizer :pods])
        tournament (subscribe [::subs/organizer :tournament])]
    (fn pods-render []
      [:div.organizer-pods
       [:h2 (str (:name @tournament) " - pods")]
       [:div.column
        (for [p @pods]
          ^{:key (:team_name p)}
          [:div.row.seat
           [:span.pod-number (:pod p)]
           [:span.seat-number (:seat p)]
           [:span.name (:team_name p)]])]])))

(defn percentage [n]
  (gstring/format "%.3f" (* 100 n)))

(defn standings []
  (let [standings (subscribe [::subs/organizer :standings])
        tournament (subscribe [::subs/organizer :tournament])
        standings-round (subscribe [::subs/organizer :standings-round])]
    (fn standings-render []
      [:div.organizer-standings
       [:h2 (str (:name @tournament) " - kierros " @standings-round)]
       [:div.column
        (for [{:keys [rank team_name points omw pgw ogw]} @standings]
          ^{:key (str "standings-" rank)}
          [:div.row.standing
           [:span.rank rank]
           [:span.player team_name]
           [:span.points points]
           [:span.omw (percentage omw)]
           [:span.pgw (percentage pgw)]
           [:span.ogw (percentage ogw)]])]])))

(defn clock []
  (let [c (subscribe [::subs/organizer :clock])]
    (fn clock-render []
      [:div.organizer-clock
       {:class (when (:timeout @c) "timeout")}
       (:text @c)])))
