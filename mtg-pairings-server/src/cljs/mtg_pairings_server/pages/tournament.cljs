(ns mtg-pairings-server.pages.tournament
  (:require [re-frame.core :refer [subscribe]]
            [mtg-pairings-server.components.tournament :refer [tournament tournament-header pairings]]))

(defn tournament-page [id]
  (let [data (subscribe [:tournament id])]
    (fn [id]
      [tournament @data])))

(defn pairings-page [id round]
  (let [tournament (subscribe [:tournament id])]
    (fn [id round]
      [:div#pairings
       [tournament-header (:id @tournament) (:name @tournament) (:day @tournament) (:organizer @tournament)]
       [pairings id round]])))
