(ns mtg-pairings-server.test-util
  (:require [config.core :refer [env]]
            [mount.core :as m]
            [taoensso.timbre :as timbre]
            [mtg-pairings-server.db :as db]))

(def tables [:pod_seat :pod :pod_round :seating :pairing :round
             :team_players :player :team :standings :tournament :trader_user])

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
  (db/with-transaction
    (doseq [table tables]
      (db/delete! table ["true"]))))

(defn db-fixture [f]
  (check-db!)
  (timbre/swap-config! (fn [config] (assoc config :ns-blacklist ["org.eclipse.jetty.*" "io.netty.*" "com.zaxxer.hikari.*"])))
  (m/in-cljc-mode)
  (m/start #'db/db)
  (db/with-transaction
    (erase-db!)
    (db/insert-multi! :trader_user
                      [:id :username :uuid]
                      (map (juxt :id :username :uuid) (vals users))))
  (f)
  (m/stop))

(defn erase-fixture [f]
  (try
    (f)
    (finally
      (erase-db!))))
