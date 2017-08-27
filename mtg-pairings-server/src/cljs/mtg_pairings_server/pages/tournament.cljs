(ns mtg-pairings-server.pages.tournament
  (:require [re-frame.core :refer [subscribe]]
            [mtg-pairings-server.components.tournament :refer [tournament tournament-header pairings standings pods]]))

(defn tournament-page [id]
  (let [data (subscribe [:tournament id])]
    (fn [id]
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
