(ns mtg-pairings-server.pages.tournament
  (:require [re-frame.core :refer [subscribe]]
            [mtg-pairings-server.components.tournament :refer [tournament-list tournament tournament-header pairings standings pods seatings]]))

(defn tournaments-page []
  [tournament-list])

(defn tournament-page [id]
  (let [data (subscribe [:tournament id])]
    (fn tournament-page-render [id]
      [tournament @data])))

(defn pairings-page [id round]
  [:div#pairings
   [tournament-header id]
   [pairings id round]])

(defn standings-page [id round]
  [:div#standings
   [tournament-header id]
   [standings id round]])

(defn pods-page [id round]
  [:div#pods
   [tournament-header id]
   [pods id round]])

(defn seatings-page [id]
  [:div#seatings
   [tournament-header id]
   [seatings id]])
