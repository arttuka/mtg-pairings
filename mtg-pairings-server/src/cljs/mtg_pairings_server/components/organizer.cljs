(ns mtg-pairings-server.components.organizer
  (:require [reagent.core :refer [atom]]
            [re-frame.core :refer [dispatch subscribe]]
            [reagent-material-ui.components :as ui]
            [reagent-material-ui.icons.zoom-out-map :refer [zoom-out-map]]
            [goog.string :as gstring]
            [mtg-pairings-server.events.pairings :as events]
            [mtg-pairings-server.subscriptions.pairings :as subs]
            [mtg-pairings-server.util :refer [cls indexed]]
            [mtg-pairings-server.util.material-ui :refer [wrap-on-change]]
            [mtg-pairings-server.util.mtg :refer [bye? duplicate-pairings]]
            [mtg-pairings-server.components.tournament :refer [standing-table]]))

(defn round-select [type a rounds]
  [ui/select
   {:on-change (wrap-on-change #(dispatch [::events/organizer-mode type %]))
    :value     @a
    :style     {:width        "60px"
                :margin-left  "10px"
                :margin-right "10px"}}
   (for [round @rounds]
     ^{:key (str type round)}
     [ui/menu-item {:value (str round)}
      (str round)])])

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
                   :position :static
                   :style    {:margin-bottom  "12px"}}
       [ui/toolbar
        [ui/icon-button
         {:on-click #(dispatch [::events/popup-organizer-menu])}
         [zoom-out-map]]
        [ui/button
         {:on-click #(dispatch [::events/organizer-mode :pairings (js/parseInt @pairings-round)])
          :color    (when @new-pairings :primary)
          :variant  :outlined}
         "Pairings"]
        [round-select :select-pairings pairings-round pairings-rounds]
        [ui/button
         {:on-click #(dispatch [::events/organizer-mode :standings (js/parseInt @standings-round)])
          :color    (when @new-standings :primary)
          :variant  :outlined}
         "Standings"]
        [round-select :select-standings standings-round standings-rounds]
        [ui/button
         {:on-click #(dispatch [::events/organizer-mode :pods (js/parseInt @pods-round)])
          :color    (when @new-pods :primary)
          :variant  :outlined}
         "Pods"]
        [round-select :select-pods pods-round pods-rounds]
        [ui/button
         {:on-click #(dispatch [::events/organizer-mode :seatings])
          :color    (when @new-seatings :primary)
          :variant  :outlined}
         "Seatings"]
        [ui/button
         {:on-click #(dispatch [::events/organizer-mode :clock])
          :variant  :outlined
          :style    {:margin-left  "10px"
                     :margin-right "10px"}}
         "Kello"]
        [ui/text-field
         {:type      :number
          :value     @minutes
          :min       0
          :max       100
          :on-change (fn [_ new-value]
                       (reset! minutes new-value))
          :style     {:width        "40px"
                      :margin-left  "10px"
                      :margin-right "10px"}
          :id        :clock-minutes}]
        [ui/button
         {:on-click #(dispatch [::events/organizer-mode :set-clock @minutes])
          :variant  :outlined
          :disabled @clock-running}
         "Aseta"]
        [ui/button
         {:on-click #(dispatch [::events/organizer-mode :start-clock])
          :variant  :outlined
          :color    :primary
          :disabled @clock-running
          :style    {:margin-left  "10px"
                     :margin-right "10px"}}
         "Käynnistä"]
        [ui/button
         {:on-click #(dispatch [::events/organizer-mode :stop-clock])
          :variant  :outlined
          :color    :secondary
          :disabled (not @clock-running)}
         "Pysäytä"]]])))

(defn pairing [data]
  (let [bye (bye? data)]
    [:div.row.pairing.no-round {:class (when bye :bye)}
     [:span.table-number (when-not bye (or (:table_number data) (:pod data)))]
     [:span.player (:team1_name data) [:span.points (:team1_points data)]]
     [:span.player.opponent (:team2_name data) [:span.points (when-not bye (:team2_points data))]]]))

(defn pairings []
  (let [pairings (subscribe [::subs/organizer :pairings])
        pairings-round (subscribe [::subs/organizer :pairings-round])
        tournament (subscribe [::subs/organizer :tournament])]
    (fn pairings-render []
      [:div.organizer-pairings
       [:h2 (str (:name @tournament) " - kierros " @pairings-round)]
       [:div.column
        (for [p (sort-by :team1_name (duplicate-pairings @pairings))]
          ^{:key (:team1_name p)}
          [pairing p])]])))

(defn seatings []
  (let [seatings (subscribe [::subs/organizer :seatings])
        tournament (subscribe [::subs/organizer :tournament])]
    (fn seatings-render []
      [:div.organizer-seatings
       [:h2 (str (:name @tournament) " - seatings")]
       [:div.column
        (for [s @seatings]
          ^{:key (:name s)}
          [:div.row.seating
           [:span.table-number (:table_number s)]
           [:span.name (:name s)]])]])))

(defn pods []
  (let [pods (subscribe [::subs/organizer :pods])
        tournament (subscribe [::subs/organizer :tournament])]
    (fn pods-render []
      [:div.organizer-pods
       [:h2 (str (:name @tournament) " - pods")]
       [:div.column
        (for [p @pods]
          ^{:key (:team_name p)}
          [:div.row.seat
           [:span.pod-number (:pod p)]
           [:span.seat-number (:seat p)]
           [:span.name (:team_name p)]])]])))

(defn percentage [n]
  (gstring/format "%.3f" (* 100 n)))

(defn standings []
  (let [standings (subscribe [::subs/organizer :standings])
        tournament (subscribe [::subs/organizer :tournament])
        standings-round (subscribe [::subs/organizer :standings-round])]
    (fn standings-render []
      [:div.organizer-standings
       [:h2 (str (:name @tournament) " - kierros " @standings-round)]
       [:div.column
        (for [{:keys [rank team_name points omw pgw ogw]} @standings]
          ^{:key (str "standings-" rank)}
          [:div.row.standing
           [:span.rank rank]
           [:span.player team_name]
           [:span.points points]
           [:span.omw (percentage omw)]
           [:span.pgw (percentage pgw)]
           [:span.ogw (percentage ogw)]])]])))

(defn clock []
  (let [c (subscribe [::subs/organizer :clock])]
    (fn clock-render []
      [:div.organizer-clock
       {:class (when (:timeout @c) "timeout")}
       (:text @c)])))
