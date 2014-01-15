(ns mtg-pairings-backend.server
  (:gen-class)
  (:require [org.httpkit.server :as hs]
            [compojure.core :as c]
            [ring.middleware.json :refer [wrap-json-body wrap-json-response]]
            [ring.middleware.session :refer [wrap-session]]
            [ring.util.response :refer [response]]
            [clojure.java.io :as io]
            
            [mtg-pairings-backend.tournament-api :as tournament-api]
            [mtg-pairings-backend.in-memory-db :refer [create-db load-from-file]]))

(def default-properties
  {:db-filename "mtg-pairings.db"})

(defn ^:private create-and-load-db
  [filename]
  (let [db (create-db)
        file (io/file filename)]
    (if (.exists file)
      (try
        (load-from-file db filename)
        (catch clojure.lang.ExceptionInfo e
          db))
      db)))

(defn routes [db]
  (let [tournament-routes (tournament-api/routes db)]
    (c/routes
      (c/GET "/" [] "Hello World")
      (c/context "/tournament" [] tournament-routes))))

(defn run! []
  (let [properties default-properties
        db (create-and-load-db (:db-filename properties))
        stop-fn (hs/run-server 
                  (-> (routes db)
                    wrap-session
                    wrap-json-response
                    (wrap-json-body {:keywords? true})) 
                  {:port 8080})]
    {:db db
     :stop-fn stop-fn}))

(defn -main []
  (run!))