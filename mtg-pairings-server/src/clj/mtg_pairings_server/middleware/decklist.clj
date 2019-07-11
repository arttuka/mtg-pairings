(ns mtg-pairings-server.middleware.decklist
  (:require [config.core :refer [env]]))

(def rewrite-blacklist [#"^.*\.(ico|png|jpg|js|css|woff2|txt|map)$"
                        #"^/chsk$"
                        #"^robots.txt$"
                        #"^/login$"])

(defn rewrite? [uri]
  (not-any? #(re-matches % uri) rewrite-blacklist))

(defn rewrite [uri]
  (let [prefix (env :decklist-prefix)]
    (cond
      (nil? prefix) uri
      (= "/" uri) prefix
      :else (str prefix uri))))

(defn wrap-decklist-prefix [handler]
  (fn [{:keys [uri headers] :as request}]
    (if (and (= (env :decklist-host) (get headers "host"))
             (rewrite? uri))
      (handler (update request :uri rewrite))
      (handler request))))
