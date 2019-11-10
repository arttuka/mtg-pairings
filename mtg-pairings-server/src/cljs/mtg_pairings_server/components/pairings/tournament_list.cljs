(ns mtg-pairings-server.components.pairings.tournament-list
  (:require [reagent.core :as reagent]
            [re-frame.core :refer [subscribe]]
            [reagent-material-ui.components :as ui]
            [mtg-pairings-server.events.pairings :as events]
            [mtg-pairings-server.subscriptions.pairings :as subs]
            [mtg-pairings-server.routes.pairings :refer [tournaments-path]]
            [mtg-pairings-server.components.paging :refer [with-paging]]
            [mtg-pairings-server.components.pairings.filter :refer [filters]]
            [mtg-pairings-server.components.pairings.tournament :refer [tournament]]))

(defn newest-tournaments-list []
  (let [tournaments (subscribe [::subs/newest-tournaments])
        translate (subscribe [::subs/translate])]
    (fn []
      (let [translate @translate]
        [ui/list
         [ui/list-item
          [ui/list-item-text {:primary (reagent/as-element
                                        [ui/typography {:variant :h5}
                                         (translate :tournaments.active)
                                         " | "
                                         [ui/link {:href (tournaments-path)}
                                          (translate :tournaments.to-archive)]])}]]
         (if-let [ts (seq @tournaments)]
           [:<>
            (for [t ts]
              ^{:key (:id t)}
              [tournament {:data       t
                           :list-item? true}])]
           [ui/list-item
            [ui/list-item-text {:primary                  (translate :tournaments.no-active-tournaments)
                                :primary-typography-props {:variant :h6}}]])]))))

(defn tournament-list-render [tournaments]
  [ui/list
   (for [t tournaments]
     ^{:key (:id t)}
     [tournament {:data       t
                  :list-item? true}])])

(defn tournament-list []
  [:<>
   [filters]
   [with-paging ::events/tournaments-page ::subs/tournaments-page ::subs/filtered-tournaments
    tournament-list-render]])
