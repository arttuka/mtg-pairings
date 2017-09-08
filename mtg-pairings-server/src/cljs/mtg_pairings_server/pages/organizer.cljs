(ns mtg-pairings-server.pages.organizer
  (:require [re-frame.core :refer [subscribe]]
            [mtg-pairings-server.components.organizer :refer [menu pairings seatings pods standings clock]]))

(defn organizer-page []
  (let [organizer-mode (subscribe [:organizer-mode])
        hide-menu? (subscribe [:organizer :menu])]
    (fn []
      [:div#organizer-page
       [:style {:type "text/css"}
        "#header { display: none; }"]
       (when-not @hide-menu? [menu])
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
    "#header { display: none; }"]
   [menu]])

(defn deck-construction-table [pod-num seats name->seating]
  [:div.pod
   [:h3 "Pod " pod-num]
   (for [seat seats]
     [:div
      [:span.table (name->seating (:team_name seat))]
      [:span.name (:team_name seat)]])])

(defn deck-construction-tables []
  (let [seatings (subscribe [:organizer :seatings])
        pods (subscribe [:organizer :pods])]
    (fn []
      (let [name->seating (into {} (map (juxt :name :table_number) @seatings))
            pods (into (sorted-map) (group-by :pod (sort-by :team_name @pods)))]
        [:div#deck-construction
         [:style {:type "text/css"}
          "#header { display: none; }"]
         (for [[pod-number seats] pods]
           [deck-construction-table pod-number seats name->seating])]))))
