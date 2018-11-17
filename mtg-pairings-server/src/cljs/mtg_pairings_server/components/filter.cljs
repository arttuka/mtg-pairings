(ns mtg-pairings-server.components.filter
  (:require [reagent.core :as reagent]
            [re-frame.core :refer [subscribe dispatch dispatch]]
            [cljsjs.material-ui]
            [cljs-react-material-ui.core]
            [cljs-react-material-ui.reagent :as ui]
            [cljs-react-material-ui.icons :as icons]
            [cljs-time.coerce :as coerce]
            [prop-types]
            [oops.core :refer [oget]]
            [mtg-pairings-server.components.slider :refer [slider]]
            [mtg-pairings-server.events :as events]
            [mtg-pairings-server.subscriptions :as subs]
            [mtg-pairings-server.util.util :refer [to-local-date]]
            [mtg-pairings-server.material-ui.util :refer [get-theme]]))

(defn organizer-filter []
  (let [organizers (subscribe [::subs/organizers])
        value (subscribe [::subs/tournament-filter :organizer])]
    [:div.filter
     [ui/select-field
      {:on-change            (fn [_ _ new-value]
                               (dispatch [::events/tournament-filter [:organizer new-value]]))
       :value                @value
       :floating-label-text  "Turnausjärjestäjä"
       :floating-label-fixed true}
      [ui/menu-item {:value        ""
                     :primary-text "Kaikki järjestäjät"}]
      (for [organizer @organizers
            :when (not= organizer "")]
        ^{:key organizer}
        [ui/menu-item {:value        organizer
                       :primary-text organizer}])]]))

(defn date-picker [{:keys [hint-text on-change on-clear value]}]
  (let [handler (fn [_ new-value]
                  (on-change (to-local-date new-value)))
        v (when value (js/Date. (coerce/to-long value)))]
    [:div.date-picker
     [ui/date-picker
      {:hint-text        hint-text
       :container        :inline
       :auto-ok          true
       :locale           "fi-FI"
       :DateTimeFormat   (oget js/Intl "DateTimeFormat")
       :style            {:display       :inline-block
                          :margin-bottom "2px"}
       :text-field-style {:width "128px"}
       :on-change        handler
       :value            v
       :cancel-label     "Peruuta"}]
     (when v
       [ui/icon-button
        {:on-click on-clear
         :style    {:position :absolute
                    :right    0}}
        [icons/navigation-cancel
         {:style {:color :grey}}]])]))

(defn date-filter []
  (let [from (subscribe [::subs/tournament-filter :date-from])
        to (subscribe [::subs/tournament-filter :date-to])]
    (fn date-filter-render []
      [:div.filter.date-filter
       [:label.filter-label "Päivämäärä"]
       [date-picker
        {:hint-text "Alkaen"
         :on-change #(dispatch [::events/tournament-filter [:date-from %]])
         :on-clear  #(dispatch [::events/tournament-filter [:date-from nil]])
         :value     @from}]
       " – "
       [date-picker
        {:hint-text "Asti"
         :on-change #(dispatch [::events/tournament-filter [:date-to %]])
         :on-clear  #(dispatch [::events/tournament-filter [:date-to nil]])
         :value     @to}]])))

(defn player-filter []
  (let [players (subscribe [::subs/tournament-filter :players])
        max-players (subscribe [::subs/max-players])]
    (reagent/create-class
      {:context-types  #js {:muiTheme prop-types/object.isRequired}
       :reagent-render (fn player-filter-render []
                         (let [palette (:palette (get-theme (reagent/current-component)))]
                           [:div.filter.player-filter
                            [:label.filter-label "Pelaajamäärä"]
                            [slider {:min       (atom 0)
                                     :max       max-players
                                     :value     players
                                     :step      10
                                     :color     (:accent1Color palette)
                                     :on-change #(dispatch [::events/tournament-filter [:players %]])}]]))})))

(defn clear-filters []
  [ui/raised-button
   {:label      "Poista valinnat"
    :on-click   #(dispatch [::events/reset-tournament-filter])
    :class-name :filter-button}])

(defn tournament-filters []
  [:div.filters
   [organizer-filter]
   [date-filter]
   [player-filter]
   [clear-filters]])