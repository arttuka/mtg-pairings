(ns mtg-pairings-server.components.pairings.filter
  (:require [reagent.core :as reagent :refer [atom]]
            [re-frame.core :refer [subscribe dispatch dispatch]]
            [reagent-material-ui.components :as ui]
            [reagent-material-ui.icons.cancel :refer [cancel]]
            [reagent-material-ui.icons.expand-more :refer [expand-more]]
            [reagent-material-ui.icons.filter-list :refer [filter-list]]
            [reagent-material-ui.pickers :as pickers]
            [reagent-material-ui.styles :refer [styled with-styles]]
            [oops.core :refer [oget]]
            [mtg-pairings-server.components.pairings.expandable :refer [expandable-header]]
            [mtg-pairings-server.events.pairings :as events]
            [mtg-pairings-server.util.material-ui :refer [wrap-on-change]]
            [mtg-pairings-server.subscriptions.common :as common-subs]
            [mtg-pairings-server.subscriptions.pairings :as subs]
            [mtg-pairings-server.util :refer [debounce to-local-date]]
            [mtg-pairings-server.util.material-ui :as mui-util]))

(defn organizer-filter-styles [{:keys [spacing] :as theme}]
  {:container {:height                     "48px"
               (mui-util/on-desktop theme) {:flex         "0 1 256px"
                                            :margin-right (spacing 2)
                                            :display      "inline-block"}
               (mui-util/on-mobile theme)  {:width         "100%"
                                            :margin-bottom (spacing 1)}}})

(defn organizer-filter* [props]
  (let [organizers (subscribe [::subs/organizers])
        value (subscribe [::subs/tournament-filter :organizer])
        on-change (wrap-on-change #(dispatch [::events/tournament-filter [:organizer %]]))]
    (fn organizer-filter-render [{:keys [classes]}]
      [:div {:class (:container classes)}
       [ui/form-control {:full-width true}
        [ui/input-label {:html-for "organizer-filter"}
         "Turnausjärjestäjä"]
        [ui/select {:value       @value
                    :on-change   on-change
                    :input-props {:name "organizer-filter"
                                  :id   "organizer-filter"}}
         [ui/menu-item {:value "all-organizers"}
          "Kaikki järjestäjät"]
         (for [organizer @organizers
               :when (not= "" organizer)]
           ^{:key organizer}
           [ui/menu-item {:value organizer}
            organizer])]]])))

(def organizer-filter ((with-styles organizer-filter-styles) organizer-filter*))

(defn date-filter-styles [{:keys [palette spacing] :as theme}]
  (let [on-desktop (mui-util/on-desktop theme)
        on-mobile (mui-util/on-mobile theme)]
    {:container             {:height      "48px"
                             :position    :relative
                             :display     "inline-flex"
                             :align-items "flex-end"
                             on-desktop   {:flex         "0 1 280px"
                                           :margin-right (spacing 2)}
                             on-mobile    {:width         "100%"
                                           :margin-bottom (spacing 1)}}
     :label                 {:position  :absolute
                             :top       0
                             :left      0
                             :color     (get-in palette [:text :secondary])
                             :font-size "12px"}
     :date-picker-container {:flex     "1"
                             :position :relative}
     :separator             {:flex   "0 0 auto"
                             :margin (spacing 0 1)}
     :date-picker           {:width "100%"}
     :clear-icon            {:position :absolute
                             :right    "-8px"
                             :bottom   "-8px"
                             :color    (get-in palette [:text :secondary])}}))

(defn date-picker [{:keys [label on-change on-clear value classes]}]
  [:div {:class (:date-picker-container classes)}
   [pickers/date-picker {:class       (:date-picker classes)
                         :value       value
                         :placeholder label
                         :on-change   on-change
                         :auto-ok     true
                         :format      "dd.MM.yyyy"
                         :variant     :inline}]
   (when value
     [ui/icon-button
      {:class    (:clear-icon classes)
       :on-click on-clear}
      [cancel]])])

(defn date-filter* [props]
  (let [from (subscribe [::subs/tournament-filter :date-from])
        to (subscribe [::subs/tournament-filter :date-to])]
    (fn date-filter-render [{:keys [classes]}]
      [:div {:class (:container classes)}
       [:label {:class (:label classes)}
        "Päivämäärä"]
       [date-picker
        {:label     "Alkaen"
         :on-change #(dispatch [::events/tournament-filter [:date-from %]])
         :on-clear  #(dispatch [::events/tournament-filter [:date-from nil]])
         :value     @from
         :classes   classes}]
       [:span {:class (:separator classes)} "–"]
       [date-picker
        {:label     "Asti"
         :on-change #(dispatch [::events/tournament-filter [:date-to %]])
         :on-clear  #(dispatch [::events/tournament-filter [:date-to nil]])
         :value     @to
         :classes   classes}]])))

(def date-filter ((with-styles date-filter-styles) date-filter*))

(defn player-filter-styles [{:keys [palette spacing] :as theme}]
  (let [on-desktop (mui-util/on-desktop theme)
        on-mobile (mui-util/on-mobile theme)]
    {:container {:height    "48px"
                 :padding   (spacing 3 1 0)
                 :position  :relative
                 on-desktop {:flex         "0 1 220px"
                             :margin-right (spacing 2)
                             :display      "inline-block"}
                 on-mobile  {:width         "100%"
                             :margin-bottom (spacing 1)}}
     :label     {:position  :absolute
                 :top       0
                 :left      0
                 :color     (get-in palette [:text :secondary])
                 :font-size "12px"}}))

(defn player-filter* [props]
  (let [players (subscribe [::subs/tournament-filter :players])
        max-players (subscribe [::subs/max-players])
        dispatch-event (debounce (fn [new-value]
                                   (dispatch [::events/tournament-filter [:players new-value]]))
                                 200)
        value (atom @players)
        on-change (fn [_ new-value]
                    (reset! value new-value)
                    (dispatch-event new-value))]
    (add-watch players :player-filter-watch
               (fn [_ _ _ new]
                 (reset! value new)))
    (fn player-filter-render [{:keys [classes]}]
      [:div {:class (:container classes)}
       [:label {:class (:label classes)}
        "Pelaajamäärä"]
       [ui/slider {:value               @value
                   :min                 0
                   :max                 @max-players
                   :step                10
                   :value-label-display :auto
                   :on-change           on-change}]])))

(def player-filter ((with-styles player-filter-styles) player-filter*))



(defn clear-filters []
  (let [filters-active? (subscribe [::subs/filters-active])]
    (fn clear-filters-render []
      [ui/button
       {:on-click #(dispatch [::events/reset-tournament-filter])
        :disabled (not @filters-active?)
        :variant  :contained
        :color    :secondary}
       "Poista valinnat"])))

(def desktop-filter-container (styled :div
                                      (fn [{{spacing :spacing} :theme}]
                                        {:display     :flex
                                         :align-items "flex-end"
                                         :padding     (spacing 2)})))

(defn desktop-filters []
  [desktop-filter-container
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
       [expandable-header
        {:title     "Hakutyökalut"
         :expanded? @expanded?
         :on-expand on-expand
         :avatar    (reagent/as-element
                     [filter-list {:color (if @filters-active?
                                            :secondary
                                            :primary)}])}]
       [ui/collapse {:in @expanded?}
        [ui/card-content
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
