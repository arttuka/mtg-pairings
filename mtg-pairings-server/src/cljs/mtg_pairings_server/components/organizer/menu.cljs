(ns mtg-pairings-server.components.organizer.menu
  (:require [reagent.core :as reagent :refer [atom]]
            [re-frame.core :refer [dispatch subscribe]]
            [reagent-material-ui.components :as ui]
            [reagent-material-ui.icons.arrow-drop-down :refer [arrow-drop-down]]
            [reagent-material-ui.icons.zoom-out-map :refer [zoom-out-map]]
            [reagent-material-ui.styles :refer [styled with-styles]]
            [goog.object :as obj]
            [mtg-pairings-server.events.pairings :as events]
            [mtg-pairings-server.subscriptions.pairings :as subs]
            [mtg-pairings-server.util.material-ui :refer [wrap-on-change]]))

(def popout-button ((with-styles (fn [{:keys [palette spacing]}]
                                   {:root {:background-color (get-in palette [:primary :main])
                                           :color            :white
                                           :margin-right     (spacing 1)}}))
                    ui/icon-button))

(defn round-dropdown [{:keys [class value label rounds on-close on-menu-item-click]}]
  (reagent/as-element
   [ui/paper {:class class}
    [ui/click-away-listener {:on-click-away on-close}
     [ui/menu-list
      (for [round rounds]
        ^{:key (str label "-option-" round)}
        [ui/menu-item {:selected (= value (str round))
                       :on-click #(on-menu-item-click (str round))}
         (str label " " round)])]]]))

(defn round-select* [{:keys [on-change]}]
  (let [open? (atom false)
        toggle-open #(swap! open? not)
        ^js/React.Ref anchor-ref (.createRef js/React)
        on-close (fn [^js/Event event]
                   (when-not (some-> (.-current anchor-ref)
                                     (.contains (.-target event)))
                     (reset! open? false)))
        on-menu-item-click (fn [new-value]
                             (reset! open? false)
                             (on-change new-value))]
    (fn [{:keys [classes value rounds label on-click highlight?]}]
      (let [disabled? (empty? rounds)]
        [:<>
         [ui/button-group {:variant  (if highlight? :contained :outlined)
                           :color    :primary
                           :ref      anchor-ref
                           :classes  {:root (:root classes)}
                           :disabled disabled?}
          [ui/button {:on-click on-click
                      :variant  (if highlight? :contained :outlined)
                      :color    :primary
                      :classes  {:root (:button-root classes)}}
           (str label " " value)]
          [ui/button {:variant  (if highlight? :contained :outlined)
                      :color    :primary
                      :size     :small
                      :on-click toggle-open}
           [arrow-drop-down]]]
         [ui/popper {:open           @open?
                     :anchor-el      (.-current anchor-ref)
                     :transition     true
                     :disable-portal true}
          (fn [props]
            (reagent/as-element
             [ui/grow (assoc (js->clj (obj/get props "TransitionProps"))
                             :children (round-dropdown {:class              (:menu-root classes)
                                                        :value              value
                                                        :label              label
                                                        :rounds             rounds
                                                        :on-close           on-close
                                                        :on-menu-item-click on-menu-item-click}))]))]]))))

(def round-select ((with-styles (fn [{:keys [spacing]}]
                                  {:root        {:margin (spacing 0 1)
                                                 :flex   "0 1 180px"}
                                   :button-root {:flex 1}
                                   :menu-root   {:width 160}}))
                   round-select*))

(def button (styled ui/button (fn [{:keys [theme]}]
                                (let [spacing (:spacing theme)]
                                  {:flex   "0 1 120px"
                                   :margin (spacing 0 1)}))))

(def button-group ((with-styles (fn [{:keys [spacing]}]
                                  {:root    {:margin (spacing 0 1)
                                             :flex   "0 3 360px"}
                                   :grouped {:flex 1}}))
                   ui/button-group))

(defn time-field* [{:keys [class-name value on-change]}]
  [ui/text-field {:class     class-name
                  :type      :number
                  :min       0
                  :max       100
                  :value     value
                  :on-change (wrap-on-change on-change)}])

(def time-field (styled time-field* (fn [{:keys [theme]}]
                                      (let [spacing (:spacing theme)]
                                        {:width  40
                                         :margin (spacing 0 1)}))))

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
        [popout-button
         {:on-click #(dispatch [::events/popup-organizer-menu])}
         [zoom-out-map]]
        [round-select {:value      @pairings-round
                       :rounds     @pairings-rounds
                       :on-change  #(dispatch [::events/organizer-mode :select-pairings %])
                       :on-click   #(dispatch [::events/organizer-mode :pairings (js/parseInt @pairings-round)])
                       :highlight? @new-pairings
                       :label      "Pairings"}]
        [round-select {:value      @standings-round
                       :rounds     @standings-rounds
                       :on-change  #(dispatch [::events/organizer-mode :select-standings %])
                       :on-click   #(dispatch [::events/organizer-mode :standings (js/parseInt @standings-round)])
                       :highlight? @new-standings
                       :label      "Standings"}]
        [round-select {:value      @pods-round
                       :rounds     @pods-rounds
                       :on-change  #(dispatch [::events/organizer-mode :select-pods %])
                       :on-click   #(dispatch [::events/organizer-mode :pods (js/parseInt @pods-round)])
                       :highlight? @new-pods
                       :label      "Pods"}]
        [button {:on-click #(dispatch [::events/organizer-mode :seatings])
                 :color    :primary
                 :variant  (if @new-seatings :contained :outlined)}
         "Seatings"]
        [button {:on-click #(dispatch [::events/organizer-mode :clock])
                 :variant  :outlined
                 :color    :primary}
         "Kello"]
        [time-field {:value     @minutes
                     :on-change #(reset! minutes %)}]
        [button-group {:variant :outlined}
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
