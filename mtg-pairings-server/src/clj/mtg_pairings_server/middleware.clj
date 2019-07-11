(ns mtg-pairings-server.middleware
  (:require [ring.middleware.defaults :refer [site-defaults api-defaults wrap-defaults]]
            [config.core :refer [env]]
            [mtg-pairings-server.middleware.cache-control :refer [wrap-cache-control]]
            [mtg-pairings-server.middleware.decklist :refer [wrap-decklist-prefix]]
            [mtg-pairings-server.middleware.error :refer [wrap-errors]]))

(defn add-dev-middleware [handler]
  (require 'prone.middleware 'ring.middleware.reload)
  (let [wrap-exceptions (ns-resolve 'prone.middleware 'wrap-exceptions)
        wrap-reload (ns-resolve 'ring.middleware.reload 'wrap-reload)]
    (-> handler
        wrap-exceptions
        (wrap-reload {:dirs ["src/clj" "src/cljc"]}))))

(defn add-prod-middleware [handler]
  (-> handler
      wrap-decklist-prefix
      (wrap-cache-control {#"\.(css|js|txt)$" "max-age=31536000"})
      wrap-errors))

(defn wrap-site-middleware [handler]
  (cond-> handler
    true (wrap-defaults (update site-defaults :security dissoc :frame-options :content-type-options))
    (env :dev) (add-dev-middleware)
    (not (env :dev)) (add-prod-middleware)))
