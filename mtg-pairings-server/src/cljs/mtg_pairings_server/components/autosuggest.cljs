(ns mtg-pairings-server.components.autosuggest
  (:require [reagent.core :as reagent :refer [atom]]
            [cljsjs.material-ui]
            [cljs-react-material-ui.reagent :as ui]
            [cljsjs.react-autosuggest]
            [oops.core :refer [oget]]
            [mtg-pairings-server.util :refer [deep-merge]]))

(defn ^:private suggestion [suggestion opts]
  (reagent/as-element
   [ui/menu-item {:primary-text (reagent/as-element
                                 [:div {:style {:white-space   :nowrap
                                                :text-overflow :ellipsis
                                                :overflow      :hidden}}
                                  suggestion])}]))

(defn ^:private suggestion-container [props]
  (reagent/as-element
   [ui/paper (js->clj (oget props "containerProps")) (oget props "children")]))

(defn ^:private input [props]
  (reagent/as-element
   [ui/text-field (js->clj props)]))

(def ^:private default-styles
  {:container                  {:flex-grow 1
                                :position  "relative"
                                :width     256
                                :display   "inline-block"}
   :suggestions-container-open {:position "absolute"
                                :z-index  10
                                :left     0
                                :right    0}
   :suggestion                 {:display "block"}
   :suggestions-list           {:margin          0
                                :padding         0
                                :list-style-type "none"}
   :suggestion-highlighted     {:background-color "rgba(0, 0, 0, 0.1)"}})

(defn autosuggest [{:keys [on-suggestions-fetch-requested on-change] :as options}]
  (let [value (atom "")
        on-suggestions-fetch-requested (fn [event]
                                         (on-suggestions-fetch-requested (oget event "value")))
        select-suggestion (fn [suggestion]
                            (reset! value "")
                            (on-change suggestion))
        on-suggestion-selected (fn [_ data]
                                 (select-suggestion (oget data "suggestion")))]
    (fn autosuggest-render [{:keys [suggestions styles on-suggestions-clear-requested] :as options}]
      (let [input-props (merge (dissoc options :suggestions :styles :on-change :on-suggestions-fetch-requested :on-suggestions-clear-requested)
                               {:on-change    (fn [event new-value]
                                                (when (= "type" (oget new-value "method"))
                                                  (reset! value (oget event "target" "value"))))
                                :value        @value})]
        [:> js/Autosuggest {:suggestions                    @suggestions
                            :on-suggestions-fetch-requested on-suggestions-fetch-requested
                            :on-suggestions-clear-requested on-suggestions-clear-requested
                            :on-suggestion-selected         on-suggestion-selected
                            :get-suggestion-value           identity
                            :render-suggestions-container   suggestion-container
                            :render-suggestion              suggestion
                            :render-input-component         input
                            :input-props                    input-props
                            :theme                          (deep-merge default-styles styles)}]))))
