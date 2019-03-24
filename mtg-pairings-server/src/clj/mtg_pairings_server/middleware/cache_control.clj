(ns mtg-pairings-server.middleware.cache-control)

(defn wrap-cache-control [handler options]
  (fn [request]
    (let [{uri :uri} request
          {:keys [status] :as response} (handler request)
          directive (some (fn [[re d]]
                            (when (re-find re uri)
                              d))
                          options)]
      (if (and (#{200 201 204} status) directive)
        (assoc-in response [:headers "Cache-Control"] directive)
        response))))
