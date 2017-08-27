(ns mtg-pairings-server.util.local-storage)

(defn store [key obj]
  (.setItem js/localStorage (name key) (js/JSON.stringify (clj->js obj))))

(defn fetch
  ([key]
   (fetch key nil))
  ([key default]
   (if-let [item (.getItem js/localStorage (name key))]
     (-> item
         (or (js-obj))
         (js/JSON.parse)
         (js->clj)
         (clojure.walk/keywordize-keys))
     default)))
