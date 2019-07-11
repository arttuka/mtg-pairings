(ns mtg-pairings-server.components.decklist.print
  (:require [reagent.core :as reagent :refer [atom]]
            [cljsjs.material-ui]
            [cljs-react-material-ui.reagent :as ui]
            [mtg-pairings-server.routes.decklist :as routes]
            [mtg-pairings-server.util :refer [format-date]]
            [mtg-pairings-server.util.decklist :refer [card-types type->header]]))

(defn decklist-card [card]
  [:div.card
   [:div.quantity
    (:quantity card)]
   [:div.name
    (:name card)]])

(defn render-decklist [decklist tournament]
  (let [{:keys [player main side id], counts :count} decklist
        {:keys [last-name first-name dci deck-name]} player
        [l1 l2 l3] last-name
        dci (vec dci)
        {tournament-id :id, tournament-name :name, date :date} tournament]
    [:div.print-decklist
     [:div.first-letters
      [:div.label "Alkukirjaimet"]
      [:div.letter l1]
      [:div.letter l2]
      [:div.letter l3]]
     [:div.deck-info
      [:div.tournament-date
       [:div.label "Päivä:"]
       [:div.value (format-date date)]]
      [:div.tournament-name
       [:div.label "Turnaus:"]
       [:div.value
        [:a {:href (routes/organizer-tournament-path {:id tournament-id})}
         tournament-name]]]
      [:div.deck-name
       [:div.label "Pakka:"]
       [:div.value deck-name]]]
     [:div.player-info
      [:div.name
       [:div.last-name
        [:div.label "Sukunimi:"]
        [:div.value last-name]]
       [:div.first-name
        [:div.label "Etunimi:"]
        [:div.value first-name]]]
      [:div.dci
       [:div.label "DCI:"]
       [:div.value
        (for [index (range 10)]
          ^{:key (str id "--dci--" index)}
          [:span.digit (get dci index)])]]]
     [:div.decklists
      [:div.maindeck
       [:h3 "Maindeck (" (:main counts) ")"]
       (into [:div.cards]
             (mapcat (fn [type]
                       (when-let [cards (get main type)]
                         (list* [:h4.type-header (type->header type)]
                                (for [card cards]
                                  [decklist-card card])))))
             card-types)]
      [:div.sideboard
       [:h3 "Sideboard (" (:side counts) ")"]
       (into [:div.cards]
             (for [card side]
               [decklist-card card]))]]]))
