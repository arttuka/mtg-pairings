(ns mtg-pairings-server.middleware.db
  (:require [mtg-pairings-server.db :as db]))

(defn wrap-db-transaction [handler]
  (fn [request]
    (db/with-transaction
      (handler request))))
