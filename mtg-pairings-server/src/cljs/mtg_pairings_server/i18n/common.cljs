(ns mtg-pairings-server.i18n.common
  (:require [cljs.core :refer-macros [exists?]]
            [goog.string :as gstring]
            [goog.string.format]
            [mtg-pairings-server.util :as util]
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

(defn make-translate [translations]
  (fn [language key & args]
    (if-let [translation (get-in translations (concat (util/split-key key true) [language]))]
      (apply gstring/format translation args)
      (throw (js/Error. (str "No translation found for language "
                             language
                             " and key "
                             key))))))
