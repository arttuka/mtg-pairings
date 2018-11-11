(ns mtg-pairings-server.components.filter
  (:require [re-frame.core :refer [subscribe dispatch]]
            [mtg-pairings-server.events :as events]
            [mtg-pairings-server.subscriptions :as subs]
            [mtg-pairings-server.components.date-picker :refer [date-picker]]))

(defn organizer-filter []
  (let [organizers (subscribe [::subs/organizers])
        value (subscribe [::subs/tournament-filter :organizer])]
    [:div.filter
     [:div "Turnausjärjestäjä"]
     [:select {:on-change #(dispatch [::events/tournament-filter [:organizer (.-value (.-target %))]])
               :value     @value}
      [:option {} ""]
      (for [organizer @organizers
            :when (not= organizer "")]
        ^{:key organizer}
        [:option {:value organizer}
         organizer])]]))

(defn date-filter []
  (let [from (subscribe [::subs/tournament-filter :date-from])
        to (subscribe [::subs/tournament-filter :date-to])]
    (fn date-filter-render []
      [:div.filter
       [:div "Päivämäärä"]
       [date-picker {:on-day-click #(dispatch [::events/tournament-filter [:date-from %]])
                     :selected-day @from}]
       " – "
       [date-picker {:on-day-click #(dispatch [::events/tournament-filter [:date-to %]])
                     :selected-day @to}]])))

(defn tournament-filters []
  (let []
    (fn tournament-filters-render []
      [:div.filters
       [organizer-filter]
       [date-filter]])))
