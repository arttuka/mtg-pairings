(ns mtg-pairings-server.middleware.error
  (:require [compojure.api.exception :as ex]
            [config.core :refer [env]]
            [cheshire.core :as json]
            [schema.utils :as su]
            [taoensso.timbre :as log]
            [mtg-pairings-server.util.sql :as sql-util]))

(defn error-response [^Exception e]
  {:status 500
   :body   (cond-> {:message (.getMessage e)
                    :type    (.getName (class e))}
             (env :dev) (assoc :data (ex-data e))
             true (json/generate-string))
   :headers {:content-type "application/json"}})

(defn request-validation-error-handler [e data req]
  (let [errors (ex/stringify-error (su/error-val data))]
    (log/error "Bad Request" (get-in req [:params :key]) errors)
    (ex/request-validation-handler e data req)))

(defn sql-error-handler [e data req]
  (if-not (::sql-util/found? data)
    {:status 404}
    (do
      (log/error e "SQL assertion error")
      (error-response e))))

(defn wrap-errors
  [handler]
  (fn [request]
    (try
      (handler request)
      (catch Exception e
        (log/error e "Unhandled exception")
        (error-response e)))))
