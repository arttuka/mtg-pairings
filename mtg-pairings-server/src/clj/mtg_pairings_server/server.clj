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
            [mtg-pairings-server.util.sql :as sql-util])
  (:import (com.fasterxml.jackson.core JsonGenerator)
           (org.joda.time LocalDate)
           (clojure.lang ExceptionInfo)))

(defn error-response [^Exception e]
  {:status 500
   :body (.getMessage e)})

(defn wrap-errors
  [handler]
  (fn [request]
    (try
      (handler request)
      (catch ExceptionInfo e
        (let [data (ex-data e)]
          (case (:type data)
            ::sql-util/assertion (if-not (::sql-util/found? data)
                                   {:status 404}
                                   (error-response e))
            (error-response e))))
      (catch Exception e
        (log/error e (pr-str request))
        (error-response e)))))

(def ^:private log-blacklist
  [#"^.*\.(ico|png|jpg|js|css|woff2|txt|map)$"
   #"^/chsk$"])

(defn ^:private logged? [uri]
  (not-any? #(re-matches % uri) log-blacklist))

(defn wrap-request-log
  [handler]
  (fn [request]
    (let [response (handler request)
          uri (:uri request)]
      (when (logged? uri)
        (log/info (get-in request [:headers "x-real-ip"] (:remote-addr request))
                  (-> request :request-method name string/upper-case)
                  uri
                  (:status response)))
      response)))

(defn wrap-allow-origin
  [handler]
  (fn [request]
    (let [response (handler request)]
      (if (= (:request-method request) :get)
        (assoc-in response [:headers "Access-Control-Allow-Origin"] "*")
        response))))

(defn run-server! [handler port]
  (log/info "Starting server on port" port "...")
  (json-gen/add-encoder LocalDate
                        (fn [c ^JsonGenerator generator]
                          (.writeString generator (str c))))
  (hs/run-server
    (-> handler
        wrap-json-with-padding
        wrap-request-log
        wrap-allow-origin
        wrap-errors)
    {:port port}))
