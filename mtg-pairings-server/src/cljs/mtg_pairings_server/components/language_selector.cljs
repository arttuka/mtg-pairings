(ns mtg-pairings-server.components.language-selector
  (:require [reagent.core :as reagent :refer [atom]]
            [re-frame.core :refer [subscribe dispatch]]
            [reagent-material-ui.styles :refer [with-styles]]
            [mtg-pairings-server.components.button-toggle :refer [button-toggle]]
            [mtg-pairings-server.events.common :as common-events]
            [mtg-pairings-server.subscriptions.common :as common-subs]
            [mtg-pairings-server.util.styles :refer [on-print]]))

(def styles {:root {:width   120
                    on-print {:display :none}}})

(defn language-selector* [{:keys [on-click]}]
  (let [language (subscribe [::common-subs/language])
        select-fi (fn []
                    (dispatch [::common-events/set-language :fi])
                    (when on-click (on-click)))
        select-en (fn []
                    (dispatch [::common-events/set-language :en])
                    (when on-click (on-click)))]
    (fn [{:keys [class-name classes invert]}]
      [button-toggle {:class   [class-name (:root classes)]
                      :invert  invert
                      :value   @language
                      :options [{:on-click select-fi
                                 :value    :fi
                                 :label    "FI"}
                                {:on-click select-en
                                 :value    :en
                                 :label    "EN"}]}])))

(def language-selector ((with-styles styles) language-selector*))
