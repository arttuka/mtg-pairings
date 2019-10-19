(ns mtg-pairings-server.components.pairings.pairings-table
  (:require [reagent.core :as reagent]
            [re-frame.core :refer [subscribe dispatch]]
            [reagent-material-ui.components :as ui]
            [reagent-material-ui.styles :refer [styled with-styles]]
            [mtg-pairings-server.components.pairings.table :as table]
            [mtg-pairings-server.events.pairings :as events]
            [mtg-pairings-server.styles.common :refer [ellipsis-overflow]]
            [mtg-pairings-server.subscriptions.pairings :as subs]
            [mtg-pairings-server.subscriptions.common :as common-subs]
            [mtg-pairings-server.util.material-ui :as mui-util]
            [mtg-pairings-server.util.mtg :refer [bye?]]))

(defn styles [{:keys [palette] :as theme}]
  (merge (table/table-styles theme)
         (let [on-mobile (mui-util/on-mobile theme)
               on-desktop (mui-util/on-desktop theme)]
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
            :player-2-cell   {on-mobile {:color (get-in palette [:text :secondary])}}})))

(defn pairing-row [{:keys [classes pairing mobile?]}]
  (let [bye (bye? pairing)
        {:keys [table-column table-row player-1-column player-2-column
                points-column result-column mobile-cell player-2-cell]} classes]
    [:tr {:class table-row}
     [:td {:class table-column}
      (when-not bye
        (:table_number pairing))]
     [:td {:class player-1-column}
      [:span {:class mobile-cell}
       (:team1_name pairing)]
      (when mobile?
        [:span {:class [mobile-cell player-2-cell]}
         (:team2_name pairing)])]
     (when-not mobile?
       [:td {:class player-2-column}
        (:team2_name pairing)])
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
         (when-not mobile? " - ")
         [:span {:class [mobile-cell player-2-cell]}
          (:team2_points pairing)]]
        [:td {:class result-column}
         [:span {:class mobile-cell}
          (:team1_wins pairing)]
         (when-not mobile? " - ")
         [:span {:class [mobile-cell player-2-cell]}
          (:team2_wins pairing)]]])]))

(defn pairings-table* [{:keys [tournament-id round]}]
  (let [data (subscribe [::subs/sorted-pairings tournament-id round])
        sort-key (subscribe [::subs/pairings-sort])
        mobile? (subscribe [::common-subs/mobile?])]
    (fn pairings-render [{:keys [classes]}]
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
                                   :label        (if @mobile? "Pelaajat" "Pelaaja 1")}]
           (when-not @mobile?
             [:th {:class (:player-2-column classes)} "Pelaaja 2"])
           [:th {:class (:points-column classes)} "Pist."]
           [:th {:class (:results-column classes)} "Tulos"]]]
         [:tbody
          (doall (for [pairing @data]
                   ^{:key [(:team1_name pairing)]}
                   [pairing-row {:classes (if (= @sort-key :team1_name)
                                            classes
                                            (dissoc classes :player-2-cell))
                                 :pairing pairing
                                 :mobile? @mobile?}]))]]))))

(def pairings-table ((with-styles styles) pairings-table*))
