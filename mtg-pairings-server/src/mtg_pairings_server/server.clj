(ns mtg-pairings-server.server
  (:gen-class)
  (:require [org.httpkit.server :as hs]
            [compojure.core :as c]
            [compojure.route :as r]
            [ring.middleware.json :refer [wrap-json-body wrap-json-response]]
            [ring.middleware.session :refer [wrap-session]]
            [ring.middleware.resource :refer [wrap-resource]]
            [ring.util.response :refer [response]]
            [cheshire.generate :as json-gen]
            [clojure.java.io :as io]
            [clojure.tools.reader.edn :as edn]
            
            [mtg-pairings-server.tournament-api :as tournament-api]
            [mtg-pairings-server.sql-db :refer [create-korma-db]]))

(defn routes []
  (let [tournament-routes (tournament-api/routes)]
    (c/routes
      (r/resources "/")
      (c/GET "/" [] (response (slurp "resources/public/index.html")))
      (c/context "/tournament" [] tournament-routes))))

(defn run! []
  (let [db-properties (edn/read-string (slurp "db.properties"))
        db (create-korma-db db-properties)
        stop-fn (hs/run-server 
                  (-> (routes)
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