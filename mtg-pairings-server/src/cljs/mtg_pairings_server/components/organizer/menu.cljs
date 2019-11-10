(ns mtg-pairings-server.components.organizer.menu
  (:require [reagent.core :as reagent :refer [atom]]
            [re-frame.core :refer [dispatch subscribe]]
            [reagent-material-ui.components :as ui]
            [reagent-material-ui.icons.arrow-drop-down :refer [arrow-drop-down]]
            [reagent-material-ui.icons.zoom-out-map :refer [zoom-out-map]]
            [reagent-material-ui.styles :refer [with-styles]]
            [mtg-pairings-server.components.organizer.clock-controls :refer [clock-buttons time-field]]
            [mtg-pairings-server.components.organizer.common :refer [select-button]]
            [mtg-pairings-server.events.pairings :as events]
            [mtg-pairings-server.subscriptions.common :as common-subs]
            [mtg-pairings-server.subscriptions.pairings :as subs]
            [mtg-pairings-server.util.material-ui :refer [wrap-on-change]]))

(def popout-button ((with-styles (fn [{:keys [palette spacing]}]
                                   {:root {:background-color (get-in palette [:primary :main])
                                           :color            :white
                                           :margin-right     (spacing 1)}}))
                    ui/icon-button))

(def round-select ((with-styles (fn [{:keys [spacing]}]
                                  {:button-group {:margin (spacing 0 1)
                                                  :flex   "0 1 180px"}
                                   :menu         {:width 160}}))
                   select-button))

(defn tournament-menu-item [{:keys [selected on-click item]}]
  [ui/menu-item {:selected selected
                 :on-click on-click}
   [ui/list-item-text {:primary   (:name item)
                       :secondary (:organizer item)}]])

(def tournament-select ((with-styles (fn [{:keys [spacing]}]
                                       {:button-group {:margin (spacing 0 1)
                                                       :width  250}}))
                        select-button))

(def button ((with-styles (fn [{:keys [spacing]}]
                            {:root {:flex   "0 1 120px"
                                    :margin (spacing 0 1)}}))
             ui/button))

(defn menu []
  (let [page (subscribe [::common-subs/page])
        tournaments (subscribe [::subs/organizer :tournaments])
        new-pairings (subscribe [::subs/organizer :new-pairings])
        pairings-rounds (subscribe [::subs/organizer :tournament :pairings])
        new-standings (subscribe [::subs/organizer :new-standings])
        standings-rounds (subscribe [::subs/organizer :tournament :standings])
        new-pods (subscribe [::subs/organizer :new-pods])
        pods-rounds (subscribe [::subs/organizer :tournament :pods])
        new-seatings (subscribe [::subs/organizer :new-seatings])
        seatings (subscribe [::subs/organizer :tournament :seatings])
        clock-running (subscribe [::subs/clock-running])
        selected-clock (subscribe [::subs/organizer :selected-clock])
        pairings-round (subscribe [::subs/organizer :selected-pairings])
        standings-round (subscribe [::subs/organizer :selected-standings])
        pods-round (subscribe [::subs/organizer :selected-pods])
        minutes (atom 50)
        selected-tournament (atom nil)
        select-tournament (fn [t]
                            (reset! selected-tournament t)
                            (dispatch [::events/organizer-mode :clear])
                            (dispatch [::events/load-organizer-tournament (:id t)]))
        set-clock #(dispatch [::events/organizer-mode :set-clock @minutes])
        start-clock #(dispatch [::events/organizer-mode :start-clock])
        stop-clock #(dispatch [::events/organizer-mode :stop-clock])
        add-clock #(dispatch [::events/organizer-mode :add-clock @minutes])
        remove-clock #(dispatch [::events/organizer-mode :remove-clock])]
    (fn []
      (let [multi? (nil? (:id @page))]
        [ui/app-bar {:color    :default
                     :position :static}
         [ui/toolbar
          [popout-button
           {:on-click #(dispatch [::events/popup-organizer-menu])}
           [zoom-out-map]]
          (when multi?
            [tournament-select {:items            @tournaments
                                :value            @selected-tournament
                                :on-change        select-tournament
                                :variant          :outlined
                                :item-to-label    (fn [item] (or (:name item) "Valitse turnaus"))
                                :render-menu-item tournament-menu-item}])
          [round-select {:value         @pairings-round
                         :items         @pairings-rounds
                         :on-change     #(dispatch [::events/organizer-mode :select-pairings %])
                         :on-click      #(dispatch [::events/organizer-mode :pairings (js/parseInt @pairings-round)])
                         :variant       (if @new-pairings :contained :outlined)
                         :item-to-label #(str "Pairings " %)}]
          [round-select {:value         @standings-round
                         :items         @standings-rounds
                         :on-change     #(dispatch [::events/organizer-mode :select-standings %])
                         :on-click      #(dispatch [::events/organizer-mode :standings (js/parseInt @standings-round)])
                         :variant       (if @new-standings :contained :outlined)
                         :item-to-label #(str "Standings " %)}]
          [round-select {:value         @pods-round
                         :items         @pods-rounds
                         :on-change     #(dispatch [::events/organizer-mode :select-pods %])
                         :on-click      #(dispatch [::events/organizer-mode :pods (js/parseInt @pods-round)])
                         :variant       (if @new-pods :contained :outlined)
                         :item-to-label #(str "Pods " %)}]
          [button {:on-click #(dispatch [::events/organizer-mode :seatings])
                   :color    :primary
                   :variant  (if @new-seatings :contained :outlined)
                   :disabled (not @seatings)}
           "Seatings"]
          [button {:on-click #(dispatch [::events/organizer-mode :clock])
                   :variant  :outlined
                   :color    :primary}
           "Kello"]
          [time-field {:value     @minutes
                       :on-change #(reset! minutes %)}]
          [clock-buttons {:set-clock      set-clock
                          :start-clock    start-clock
                          :stop-clock     stop-clock
                          :add-clock      add-clock
                          :remove-clock   remove-clock
                          :clock-running  @clock-running
                          :selected-clock (some? @selected-clock)}]]]))))

