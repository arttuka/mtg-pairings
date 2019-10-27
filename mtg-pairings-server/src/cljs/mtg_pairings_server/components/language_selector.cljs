(ns mtg-pairings-server.components.language-selector
  (:require [reagent.core :as reagent :refer [atom]]
            [re-frame.core :refer [subscribe dispatch]]
            [reagent-material-ui.components :as ui]
            [reagent-material-ui.styles :refer [with-styles]]
            [mtg-pairings-server.events.common :as common-events]
            [mtg-pairings-server.subscriptions.common :as common-subs]))

(def styles {:root {:float :right
                    :width 120}})

(defn language-button [{:keys [label on-click selected?]}]
  [ui/button {:on-click on-click
              :color (if selected? :primary :default)
              :variant (if selected? :contained :outlined)}
   label])

(defn language-selector* [props]
  (let [language (subscribe [::common-subs/language])
        select-fi #(dispatch [::common-events/set-language :fi])
        select-en #(dispatch [::common-events/set-language :en])]
    (fn language-selector-render [{:keys [classes]}]
      [ui/button-group {:full-width true
                        :class      [(:root classes) :no-print]}
       (language-button {:on-click  select-fi
                         :selected? (= :fi @language)
                         :label     "FI"})
       (language-button {:on-click  select-en
                         :selected? (= :en @language)
                         :label     "EN"})])))

(def language-selector ((with-styles styles) language-selector*))
