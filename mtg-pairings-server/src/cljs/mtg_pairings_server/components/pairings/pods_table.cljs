(ns mtg-pairings-server.components.pairings.pods-table
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
           {:pod-column {:text-align :center
                         on-desktop  {:width "100px"}
                         on-mobile   {:width "65px"}}
            :seat-column {:text-align :center
                          on-desktop  {:width "100px"}
                          on-mobile   {:width "65px"}}
            :player-column {on-desktop {:min-width "300px"}
                            on-mobile  (merge {:max-width "calc(100vw - 162px)"}
                                              ellipsis-overflow)}})))

(defn pods-table* [{:keys [tournament-id round]}]
  (let [data (subscribe [::subs/sorted-pods tournament-id round])
        sort-key (subscribe [::subs/pods-sort])]
    (fn pods-render [{:keys [classes]}]
      (when (seq @data)
        (let [{:keys [table table-header table-row
                      pod-column seat-column player-column]} classes]
          [:table {:class table}
           [:thead {:class table-header}
            [:tr
             [table/sortable-header {:class        pod-column
                                     :column       :pod
                                     :sort-key     @sort-key
                                     :dispatch-key ::events/sort-pods
                                     :label        "Pöytä"}]
             [:th {:class seat-column}
              "Paikka"]
             [table/sortable-header {:class        player-column
                                     :column       :team_name
                                     :sort-key     @sort-key
                                     :dispatch-key ::events/sort-pods
                                     :label        "Pelaaja"}]]]
           [:tbody
            (for [seat @data]
              ^{:key [(:team_name seat)]}
              [:tr {:class table-row}
               [:td {:class pod-column}
                (:pod seat)]
               [:td {:class seat-column}
                (:seat seat)]
               [:td {:class player-column}
                (:team_name seat)]])]])))))

(def pods-table ((with-styles styles) pods-table*))
