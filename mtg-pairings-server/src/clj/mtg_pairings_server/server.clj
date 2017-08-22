(ns mtg-pairings-server.server
  (:gen-class)
  (:require [org.httpkit.server :as hs]
            [ring.middleware.resource :refer [wrap-resource]]
            [ring.middleware.content-type :refer [wrap-content-type]]
            [ring.middleware.not-modified :refer [wrap-not-modified]]
            [ring.middleware.jsonp :refer [wrap-json-with-padding]]
            [ring.util.response :refer [resource-response file-response]]
            [cheshire.generate :as json-gen]
            [clojure.tools.logging :as log]
            [clojure.string :as string]
            [mount.core :as m]
            [mtg-pairings-server.handler]
            [mtg-pairings-server.properties :refer [properties]]))

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

(defn wrap-allow-origin
  [handler]
  (fn [request]
    (let [response (handler request)]
      (if (= (:request-method request) :get)
        (assoc-in response [:headers "Access-Control-Allow-Origin"] "*")
        response))))

(defn run! [server-properties]
  (log/info "Starting server on port" (:port server-properties) "...")
  (json-gen/add-encoder org.joda.time.LocalDate
                        (fn [c generator]
                          (.writeString generator (str c))))
  (hs/run-server
    (-> #'mtg-pairings-server.handler/app
        wrap-json-with-padding
        wrap-request-log
        (wrap-resource-304 "public")
        wrap-allow-origin)
    server-properties))

(m/defstate server
  :start (run! (:server properties))
  :stop (server))

(defn -main []
  (m/start))
