(ns mtg-pairings-server.components.organizer.deck-construction
  (:require [re-frame.core :refer [subscribe]]
            [reagent-material-ui.styles :refer [with-styles]]
            [mtg-pairings-server.subscriptions.pairings :as subs]
            [mtg-pairings-server.util.styles :refer [ellipsis-overflow]]))

(def styles
  {:root   {:display         :flex
            :flex-wrap       :wrap
            :justify-content :flex-start}
   :pod    {:width     "30%"
            :margin    "0 0.5%"
            :font-size 18}
   :player (merge ellipsis-overflow
                  {:line-height "24px"})
   :table  {:display :inline-block
            :width   40}})

(defn deck-construction-table [{:keys [classes pod seats name->seating]}]
  [:div {:class (:pod classes)}
   [:h3 "Pod " pod]
   (for [seat seats]
     [:div {:class (:player classes)}
      [:span {:class (:table classes)}
       (name->seating (:team_name seat))]
      (:team_name seat)])])

(defn deck-construction-tables* [props]
  (let [seatings (subscribe [::subs/organizer :seatings])
        pods (subscribe [::subs/organizer :pods])]
    (fn [{:keys [classes]}]
      (let [name->seating (into {} (map (juxt :name :table_number) @seatings))
            pods (into (sorted-map) (group-by :pod (sort-by :team_name @pods)))]
        [:div {:class (:root classes)}
         (for [[pod-number seats] pods]
           [deck-construction-table {:classes       classes
                                     :pod           pod-number
                                     :seats         seats
                                     :name->seating name->seating}])]))))

(def deck-construction-tables ((with-styles styles) deck-construction-tables*))
