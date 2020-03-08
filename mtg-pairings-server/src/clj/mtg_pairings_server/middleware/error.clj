(ns mtg-pairings-server.middleware.error
  (:require [compojure.api.exception :as ex]
            [config.core :refer [env]]
            [cheshire.core :as json]
            [schema.utils :as su]
            [taoensso.timbre :as log]
            [mtg-pairings-server.util.sql :as sql-util])
  (:import (clojure.lang ExceptionInfo)))

(defn error-response [^Exception e data]
  {:status  500
   :body    (json/generate-string
             (cond-> {:class (.getName (class e))
                      :type  "unknown-exception"}
               (env :dev) (assoc :data data
                                 :message (.getMessage e))))
   :headers {"Content-Type" "application/json"}})

(defn request-validation-error-handler [e data req]
  (let [errors (ex/stringify-error (su/error-val data))]
    (log/error "Bad Request" (get-in req [:params :key]) errors)
    (ex/request-validation-handler e data req)))

(defn sql-error-handler [e data req]
  (if-not (::sql-util/found? data)
    {:status 404}
    (do
      (log/error e "SQL assertion error")
      (error-response e data))))

(defn default-error-handler [^Exception e data req]
  (log/error e (.getMessage e))
  (error-response e data))

(defn wrap-errors
  [handler]
  (fn [request]
    (try
      (handler request)
      (catch ExceptionInfo e
        (let [{:keys [type sql-util/found?]} (ex-data e)]
          (if (and (= type ::sql-util/assertion)
                   (not found?))
            {:status 404
             :body   "Not found"}
            (default-error-handler e (ex-data e) request))))
      (catch Exception e
        (default-error-handler e nil request)))))
