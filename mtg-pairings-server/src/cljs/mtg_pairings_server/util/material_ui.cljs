(ns mtg-pairings-server.util.material-ui
  (:require [reagent.core :as reagent :refer [atom]]
            [reagent-material-ui.core.text-field :refer [text-field] :rename {text-field mui-text-field}]))

(defn wrap-on-change [f]
  (fn [^js/Event event]
    (f (.. event -target -value))))

(defn wrap-on-checked [f]
  (fn [^js/Event event]
    (f (.. event -target -checked))))

(defn text-field [props]
  (let [original-on-change (:on-change props)
        error-text (:error-text props)
        on-change (wrap-on-change original-on-change)]
    [mui-text-field (-> props
                        (assoc :on-change on-change
                               :error (some? error-text)
                               :helper-text error-text)
                        (dissoc :error-text))]))
