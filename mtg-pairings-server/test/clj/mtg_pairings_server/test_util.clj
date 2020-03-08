(ns mtg-pairings-server.test-util
  (:require [korma.core :as sql]
            [config.core :refer [env]]
            [mount.core :as m]
            [taoensso.timbre :as timbre]
            [mtg-pairings-server.sql-db :as db]))

(def tables [db/seat db/pod db/pod-round db/seating db/pairing db/round
             db/team-players db/player db/team db/standings db/tournament db/user])

(def users {:user1 {:id       1
                    :username "user1"
                    :uuid     #uuid "8e6d5f6e-66bd-4f2d-a1fa-634140bd38e2"}
            :user2 {:id       2
                    :username "user2"
                    :uuid     #uuid "2b901ae8-02f4-4c95-afcd-af09b7137359"}})

(defn check-db! []
  (when-not (= "mtgsuomi_test" (:db-name env))
    (throw (IllegalStateException. "Not in test environment")))
  true)

(defn erase-db! []
  (check-db!)
  (doseq [table tables]
    (sql/delete table)))

(defn db-fixture [f]
  (check-db!)
  (timbre/swap-config! (fn [config] (assoc config :ns-blacklist ["org.eclipse.jetty.*" "io.netty.*" "com.zaxxer.hikari.*"])))
  (m/in-cljc-mode)
  (m/start #'db/db)
  (sql/insert db/user (sql/values (vals users)))
  (f)
  (m/stop))

(defn erase-fixture [f]
  (f)
  (erase-db!))
