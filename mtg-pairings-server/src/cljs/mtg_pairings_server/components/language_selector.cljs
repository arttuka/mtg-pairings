(ns mtg-pairings-server.components.language-selector
  (:require [reagent.core :as reagent :refer [atom]]
            [re-frame.core :refer [subscribe dispatch]]
            [cljsjs.material-ui]
            [cljs-react-material-ui.reagent :as ui]
            [mtg-pairings-server.events.common :as common-events]
            [mtg-pairings-server.subscriptions.common :as common-subs]))

(defn language-selector []
  (let [language (subscribe [::common-subs/language])
        button-style {:width     "60px"
                      :min-width "60px"}
        select-fi #(dispatch [::common-events/set-language :fi])
        select-en #(dispatch [::common-events/set-language :en])]
    (fn language-selector-render []
      [:div.language-selector.no-print
       [ui/raised-button
        {:label        "FI"
         :on-click     select-fi
         :primary      (= :fi @language)
         :style        button-style
         :button-style {:border-radius "2px 0 0 2px"}}]
       [ui/raised-button
        {:label        "EN"
         :on-click     select-en
         :primary      (= :en @language)
         :style        button-style
         :button-style {:border-radius "0 2px 2px 0"}}]])))
