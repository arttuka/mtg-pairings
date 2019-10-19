(ns mtg-pairings-server.components.pairings.seatings-table
  (:require [reagent.core :as reagent]
            [re-frame.core :refer [subscribe dispatch]]
            [reagent-material-ui.styles :refer [with-styles]]
            [mtg-pairings-server.components.pairings.table :as table]
            [mtg-pairings-server.events.pairings :as events]
            [mtg-pairings-server.styles.common :refer [ellipsis-overflow]]
            [mtg-pairings-server.subscriptions.pairings :as subs]
            [mtg-pairings-server.util.material-ui :as mui-util]))

(defn styles [theme]
  (merge (table/table-styles theme)
         (let [on-mobile (mui-util/on-mobile theme)
               on-desktop (mui-util/on-desktop theme)]
           {:table-column  {:text-align :center
                            on-desktop  {:width "100px"}
                            on-mobile   {:width "65px"}}
            :player-column {on-desktop {:min-width "300px"}
                            on-mobile  (merge {:max-width "calc(100vw - 97px)"}
                                              ellipsis-overflow)}})))

(defn seatings-table* [{:keys [tournament-id round]}]
  (let [data (subscribe [::subs/sorted-seatings tournament-id round])
        sort-key (subscribe [::subs/seatings-sort])]
    (fn seatings-render [{:keys [classes]}]
      (when (seq @data)
        (let [{:keys [table table-header table-row
                      table-column player-column]} classes]
          [:table {:class table}
           [:thead {:class table-header}
            [:tr
             [table/sortable-header {:class        table-column
                                     :column       :table_number
                                     :sort-key     @sort-key
                                     :dispatch-key ::events/sort-seatings
                                     :label        "Pöytä"}]
             [table/sortable-header {:class        player-column
                                     :column       :name
                                     :sort-key     @sort-key
                                     :dispatch-key ::events/sort-seatings
                                     :label        "Pelaaja"}]]]
           [:tbody
            (for [seat @data]
              ^{:key [(:name seat)]}
              [:tr {:class table-row}
               [:td {:class table-column}
                (:table_number seat)]
               [:td {:class player-column}
                (:name seat)]])]])))))

(def seatings-table ((with-styles styles) seatings-table*))
