(ns mtg-pairings-server.server
  (:require [cheshire.generate :as json-gen]
            [taoensso.timbre :as log]
            [clojure.string :as string]
            [mount.core :as m]
            [org.httpkit.server :as hs]
            [ring.middleware.resource :refer [wrap-resource]]
            [ring.middleware.content-type :refer [wrap-content-type]]
            [ring.middleware.not-modified :refer [wrap-not-modified]]
            [ring.middleware.jsonp :refer [wrap-json-with-padding]]
            [ring.util.response :refer [resource-response file-response]]
            [mtg-pairings-server.handler]
            [mtg-pairings-server.properties :refer [properties]])
  (:import (com.fasterxml.jackson.core JsonGenerator)
           (org.joda.time LocalDate)))

(defn wrap-errors
  [handler]
  (fn [request]
    (try
      (handler request)
      (catch Exception e
        (log/error e (pr-str request))
        {:status 500
         :body   (.getMessage e)}))))

(defn wrap-request-log
  [handler]
  (fn [request]
    (let [response (handler request)]
      (log/info (:remote-addr request)
                (-> request :request-method name string/upper-case)
                (:uri request)
                (:status response))
      response)))

(defn wrap-allow-origin
  [handler]
  (fn [request]
    (let [response (handler request)]
      (if (= (:request-method request) :get)
        (assoc-in response [:headers "Access-Control-Allow-Origin"] "*")
        response))))

(defn run-server! [handler server-properties]
  (log/info "Starting server on port" (:port server-properties) "...")
  (json-gen/add-encoder LocalDate
                        (fn [c ^JsonGenerator generator]
                          (.writeString generator (str c))))
  (hs/run-server
    (-> handler
      wrap-json-with-padding
      wrap-request-log
      wrap-allow-origin
      wrap-errors)
    server-properties))
