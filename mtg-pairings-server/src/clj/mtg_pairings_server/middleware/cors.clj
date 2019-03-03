(ns mtg-pairings-server.middleware.cors)

(defn wrap-allow-origin
  [handler]
  (fn [request]
    (let [response (handler request)]
      (if (= (:request-method request) :get)
        (assoc-in response [:headers "Access-Control-Allow-Origin"] "*")
        response))))
