(ns mtg-pairings-server.subscriptions
  (:require [re-frame.core :refer [reg-sub]]))

(reg-sub :page
  (fn [db _]
    (:page db)))

(reg-sub :tournaments
  (fn [db _]
    (map (:tournaments db) (:tournament-ids db))))

(reg-sub :tournament
  (fn [db [_ id]]
    (get-in db [:tournaments id])))
