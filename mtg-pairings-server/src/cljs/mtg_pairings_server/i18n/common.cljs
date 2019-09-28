(ns mtg-pairings-server.i18n.common
  (:require [cljs.core :refer-macros [exists?]]
            [mtg-pairings-server.util.local-storage :as local-storage]))

(defn ^:private parse-language [language-str]
  (when-let [[_ language] (re-matches #"([a-z][a-z])(-[A-Z][A-Z])?" language-str)]
    (keyword language)))

(defn ^:private browser-language []
  (when (and (exists? js/navigator)
             (.hasOwnProperty js/navigator "languages"))
    (some (comp #{:fi :en} parse-language) (.-languages js/navigator))))

(defn ^:private stored-language []
  (some-> (local-storage/fetch :stored-language)
          keyword))

(defn language
  ([]
   (language :fi))
  ([default]
   (or (stored-language)
       (browser-language)
       default)))
