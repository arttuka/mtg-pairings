(ns mtg-pairings-server.components.downshift
  (:require [reagent.core :as r]
            [downshift]
            [reagent-material-ui.util :refer [wrap-clj-function wrap-js-function js->clj' clj->js']]
            [mtg-pairings-server.util :refer [map-values]]))

(def state-change-types (js->clj' (.-stateChangeTypes downshift)))

(defn component [props render-fn]
  (let [wrapped-props (reduce-kv (fn [m k v]
                                   (if v
                                     (assoc m k (cond-> v
                                                  (fn? v) (wrap-clj-function)))
                                     m))
                                 {}
                                 props)]
    [:> downshift wrapped-props
     (fn [render-props]
       (let [{:strs [getInputProps
                     getItemProps
                     getLabelProps
                     getMenuProps
                     highlightedIndex
                     isOpen]} (js->clj render-props)]
         (render-fn {:get-input-props         (wrap-js-function getInputProps)
                     :get-item-props          (wrap-js-function getItemProps)
                     :get-label-props         (wrap-js-function getLabelProps)
                     :get-menu-props          (wrap-js-function getMenuProps)
                     :highlighted-index       highlightedIndex
                     :open?                   isOpen})))]))
