(ns mtg-pairings-server.components.pairings.pairings-table
  (:require [reagent.core :as reagent]
            [re-frame.core :refer [subscribe dispatch]]
            [reagent-material-ui.components :as ui]
            [reagent-material-ui.styles :refer [with-styles]]
            [mtg-pairings-server.components.pairings.table :as table]
            [mtg-pairings-server.events.pairings :as events]
            [mtg-pairings-server.subscriptions.pairings :as subs]
            [mtg-pairings-server.util.mtg :refer [bye?]]
            [mtg-pairings-server.util.styles :refer [ellipsis-overflow on-desktop on-mobile]]))

(defn styles [{:keys [palette] :as theme}]
  (merge (table/table-styles theme)
         {:table-column    {:text-align :center
                            on-desktop  {:width "100px"}
                            on-mobile   {:width "65px"}}
          :player-1-column {:text-align :left
                            on-desktop  {:min-width "300px"}
                            on-mobile   {:max-width "calc(100vw - 197px)"}}
          :player-2-column {:min-width  "300px"
                            :text-align :left}
          :points-column   {:text-align :center
                            on-desktop  {:width "60px"}
                            on-mobile   {:width "50px"}}
          :result-column   {:text-align :center
                            on-desktop  {:width "60px"}
                            on-mobile   {:width "50px"}}
          :mobile-cell     {on-mobile (merge {:display :block
                                              :width   "100%"}
                                             ellipsis-overflow)}
          :player-2-cell   {on-mobile {:color (get-in palette [:text :secondary])}}
          :desktop         {on-mobile {:display :none}}
          :mobile          {on-desktop {:display :none}}}))

(defn pairing-row [{:keys [classes pairing]}]
  (let [bye (bye? pairing)
        {:keys [table-column table-row player-1-column player-2-column
                points-column result-column mobile-cell player-2-cell
                desktop mobile]} classes]
    [:tr {:class table-row}
     [:td {:class table-column}
      (when-not bye
        (:table_number pairing))]
     [:td {:class player-1-column}
      [:span {:class mobile-cell}
       (:team1_name pairing)]
      [:span {:class [mobile mobile-cell player-2-cell]}
       (:team2_name pairing)]]
     [:td {:class [desktop player-2-column]}
      (:team2_name pairing)]
     (if bye
       [:<>
        [:td {:class points-column}
         [:span {:class mobile-cell}
          (:team1_points pairing)]]
        [:td {:class result-column}]]
       [:<>
        [:td {:class points-column}
         [:span {:class mobile-cell}
          (:team1_points pairing)]
         [:span {:class desktop}
          " - "]
         [:span {:class [mobile-cell player-2-cell]}
          (:team2_points pairing)]]
        [:td {:class result-column}
         [:span {:class mobile-cell}
          (:team1_wins pairing)]
         [:span {:class desktop}
          " - "]
         [:span {:class [mobile-cell player-2-cell]}
          (:team2_wins pairing)]]])]))

(defn pairings-table* [{:keys [tournament-id round]}]
  (let [data (subscribe [::subs/sorted-pairings tournament-id round])
        sort-key (subscribe [::subs/pairings-sort])]
    (fn [{:keys [classes]}]
      (when (seq @data)
        [:table
         {:class (:table classes)}
         [:thead {:class (:table-header classes)}
          [:tr
           [table/sortable-header {:class        (:table-column classes)
                                   :column       :table_number
                                   :sort-key     @sort-key
                                   :dispatch-key ::events/sort-pairings
                                   :label        "Pöytä"}]
           [table/sortable-header {:class        (:player-1-column classes)
                                   :column       :team1_name
                                   :sort-key     @sort-key
                                   :dispatch-key ::events/sort-pairings
                                   :label        [:<>
                                                  [:span {:class (:desktop classes)}
                                                   "Pelaaja 1"]
                                                  [:span {:class (:mobile classes)}
                                                   "Pelaajat"]]}]
           [:th {:class [(:player-2-column classes)
                         (:desktop classes)]}
            "Pelaaja 2"]
           [:th {:class (:points-column classes)} "Pist."]
           [:th {:class (:results-column classes)} "Tulos"]]]
         [:tbody
          (doall (for [pairing @data]
                   ^{:key [(:team1_name pairing)]}
                   [pairing-row {:classes (if (= @sort-key :team1_name)
                                            classes
                                            (dissoc classes :player-2-cell))
                                 :pairing pairing}]))]]))))

(def pairings-table ((with-styles styles) pairings-table*))
