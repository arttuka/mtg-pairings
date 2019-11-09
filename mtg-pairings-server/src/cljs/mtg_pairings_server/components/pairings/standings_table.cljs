(ns mtg-pairings-server.components.pairings.standings-table
  (:require [reagent.core :as reagent]
            [re-frame.core :refer [subscribe dispatch]]
            [reagent-material-ui.styles :refer [with-styles]]
            [goog.string :as gstring]
            [goog.string.format]
            [mtg-pairings-server.components.pairings.table :as table]
            [mtg-pairings-server.subscriptions.pairings :as subs]
            [mtg-pairings-server.util.styles :refer [ellipsis-overflow on-desktop on-mobile]]))

(defn styles [theme]
  (merge (table/table-styles theme)
         {:rank-column       {:text-align  :center
                              :font-weight 700
                              on-desktop   {:width "50px"}
                              on-mobile    {:width "40px"}}
          :player-column     {:text-align :left
                              on-desktop  {:min-width "300px"}
                              on-mobile   (merge {:max-width "calc(100vw - 262px)"}
                                                 ellipsis-overflow)}
          :points-column     {:text-align :center
                              on-desktop  {:width "50px"}
                              on-mobile   {:width "40px"}}
          :tiebreaker-column {:text-align :center
                              on-desktop  {:width "70px"}
                              on-mobile   {:width     "50px"
                                           :font-size "14px"}}}))

(defn percentage [n]
  (gstring/format "%.3f" (* 100 n)))

(defn standings-table* [{:keys [tournament-id round]}]
  (let [data (subscribe [::subs/standings tournament-id round])]
    (fn standings-render [{:keys [classes]}]
      (when (seq @data)
        (let [{:keys [table table-header table-row rank-column
                      player-column points-column tiebreaker-column]} classes]
          [:table {:class table}
           [:thead {:class table-header}
            [:tr
             [:th {:class rank-column}]
             [:th {:class player-column} "Pelaaja"]
             [:th {:class points-column} "Pist."]
             [:th {:class tiebreaker-column} "OMW"]
             [:th {:class tiebreaker-column} "PGW"]
             [:th {:class tiebreaker-column} "OGW"]]]
           [:tbody
            (for [standing @data]
              ^{:key (:team_name standing)}
              [:tr {:class table-row}
               [:td {:class rank-column} (:rank standing)]
               [:td {:class player-column} (:team_name standing)]
               [:td {:class points-column} (:points standing)]
               [:td {:class tiebreaker-column} (percentage (:omw standing))]
               [:td {:class tiebreaker-column} (percentage (:pgw standing))]
               [:td {:class tiebreaker-column} (percentage (:ogw standing))]])]])))))

(def standings-table ((with-styles styles)
                      standings-table*))
