(ns mtg-pairings-server.pages.tournament
  (:require [re-frame.core :refer [subscribe]]
            [mtg-pairings-server.components.tournament :refer [tournament]]))

(defn tournament-page [id]
  (let [data (subscribe [:tournament id])]
    (fn [id]
      [tournament @data])))
