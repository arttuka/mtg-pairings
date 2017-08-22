(ns mtg-pairings-server.middleware
  (:require [ring.middleware.defaults :refer [secure-site-defaults wrap-defaults]]))

(defn wrap-middleware [handler]
  (wrap-defaults handler secure-site-defaults))
