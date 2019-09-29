(ns mtg-pairings-server.components.filter
  (:require [reagent.core :as reagent :refer [atom]]
            [re-frame.core :refer [subscribe dispatch dispatch]]
            [reagent-material-ui.components :as ui]
            [reagent-material-ui.icons.cancel :refer [cancel]]
            [reagent-material-ui.icons.expand-more :refer [expand-more]]
            [reagent-material-ui.icons.filter-list :refer [filter-list]]
            [oops.core :refer [oget]]
            [mtg-pairings-server.components.date-picker :as date-picker]
            [mtg-pairings-server.events.pairings :as events]
            [mtg-pairings-server.util.material-ui :refer [wrap-on-change]]
            [mtg-pairings-server.subscriptions.common :as common-subs]
            [mtg-pairings-server.subscriptions.pairings :as subs]
            [mtg-pairings-server.util :refer [to-local-date]]))

(defn organizer-filter []
  (let [organizers (subscribe [::subs/organizers])
        value (subscribe [::subs/tournament-filter :organizer])
        on-change (wrap-on-change #(dispatch [::events/tournament-filter [:organizer %]]))]
    (fn organizer-filter-render []
      [ui/form-control {:class :organizer-filter}
       [ui/input-label {:html-for "organizer-filter"}
        "Turnausjärjestäjä"]
       [ui/select {:value       @value
                   :on-change   on-change
                   :input-props {:name  "organizer-filter"
                                 :id    "organizer-filter"
                                 :style {:width "100%"}}}
        [ui/menu-item {:value :all-organizers}
         "Kaikki järjestäjät"]
        (for [organizer @organizers
              :when (not= organizer "")]
          ^{:key organizer}
          [ui/menu-item {:value organizer}
           organizer])]])))

(defn date-picker [props]
  (let [mobile? (subscribe [::common-subs/mobile?])]
    (fn date-picker-render [{:keys [label on-change on-clear value]}]
      [:div.date-picker
       [date-picker/date-picker {:value       value
                                 :placeholder label
                                 :on-change   on-change
                                 :variant     :inline
                                 :auto-ok     true
                                 :format      "DD.MM.YYYY"
                                 :style       {:width (if @mobile?
                                                        "calc(50vw - 26px)"
                                                        "128px")}}]
       (when value
         [ui/icon-button
          {:on-click on-clear
           :style    {:position :absolute
                      :right    "-8px"
                      :bottom   "-8px"}}
          [cancel
           {:style {:color :grey}}]])])))

(defn date-filter []
  (let [from (subscribe [::subs/tournament-filter :date-from])
        to (subscribe [::subs/tournament-filter :date-to])]
    (fn date-filter-render []
      [:div.filter.date-filter
       [:label.filter-label "Päivämäärä"]
       [date-picker
        {:label     "Alkaen"
         :on-change #(dispatch [::events/tournament-filter [:date-from %]])
         :on-clear  #(dispatch [::events/tournament-filter [:date-from nil]])
         :value     @from}]
       [:span.separator "–"]
       [date-picker
        {:label     "Asti"
         :on-change #(dispatch [::events/tournament-filter [:date-to %]])
         :on-clear  #(dispatch [::events/tournament-filter [:date-to nil]])
         :value     @to}]])))

(defn player-filter []
  (let [players (subscribe [::subs/tournament-filter :players])
        max-players (subscribe [::subs/max-players])]
    (fn player-filter-render []
      [:div.filter.player-filter
       [:label.filter-label "Pelaajamäärä"]
       [ui/slider {:value               @players
                   :min                 0
                   :max                 @max-players
                   :step                10
                   :value-label-display :auto
                   :on-change           (fn [_ new-value]
                                          (dispatch [::events/tournament-filter [:players new-value]]))}]])))

(defn clear-filters []
  (let [filters-active? (subscribe [::subs/filters-active])]
    (fn clear-filters-render []
      [ui/button
       {:on-click   #(dispatch [::events/reset-tournament-filter])
        :disabled   (not @filters-active?)
        :variant    :contained
        :color      :secondary
        :class-name :filter-button}
       "Poista valinnat"])))

(defn desktop-filters []
  [:div.filters.desktop-filters.hidden-mobile
   [organizer-filter]
   [date-filter]
   [player-filter]
   [clear-filters]])

(defn mobile-filters []
  (let [filters-active? (subscribe [::subs/filters-active])
        expanded? (atom false)
        on-expand #(swap! expanded? not)]
    (fn mobile-filters-render []
      [ui/card
       {:class [:filters :mobile-filters :hidden-desktop]}
       [ui/card-header
        {:title  "Hakutyökalut"
         :class  :card-header-expandable
         :style  {:height "56px"}
         :avatar (reagent/as-element
                  [filter-list {:color (if @filters-active?
                                         :secondary
                                         :primary)}])
         :action (reagent/as-element
                  [ui/icon-button {:class    [:card-header-button
                                              (when @expanded? :card-header-button-expanded)]
                                   :on-click on-expand}
                   [expand-more]])}]
       [ui/collapse {:in @expanded?}
        [ui/card-content
         {:style {:padding-top 0}}
         [organizer-filter]
         [date-filter]
         [player-filter]
         [clear-filters]]]])))

(defn filters []
  (let [mobile? (subscribe [::common-subs/mobile?])]
    (fn filters-render []
      (if @mobile?
        [mobile-filters]
        [desktop-filters]))))
