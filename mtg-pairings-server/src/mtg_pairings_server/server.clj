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
            [clojure.tools.logging :as log]
            
            [mtg-pairings-server.tournament-api :as tournament-api]
            [mtg-pairings-server.player-api :as player-api]
            [mtg-pairings-server.sql-db :refer [create-korma-db]]))

(defn routes []
  (let [tournament-routes (tournament-api/routes)
        player-routes (player-api/routes)]
    (c/routes
      (r/resources "/")
      (c/GET "/lol" [] (throw (Throwable. "lololol")))
      (c/GET "/" [] (response (slurp "resources/public/index.html")))
      (c/context "/tournament" [] tournament-routes)
      (c/context "/player" [] player-routes))))

(defn wrap-exceptions
  [handler]
  (fn [request]
    (try
      (handler request)
      (catch Throwable t
        (log/error t (.getMessage t))
        {:status 500
         :headers {"Content-Type" "text/plain"}
         :body (str "500 Internal Server Error\nCause: " t)}))))

(defn run! []
  (log/info "Starting server...")
  (let [{db-properties :db, server-properties :server} (edn/read-string (slurp "properties.edn"))
        db (create-korma-db db-properties)
        stop-fn (hs/run-server 
                  (-> (routes)
                    (wrap-resource "public")
                    wrap-session
                    wrap-json-response
                    (wrap-json-body {:keywords? true})
                    wrap-exceptions) 
                  server-properties)]
    (json-gen/add-encoder org.joda.time.LocalDate
      (fn [c generator]
        (.writeString generator (str c))))
    {:db db
     :stop-fn stop-fn}))

(defn -main []
  (run!))