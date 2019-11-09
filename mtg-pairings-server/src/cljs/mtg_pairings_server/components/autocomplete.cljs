(ns mtg-pairings-server.components.autocomplete
  (:require [reagent.core :as reagent :refer [atom]]
            [clojure.string :as str]
            [reagent-material-ui.components :as ui]
            [reagent-material-ui.util :refer [adapt-react-class js->clj' use-ref]]
            [reagent-material-ui.lab.use-autocomplete :refer [use-autocomplete]]
            [reagent-material-ui.lab.create-filter-options :refer [create-filter-options]]))

(defn input [{:keys [input-props] :as props}]
  (let [{:keys [on-blur on-change on-focus ref]} input-props]
    [ui/text-field (merge props {:InputProps  {:on-blur   on-blur
                                               :on-change on-change
                                               :on-focus  on-focus}
                                 :input-props (dissoc input-props :on-blur :on-change :on-focus :ref)
                                 :input-ref   ref})]))

(defn react-autocomplete [params]
  (let [{:keys [classes label on-select get-option-label
                fetch-suggestions clear-suggestions options]} (js->clj' params)
        on-change (fn [_ v]
                    (on-select (js->clj' (first v))))
        value (use-ref #js [])
        {:keys [anchor-el
                get-input-label-props
                get-input-props
                get-listbox-props
                get-option-props
                get-root-props
                grouped-options
                popup-open
                set-anchor-el]} (use-autocomplete {:options          options
                                                   :on-change        on-change
                                                   :on-close         clear-suggestions
                                                   :get-option-label get-option-label
                                                   :multiple         true
                                                   :value            (.-current value)})
        input-props (get-input-props)
        on-input-change (fn [e]
                          (let [v (.. e -target -value)]
                            (if (str/blank? v)
                              (clear-suggestions)
                              (fetch-suggestions v)))
                          ((:on-change input-props) e))]
    (reagent/as-element
     [:div (merge (get-root-props)
                  {:class (:root classes)
                   :ref   set-anchor-el})
      [input {:full-width      true
              :InputLabelProps (get-input-label-props)
              :input-props     (assoc input-props :on-change on-input-change)
              :label           label}]
      [ui/popper {:open      (and popup-open (boolean (seq options)))
                  :anchor-el anchor-el
                  :placement :bottom-start}
       [ui/paper {:style {:width (some-> anchor-el (.-clientWidth))}}
        [ui/menu-list (get-listbox-props)
         (for [[index option] (map-indexed vector grouped-options)]
           ^{:key option}
           (let [option-props (get-option-props {:index  index
                                                 :option option})]
             [ui/menu-item (assoc option-props :class (:menu-item classes))
              [ui/typography {:variant :inherit
                              :no-wrap true}
               (get-option-label option)]]))]]]])))

(def autocomplete (adapt-react-class react-autocomplete))
