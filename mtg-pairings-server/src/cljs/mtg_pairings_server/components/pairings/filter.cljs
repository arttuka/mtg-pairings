(ns mtg-pairings-server.components.pairings.filter
  (:require [reagent.core :as reagent :refer [atom with-let]]
            [re-frame.core :refer [subscribe dispatch dispatch]]
            [reagent-material-ui.components :as ui]
            [reagent-material-ui.icons.cancel :refer [cancel]]
            [reagent-material-ui.icons.expand-more :refer [expand-more]]
            [reagent-material-ui.icons.filter-list :refer [filter-list]]
            [reagent-material-ui.pickers :as pickers]
            [reagent-material-ui.styles :refer [with-styles]]
            [mtg-pairings-server.components.pairings.expandable :refer [expandable-header]]
            [mtg-pairings-server.events.pairings :as events]
            [mtg-pairings-server.util.material-ui :refer [wrap-on-change]]
            [mtg-pairings-server.subscriptions.common :as common-subs]
            [mtg-pairings-server.subscriptions.pairings :as subs]
            [mtg-pairings-server.util :refer [debounce to-local-date]]
            [mtg-pairings-server.util.styles :refer [on-desktop on-mobile]]))

(defn organizer-filter-styles [{:keys [spacing]}]
  {:container {on-desktop {:flex         "0 1 256px"
                           :margin-right (spacing 2)}
               on-mobile  {:width         "100%"
                           :margin-bottom (spacing 1)}}})

(defn organizer-filter* [props]
  (let [organizers (subscribe [::subs/organizers])
        value (subscribe [::subs/tournament-filter :organizer])
        on-change (wrap-on-change #(dispatch [::events/tournament-filter [:organizer %]]))]
    (fn [{:keys [classes]}]
      [ui/form-control {:class      (:container classes)
                        :full-width true}
       [ui/input-label {:html-for :organizer-filter}
        "Turnausjärjestäjä"]
       [ui/select {:value       @value
                   :on-change   on-change
                   :input-props {:id :organizer-filter}}
        [ui/menu-item {:value "all-organizers"}
         "Kaikki järjestäjät"]
        (for [organizer @organizers
              :when (not= "" organizer)]
          ^{:key organizer}
          [ui/menu-item {:value organizer}
           organizer])]])))

(def organizer-filter ((with-styles organizer-filter-styles) organizer-filter*))

(defn date-filter-styles [{:keys [spacing]}]
  {:container   {:height         48
                 :align-items    :flex-end
                 :flex-direction :row
                 on-desktop      {:flex         "0 1 280px"
                                  :margin-right (spacing 2)}
                 on-mobile       {:width         "100%"
                                  :margin-bottom (spacing 1)}}
   :separator   {:flex   "0 0 auto"
                 :margin (spacing 0 1)}
   :date-picker {:flex 1}})

(defn date-picker [{:keys [label on-change on-clear value classes]}]
  (let [on-click (fn [^js/Event e]
                   (.stopPropagation e)
                   (on-clear))
        clear-button (reagent/as-element
                      [ui/icon-button {:on-click on-click
                                       :size     :small}
                       [cancel]])]
    [pickers/date-picker {:class       (:date-picker classes)
                          :value       value
                          :placeholder label
                          :on-change   on-change
                          :auto-ok     true
                          :format      "dd.MM.yyyy"
                          :variant     :inline
                          :InputProps  {:end-adornment (when value
                                                         clear-button)}}]))

(defn date-filter* [props]
  (let [from (subscribe [::subs/tournament-filter :date-from])
        to (subscribe [::subs/tournament-filter :date-to])]
    (fn [{:keys [classes]}]
      [ui/form-control {:class (:container classes)}
       [ui/input-label {:shrink true}
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

(defn player-filter-styles [{:keys [spacing]}]
  {:container {on-desktop {:flex         "0 1 220px"
                           :margin-right (spacing 2)}
               on-mobile  {:width         "100%"
                           :margin-bottom (spacing 1)}}
   :slider    {:margin-top (spacing 3)
               :padding    [[11 0]]}})

(defn player-filter* [{:keys [classes]}]
  (with-let [players (subscribe [::subs/tournament-filter :players])
             max-players (subscribe [::subs/max-players])
             dispatch-event (debounce (fn [new-value]
                                        (dispatch [::events/tournament-filter [:players new-value]]))
                                      200)
             value (atom @players)
             on-change (fn [_ new-value]
                         (let [v (js->clj new-value)]
                           (reset! value v)
                           (dispatch-event v)))
             _ (add-watch players :player-filter-watch
                          (fn [_ _ _ new]
                            (reset! value new)))]
    [ui/form-control {:class      (:container classes)
                      :full-width true}
     [ui/input-label {:html-for :player-filter
                      :shrink   true}
      "Pelaajamäärä"]
     [ui/slider {:class               (:slider classes)
                 :value               @value
                 :min                 0
                 :max                 @max-players
                 :step                10
                 :value-label-display (if (= @value [0 @max-players])
                                        :auto
                                        :on)
                 :on-change           on-change
                 :id                  :player-filter}]]
    (finally
      (remove-watch players :player-filter-watch))))

(def player-filter ((with-styles player-filter-styles) player-filter*))

(defn clear-filters []
  (let [filters-active? (subscribe [::subs/filters-active])
        on-click #(dispatch [::events/reset-tournament-filter])]
    (fn []
      [ui/button
       {:on-click on-click
        :disabled (not @filters-active?)
        :variant  :contained
        :color    :secondary}
       "Poista valinnat"])))

(def desktop-filter-container ((with-styles (fn [{:keys [spacing]}]
                                              {:root {:align-items :flex-end
                                                      :padding     (spacing 2)}}))
                               ui/toolbar))

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
    (fn []
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
    (fn []
      (if @mobile?
        [mobile-filters]
        [desktop-filters]))))
