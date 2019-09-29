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
       [ui/button-group
        [ui/button
         {:on-click select-fi
          :color    (if (= :fi @language)
                      :primary
                      :default)
          :variant  (if (= :fi @language)
                      :contained
                      :outlined)
          :style    button-style}
         "FI"]
        [ui/button
         {:on-click select-en
          :color    (if (= :en @language)
                      :primary
                      :default)
          :variant  (if (= :en @language)
                      :contained
                      :outlined)
          :style    button-style}
         "EN"]]])))
