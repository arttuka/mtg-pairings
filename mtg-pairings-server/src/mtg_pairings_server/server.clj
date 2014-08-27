(ns mtg-pairings-server.server
  (:gen-class)
  (:require [org.httpkit.server :as hs]
            [ring.middleware.json :refer [wrap-json-body wrap-json-response]]
            [ring.middleware.resource :refer [wrap-resource]]
            [ring.middleware.content-type :refer [wrap-content-type]]
            [ring.middleware.params :refer [wrap-params]]
            [ring.middleware.not-modified :refer [wrap-not-modified]]
            [ring.util.response :refer [resource-response file-response]]
            [cheshire.generate :as json-gen]
            [clojure.java.io :as io]
            [clojure.tools.reader.edn :as edn]
            [clojure.tools.logging :as log]
            [clojure.string :as string]
            [mtg-pairings-server.api :as api]
            [mtg-pairings-server.sql-db :refer [create-korma-db]]
            [mtg-pairings-server.properties :refer [properties]]))

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

(defn wrap-request-log
  [handler]
  (fn [request]
    (let [response (handler request)]
      (log/info (:remote-addr request)
                (-> request :request-method name string/upper-case)
                (:uri request)
                (:status response))
      response)))

(defn wrap-remove-content-length
  [handler]
  (fn [request]
    (let [response (handler request)]
      (update-in response [:headers] dissoc "Content-Length" "content-length"))))

(defn wrap-resource-304
  [handler root]
  (-> handler
    (wrap-resource root)
    wrap-content-type
    wrap-not-modified
    wrap-remove-content-length))

(defn run! []
  (let [{db-properties :db, server-properties :server :as props} (edn/read-string (slurp "properties.edn"))
        _ (deliver properties props)
        _ (log/info "Starting server on port" (:port server-properties) "...")
        db (create-korma-db db-properties)
        stop-fn (hs/run-server 
                  (-> #'mtg-pairings-server.api/app
                    wrap-request-log
                    (wrap-resource-304 "public")
                    wrap-params
                    wrap-json-response
                    wrap-exceptions)
                  server-properties)]
    (json-gen/add-encoder org.joda.time.LocalDate
      (fn [c generator]
        (.writeString generator (str c))))
    {:db db
     :stop-fn stop-fn}))

(defn -main []
  (run!))