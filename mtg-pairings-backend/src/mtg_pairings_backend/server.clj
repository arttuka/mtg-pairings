(ns mtg-pairings-backend.server
  (:gen-class)
  (:require [org.httpkit.server :as hs]
            [compojure.core :as c]
            [ring.middleware.json :refer [wrap-json-body wrap-json-response]]
            [ring.util.response :refer [response]]
            [mtg-pairings-backend.tournament-api :as tournament-api]
            
            [mtg-pairings-backend.in-memory-db :refer [create-db]]))

(defn routes [db]
  (let [tournament-routes (tournament-api/routes db)]
    (c/routes
      (c/GET "/" [] "Hello World")
      (c/context "/tournament" [] tournament-routes))))

(defn run! []
  (let [db (create-db)
        stop-fn (hs/run-server 
                  (-> (routes db)
                    wrap-json-response
                    (wrap-json-body {:keywords? true})) 
                  {:port 8080})]
    {:db db
     :stop-fn stop-fn}))

(defn -main []
  (run!))