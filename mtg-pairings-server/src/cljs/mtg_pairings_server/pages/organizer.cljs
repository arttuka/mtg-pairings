(ns mtg-pairings-server.pages.organizer
  (:require [re-frame.core :refer [subscribe]]
            [mtg-pairings-server.components.organizer :refer [menu pairings seatings pods standings clock]]
            [mtg-pairings-server.subscriptions.pairings :as subs]))

(defn organizer-page []
  (let [organizer-mode (subscribe [::subs/organizer-mode])
        hide-organizer-menu? (subscribe [::subs/organizer :menu])]
    (fn organizer-page-render []
      [:div#organizer-page
       {:class (when @hide-organizer-menu? :no-menu)}
       [:style {:type "text/css"}
        "#header { display: none !important; }"]
       (case @organizer-mode
         :pairings [pairings]
         :seatings [seatings]
         :pods [pods]
         :standings [standings]
         :clock [clock]
         [:div])])))

(defn organizer-menu []
  [:div#organizer-page
   [:style {:type "text/css"}
    "#header { display: none !important; }"]
   [menu]])

(defn deck-construction-table [pod-num seats name->seating]
  [:div.pod
   [:h3 "Pod " pod-num]
   (for [seat seats]
     [:div.player
      [:span.table (name->seating (:team_name seat))]
      [:span.name (:team_name seat)]])])

(defn deck-construction-tables []
  (let [seatings (subscribe [::subs/organizer :seatings])
        pods (subscribe [::subs/organizer :pods])]
    (fn deck-construction-tables-render []
      (let [name->seating (into {} (map (juxt :name :table_number) @seatings))
            pods (into (sorted-map) (group-by :pod (sort-by :team_name @pods)))]
        [:div#deck-construction
         [:style {:type "text/css"}
          "#header { display: none !important; }"]
         (for [[pod-number seats] pods]
           [deck-construction-table pod-number seats name->seating])]))))
