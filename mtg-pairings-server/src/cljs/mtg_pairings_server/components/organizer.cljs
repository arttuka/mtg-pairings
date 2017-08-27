(ns mtg-pairings-server.components.organizer
  (:require [mtg-pairings-server.util.util :refer [cls]]))

(defn pairing [data even? display-round? pairing?]
  [:div.pairing {:class (cls {:even     even?
                              :odd      (not even?)
                              :no-round (not display-round?)})}
   (when (and display-round? pairing?)
     [:h4 (str "Kierros " (:round_number data))])
   (when-not pairing?
     [:h4 "Seating"])
   [:span.table-number (:table_number data)]
   (when-not pairing?
     [:span
      [:div.names (:team1_name data)]])
   (when pairing?
     [:span
      [:div.names
       [:span.player (str (:team1_name data) " (" (:team1_points data) ")")]
       [:span.hidden-xs " - "]
       [:br.hidden-sm.hidden-md.hidden-lg]
       [:span.opponent (str (:team2_name data) " (" (:team2_points data) ")")]]
      [:div.points
       [:span.player (:team1_wins data)]
       [:span.hidden-xs " - "]
       [:br.hidden-sm.hidden-md.hidden-lg]
       [:span.opponent (:team2_wins data)]]])])
