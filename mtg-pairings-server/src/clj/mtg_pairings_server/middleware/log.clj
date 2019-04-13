(ns mtg-pairings-server.middleware.log
  (:require [clojure.string :as str]
            [taoensso.timbre :as log]))

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
        (log/info (get-in request [:headers "x-forwarded-for"] (:remote-addr request))
                  (get-in request [:headers "host"])
                  (-> request :request-method name str/upper-case)
                  uri
                  (:status response)))
      response)))
