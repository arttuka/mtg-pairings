(ns mtg-pairings-server.components.organizer
  (:require [reagent.core :refer [atom]]
            [re-frame.core :refer [dispatch subscribe]]
            [cljsjs.material-ui]
            [cljs-react-material-ui.core]
            [cljs-react-material-ui.reagent :as ui]
            [cljs-react-material-ui.icons :as icons]
            [goog.string :as gstring]
            [mtg-pairings-server.events :as events]
            [mtg-pairings-server.subscriptions :as subs]
            [mtg-pairings-server.util.util :refer [cls indexed]]
            [mtg-pairings-server.util.mtg-util :refer [duplicate-pairings]]
            [mtg-pairings-server.components.tournament :refer [standing-table]]))

(defn round-select [type a rounds]
  [ui/select-field
   {:on-change (fn [_ _ new-value]
                 (dispatch [::events/organizer-mode type new-value]))
    :value     @a
    :style     {:vertical-align :bottom
                :width          "60px"
                :margin-left    "10px"
                :margin-right   "10px"}}
   (for [round @rounds]
     ^{:key (str type round)}
     [ui/menu-item
      {:value        (str round)
       :primary-text (str round)}])])

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
      [ui/paper
       {:style {:padding-bottom "6px"
                :margin-bottom  "12px"}}
       [ui/icon-button
        {:on-click #(dispatch [::events/popup-organizer-menu])
         :style    {:vertical-align :bottom}}
        [icons/maps-zoom-out-map]]
       [ui/raised-button
        {:label    "Pairings"
         :on-click #(dispatch [::events/organizer-mode :pairings (js/parseInt @pairings-round)])
         :primary  @new-pairings}]
       [round-select :select-pairings pairings-round pairings-rounds]
       [ui/raised-button
        {:label    "Standings"
         :on-click #(dispatch [::events/organizer-mode :standings (js/parseInt @standings-round)])
         :primary  @new-standings}]
       [round-select :select-standings standings-round standings-rounds]
       [ui/raised-button
        {:label    "Pods"
         :on-click #(dispatch [::events/organizer-mode :pods (js/parseInt @pods-round)])
         :primary  @new-pods}]
       [round-select :select-pods pods-round pods-rounds]
       [ui/raised-button
        {:label    "Seatings"
         :on-click #(dispatch [::events/organizer-mode :seatings])
         :primary  @new-seatings}]
       [ui/raised-button
        {:label    "Kello"
         :on-click #(dispatch [::events/organizer-mode :clock])
         :style    {:margin-left  "10px"
                    :margin-right "10px"}}]
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
       [ui/raised-button
        {:label    "Aseta"
         :on-click #(dispatch [::events/organizer-mode :set-clock @minutes])
         :disabled @clock-running}]
       [ui/raised-button
        {:label    "Käynnistä"
         :on-click #(dispatch [::events/organizer-mode :start-clock])
         :primary  true
         :disabled @clock-running
         :style    {:margin-left  "10px"
                    :margin-right "10px"}}]
       [ui/raised-button
        {:label     "Pysäytä"
         :on-click  #(dispatch [::events/organizer-mode :stop-clock])
         :secondary true
         :disabled  (not @clock-running)}]])))

(defn pairing [data]
  (let [bye? (= (:table_number data) 0)]
    [:div.row.pairing.no-round {:class (when bye? :bye)}
     [:span.table-number (when-not bye? (or (:table_number data) (:pod data)))]
     [:span.player (:team1_name data) [:span.points (:team1_points data)]]
     [:span.player.opponent (:team2_name data) [:span.points (when-not bye? (:team2_points data))]]]))

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
