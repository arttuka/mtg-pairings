(ns mtg-pairings-server.util.local-storage
  (:refer-clojure :exclude [remove])
  (:require [re-frame.core :refer [dispatch]]
            [oops.core :refer [oget]]
            [mtg-pairings-server.util :refer [stringify-key split-key]]))

(defn store [key obj]
  (.setItem js/localStorage (stringify-key key) (js/JSON.stringify (clj->js obj))))

(defn fetch
  ([key]
   (fetch key nil))
  ([key default]
   (if-let [item (.getItem js/localStorage (stringify-key key))]
     (-> item
         (or (js-obj))
         (js/JSON.parse)
         (js->clj :keywordize-keys true?))
     default)))

(defn remove [key]
  (.removeItem js/localStorage (stringify-key key)))

(defn listener [event]
  (dispatch [:mtg-pairings-server.events.pairings/local-storage-updated
             (split-key (oget event "key"))
             (-> (oget event "newValue")
                 js/JSON.parse
                 (js->clj :keywordize-keys true))]))
