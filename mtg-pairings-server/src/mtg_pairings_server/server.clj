(ns mtg-pairings-server.server
  (:gen-class)
  (:require [org.httpkit.server :as hs]
            [ring.middleware.json :refer [wrap-json-body wrap-json-response]]
            [ring.middleware.session :refer [wrap-session]]
            [ring.middleware.resource :refer [wrap-resource]]
            [cheshire.generate :as json-gen]
            [clojure.java.io :as io]
            [clojure.tools.reader.edn :as edn]
            
            mtg-pairings-server.api
            [mtg-pairings-server.sql-db :refer [create-korma-db]]))

(defn run! []
  (let [db-properties (edn/read-string (slurp "db.properties"))
        db (create-korma-db db-properties)
        stop-fn (hs/run-server 
                  (-> #'mtg-pairings-server.api/app
                    (wrap-resource "public")
                    wrap-session
                    wrap-json-response
                    (wrap-json-body {:keywords? true})) 
                  {:port 8080})]
    (json-gen/add-encoder org.joda.time.LocalDate
      (fn [c generator]
        (.writeString generator (str c))))
    {:db db
     :stop-fn stop-fn}))

(defn -main []
  (run!))