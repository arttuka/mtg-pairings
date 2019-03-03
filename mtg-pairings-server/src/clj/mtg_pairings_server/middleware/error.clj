(ns mtg-pairings-server.middleware.error
  (:require [config.core :refer [env]]
            [taoensso.timbre :as log]
            [mtg-pairings-server.util.sql :as sql-util])
  (:import (clojure.lang ExceptionInfo)))

(defn error-response [^Exception e]
  {:status 500
   :body   (if (env :dev)
             {:message (.getMessage e)
              :data    (ex-data e)}
             (.getMessage e))})

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
