(ns mtg-pairings-server.subscriptions
  (:require [re-frame.core :refer [reg-sub]]))

(reg-sub :tournaments
  (fn [db _]
    (map (:tournaments db) (:tournament-ids db))))
