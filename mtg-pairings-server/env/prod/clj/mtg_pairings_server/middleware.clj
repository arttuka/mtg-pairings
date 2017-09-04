(ns mtg-pairings-server.middleware
  (:require [ring.middleware.defaults :refer [site-defaults api-defaults wrap-defaults]]))

(defn wrap-api-middleware [handler]
  (wrap-defaults handler api-defaults))

(defn wrap-site-middleware [handler]
  (wrap-defaults handler site-defaults))
