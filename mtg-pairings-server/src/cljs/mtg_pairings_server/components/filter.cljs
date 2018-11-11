(ns mtg-pairings-server.components.filter
  (:require [re-frame.core :refer [subscribe dispatch]]
            [mtg-pairings-server.events :as events]
            [mtg-pairings-server.subscriptions :as subs]))

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

(defn tournament-filters []
  (let []
    (fn tournament-filters-render []
      [:div.filters
       [organizer-filter]])))
