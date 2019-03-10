(ns mtg-pairings-server.middleware
  (:require [ring.middleware.defaults :refer [site-defaults api-defaults wrap-defaults]]
            [config.core :refer [env]]
            [mtg-pairings-server.middleware.error :refer [wrap-errors]]
            [mtg-pairings-server.middleware.etag :refer [wrap-etag]]))

(defn add-dev-middleware [handler]
  (require 'prone.middleware 'ring.middleware.reload)
  (let [wrap-exceptions (ns-resolve 'prone.middleware 'wrap-exceptions)
        wrap-reload (ns-resolve 'ring.middleware.reload 'wrap-reload)]
    (-> handler
        wrap-exceptions
        (wrap-reload {:dirs ["src/clj" "src/cljc"]}))))

(defn add-prod-middleware [handler]
  (-> handler
      (wrap-etag {:paths [#".*\.(css|js|eot|svg|ttf|woff|woff2)$"]})
      wrap-errors))

(defn wrap-site-middleware [handler]
  (cond-> (wrap-defaults handler site-defaults)
    (env :dev) add-dev-middleware
    (not (env :dev)) add-prod-middleware))
