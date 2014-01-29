(ns mtg-pairings-server.server
  (:gen-class)
  (:require [org.httpkit.server :as hs]
            [compojure.core :as c]
            [compojure.route :as r]
            [ring.middleware.json :refer [wrap-json-body wrap-json-response]]
            [ring.middleware.session :refer [wrap-session]]
            [ring.util.response :refer [response]]
            [clojure.java.io :as io]
            [clojure.tools.reader.edn :as edn]
            
            [mtg-pairings-server.tournament-api :as tournament-api]
            [mtg-pairings-server.sql-db :refer [create-db]]))

(def default-properties
  {:db-filename "mtg-pairings.db"})

#_(defn ^:private create-and-load-db
   [filename]
   (let [db (create-db)
         file (io/file filename)]
     (if (.exists file)
       (try
         (load-from-file db filename)
         (catch clojure.lang.ExceptionInfo e
           db))
       db)))

(defn login [req]
  (if-let [dci (get-in req [:body :dci])]
    (do 
      (println "dci:" dci)
      {:status 200
       :session (assoc (:session req {}) :dci dci)})
    {:status 400}))

(defn logout []
  {:status 200
   :session nil})

(defn routes [db]
  (let [tournament-routes (tournament-api/routes db)]
    (c/routes
      (r/resources "/")
      (c/GET "/" [] "Hello World")
      (c/POST "/login" [:as req]
        (login req))
      (c/POST "/logout" []
        (logout))
      (c/context "/tournament" [] tournament-routes))))

(defn run! []
  (let [db-properties (edn/read-string (slurp "db.properties"))
        db (create-db db-properties)
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