(ns mtg-pairings-server.util.local-storage
  (:require [clojure.string :as str]
            [re-frame.core :refer [dispatch]]
            [oops.core :refer [oget]]))

(defn stringify-key [k]
  (cond
    (keyword? k) (name k)
    (string? k) k
    (vector? k) (str/join "." (map stringify-key k))
    :else (str k)))

(defn split-key [k]
  (for [part (str/split k #"\.")]
    (if (re-matches #"\d+" part)
      (js/parseInt part)
      part)))

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

(defn listener [event]
  (dispatch [:mtg-pairings-server.events/local-storage-updated
             (split-key (oget event "key"))
             (-> (oget event "newValue")
                 js/JSON.parse
                 (js->clj :keywordize-keys true))]))
