(ns mtg-pairings-server.components.language-selector
  (:require [reagent.core :as reagent :refer [atom]]
            [re-frame.core :refer [subscribe dispatch]]
            [reagent-material-ui.components :as ui]
            [reagent-material-ui.styles :refer [with-styles]]
            [mtg-pairings-server.components.button-toggle :refer [button-toggle]]
            [mtg-pairings-server.events.common :as common-events]
            [mtg-pairings-server.subscriptions.common :as common-subs]
            [mtg-pairings-server.util.material-ui :refer [on-print]]))

(def styles {:root {:float   :right
                    :width   120
                    on-print {:display :none}}})

(defn language-selector* [props]
  (let [language (subscribe [::common-subs/language])
        select-fi #(dispatch [::common-events/set-language :fi])
        select-en #(dispatch [::common-events/set-language :en])]
    (fn [{:keys [classes]}]
      [button-toggle {:class   (:root classes)
                      :value   @language
                      :options [{:on-click select-fi
                                 :value    :fi
                                 :label    "FI"}
                                {:on-click select-en
                                 :value    :en
                                 :label    "EN"}]}])))

(def language-selector ((with-styles styles) language-selector*))
