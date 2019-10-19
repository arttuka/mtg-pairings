(ns mtg-pairings-server.components.organizer.menu
  (:require [reagent.core :refer [atom]]
            [re-frame.core :refer [dispatch subscribe]]
            [reagent-material-ui.components :as ui]
            [reagent-material-ui.icons.zoom-out-map :refer [zoom-out-map]]
            [reagent-material-ui.styles :refer [styled]]
            [mtg-pairings-server.events.pairings :as events]
            [mtg-pairings-server.subscriptions.pairings :as subs]
            [mtg-pairings-server.util.material-ui :refer [wrap-on-change]]))


(defn round-select* [{:keys [value rounds class-name label
                             on-change on-click primary?]}]
  [:<>
   [ui/button {:on-click on-click
               :color    (if primary? :primary :default)
               :variant  :outlined}
    label]
   (into [ui/select {:class     class-name
                     :on-change (wrap-on-change on-change)
                     :value     (or value "")}]
         (for [round rounds]
           [ui/menu-item {:value (str round)}
            (str round)]))])

(def round-select (styled round-select* (fn [{:keys [theme]}]
                                          (let [spacing (:spacing theme)]
                                            {:width        60
                                             :margin-left  (spacing 1)
                                             :margin-right (spacing 1)}))))

(defn time-field* [{:keys [class-name value on-change]}]
  [ui/text-field {:class     class-name
                  :type      :number
                  :min       0
                  :max       100
                  :value     value
                  :on-change (wrap-on-change on-change)}])

(def time-field (styled time-field* (fn [{:keys [theme]}]
                                      (let [spacing (:spacing theme)]
                                        {:width        40
                                         :margin-left  (spacing 1)
                                         :margin-right (spacing 1)}))))

(defn menu []
  (let [new-pairings (subscribe [::subs/organizer :new-pairings])
        pairings-rounds (subscribe [::subs/organizer :tournament :pairings])
        new-standings (subscribe [::subs/organizer :new-standings])
        standings-rounds (subscribe [::subs/organizer :tournament :standings])
        new-pods (subscribe [::subs/organizer :new-pods])
        pods-rounds (subscribe [::subs/organizer :tournament :pods])
        new-seatings (subscribe [::subs/organizer :new-seatings])
        clock-running (subscribe [::subs/organizer :clock :running])
        pairings-round (subscribe [::subs/organizer :selected-pairings])
        standings-round (subscribe [::subs/organizer :selected-standings])
        pods-round (subscribe [::subs/organizer :selected-pods])
        minutes (atom 50)]
    (fn menu-render []
      [ui/app-bar {:color    :default
                   :position :static}
       [ui/toolbar
        [ui/icon-button
         {:on-click #(dispatch [::events/popup-organizer-menu])}
         [zoom-out-map]]
        [round-select {:value     @pairings-round
                       :rounds    @pairings-rounds
                       :on-change #(dispatch [::events/organizer-mode :select-pairings %])
                       :on-click  #(dispatch [::events/organizer-mode :pairings (js/parseInt @pairings-round)])
                       :primary?  @new-pairings
                       :label     "Pairings"}]
        [round-select {:value     @standings-round
                       :rounds    @standings-rounds
                       :on-change #(dispatch [::events/organizer-mode :select-standings %])
                       :on-click  #(dispatch [::events/organizer-mode :standings (js/parseInt @standings-round)])
                       :primary?  @new-standings
                       :label     "Standings"}]
        [round-select {:value     @pods-round
                       :rounds    @pods-rounds
                       :on-change #(dispatch [::events/organizer-mode :select-pods %])
                       :on-click  #(dispatch [::events/organizer-mode :pods (js/parseInt @pods-round)])
                       :primary?  @new-pods
                       :label     "Pods"}]
        [ui/button {:on-click #(dispatch [::events/organizer-mode :seatings])
                    :color    (if @new-seatings :primary :default)
                    :variant  :outlined}
         "Seatings"]
        [ui/button {:on-click #(dispatch [::events/organizer-mode :clock])
                    :variant  :outlined
                    :style    {:margin-left  "10px"
                               :margin-right "10px"}}
         "Kello"]
        [time-field {:value     @minutes
                     :on-change #(reset! minutes %)}]
        [ui/button-group {:variant :outlined}
         [ui/button {:on-click #(dispatch [::events/organizer-mode :set-clock @minutes])
                     :variant  :outlined
                     :disabled @clock-running}
          "Aseta"]
         [ui/button {:on-click #(dispatch [::events/organizer-mode :start-clock])
                     :variant  :outlined
                     :color    :primary
                     :disabled @clock-running}
          "Käynnistä"]
         [ui/button {:on-click #(dispatch [::events/organizer-mode :stop-clock])
                     :variant  :outlined
                     :color    :secondary
                     :disabled (not @clock-running)}
          "Pysäytä"]]]])))
