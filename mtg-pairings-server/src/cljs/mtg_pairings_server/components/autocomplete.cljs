(ns mtg-pairings-server.components.autocomplete
  (:require [reagent.core :as reagent :refer [atom]]
            [reagent-material-ui.components :as ui]
            [clojure.string :as str]
            [mtg-pairings-server.components.downshift :as downshift]))

(defn input [{:keys [input-props] :as props}]
  (let [{:keys [on-blur on-change on-key-down]} input-props]
    [ui/text-field (merge props {:InputProps  {:on-blur     on-blur
                                               :on-change   on-change
                                               :on-key-down on-key-down}
                                 :input-props (dissoc input-props :on-blur :on-change :on-key-down)})]))

(defn autocomplete [{:keys [clear-suggestions fetch-suggestions on-select]}]
  (let [{:keys [key-down-enter click-item]} downshift/state-change-types
        on-input-value-change (fn [value {:keys [type]}]
                                (when-not (contains? #{click-item key-down-enter} type)
                                  (if (str/blank? value)
                                    (clear-suggestions)
                                    (fetch-suggestions value))))
        state-reducer (fn [state changes]
                        (condp contains? (:type changes)
                          #{click-item key-down-enter} (assoc changes
                                                              :input-value ""
                                                              :selected-item nil)
                          changes))
        ^js/React.Ref popper-anchor (.createRef js/React)]
    (fn [{:keys [classes label suggestion->string suggestions]}]
      [downshift/component {:item-to-string        suggestion->string
                            :on-input-value-change on-input-value-change
                            :on-select             on-select
                            :item-count            (count @suggestions)
                            :selected-item         nil
                            :state-reducer         state-reducer}
       (fn downshift-render [downshift-props]
         (let [{:keys [get-input-props
                       get-label-props
                       get-menu-props
                       get-item-props
                       highlighted-index
                       open?]} downshift-props
               anchor-el (.-current popper-anchor)
               menu-open? (boolean (and open? (seq @suggestions)))]
           (reagent/as-element
            [:div {:class (:container classes)}
             [input {:full-width      true
                     :input-props     (get-input-props)
                     :InputLabelProps (get-label-props)
                     :input-ref       popper-anchor
                     :label           label}]
             [ui/popper {:open           menu-open?
                         :anchor-el      anchor-el
                         :placement      :bottom-start
                         :disable-portal true
                         :class          (:menu-container classes)}
              [ui/paper {:style {:width (some-> anchor-el (.-clientWidth))}}
               [ui/menu-list (if menu-open? (get-menu-props {} {:suppress-ref-error true}) {})
                (for [[index item] (map-indexed vector @suggestions)
                      :let [name (suggestion->string item)]]
                  ^{:key name}
                  [ui/menu-item (get-item-props {:index    index
                                                 :item     item
                                                 :selected (= highlighted-index index)})
                   [ui/typography {:variant :inherit
                                   :no-wrap true}
                    name]])]]]])))])))
