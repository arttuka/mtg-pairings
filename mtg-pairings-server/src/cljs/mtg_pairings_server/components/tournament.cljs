(ns mtg-pairings-server.components.tournament
  (:require [reagent.core :as reagent]
            [re-frame.core :refer [subscribe dispatch]]
            [reagent-material-ui.components :as ui]
            [reagent-material-ui.icons.expand-more :refer [expand-more]]
            [reagent-material-ui.icons.keyboard-arrow-down :refer [keyboard-arrow-down]]
            [reagent-material-ui.styles :as styles]
            [goog.string :as gstring]
            [goog.string.format]
            [mtg-pairings-server.events.pairings :as events]
            [mtg-pairings-server.subscriptions.pairings :as subs]
            [mtg-pairings-server.subscriptions.common :as common-subs]
            [mtg-pairings-server.routes.pairings :refer [tournaments-path tournament-path pairings-path standings-path pods-path seatings-path bracket-path]]
            [mtg-pairings-server.components.paging :refer [with-paging]]
            [mtg-pairings-server.components.pairings.filter :refer [filters]]
            [mtg-pairings-server.components.pairings.tournament :refer [tournament]]
            [mtg-pairings-server.util :refer [format-date indexed]]
            [mtg-pairings-server.util.mtg :refer [bye?]]))

(defn newest-tournaments-list []
  (let [tournaments (subscribe [::subs/newest-tournaments])]
    (fn newest-tournaments-list-render []
      [:div#tournaments
       [:h2.newest-header
        "Aktiiviset turnaukset | "
        [:a {:href (tournaments-path)}
         "Turnausarkistoon"]]
       (if-let [ts (seq @tournaments)]
         (for [t ts]
           ^{:key (:id t)}
           [tournament {:data       t
                        :list-item? true}])
         [:h3.no-active "Ei aktiivisia turnauksia."])])))

(defn tournament-list []
  [:div
   [filters]
   [with-paging ::events/tournaments-page ::subs/tournaments-page ::subs/filtered-tournaments
    (fn tournament-list-render [tournaments]
      [:div#tournaments
       (for [t tournaments]
         ^{:key (:id t)}
         [tournament {:data       t
                      :list-item? true}])])]])
