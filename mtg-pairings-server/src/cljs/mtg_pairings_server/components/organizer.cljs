(ns mtg-pairings-server.components.organizer
  (:require [reagent.core :refer [atom]]
            [re-frame.core :refer [dispatch subscribe]]
            [mtg-pairings-server.events :as events]
            [mtg-pairings-server.subscriptions :as subs]
            [mtg-pairings-server.util.util :refer [cls indexed]]
            [mtg-pairings-server.util.mtg-util :refer [duplicate-pairings]]
            [mtg-pairings-server.components.tournament :refer [standing-table]]))

(defn round-select [type a rounds]
  [:select.form-control
   {:on-change #(dispatch [::events/organizer-mode type (-> % .-target .-value)])
    :value     (or @a "")}
   (for [round @rounds]
     ^{:key (str type round)}
     [:option {:value round}
      round])])

(defn menu []
  (let [new-pairings (subscribe [::subs/organizer :new-pairings])
        pairings-rounds (subscribe [::subs/organizer :tournament :pairings])
        new-standings (subscribe [::subs/organizer :new-standings])
        standings-rounds (subscribe [::subs/organizer :tournament :standings])
        new-pods (subscribe [::subs/organizer :new-pods])
        pods-rounds (subscribe [::subs/organizer :tournament :pods])
        clock-running (subscribe [::subs/organizer :clock :running])
        pairings-round (subscribe [::subs/organizer :selected-pairings])
        standings-round (subscribe [::subs/organizer :selected-standings])
        pods-round (subscribe [::subs/organizer :selected-pods])
        minutes (atom 50)]
    (fn menu-render []
      [:div#organizer-menu
       [:div.form-inline
        [:a {:on-click #(dispatch [::events/popup-organizer-menu])}
         [:i.glyphicon.glyphicon-resize-full]]
        [:button.btn
         {:on-click #(dispatch [::events/organizer-mode :pairings (js/parseInt @pairings-round)])
          :class    (if @new-pairings "btn-success" "btn-default")}
         "Pairings"]
        [round-select :select-pairings pairings-round pairings-rounds]
        [:button.btn
         {:on-click #(dispatch [::events/organizer-mode :standings (js/parseInt @standings-round)])
          :class    (if @new-standings "btn-success" "btn-default")}
         "Standings"]
        [round-select :select-standings standings-round standings-rounds]
        [:button.btn
         {:on-click #(dispatch [::events/organizer-mode :pods (js/parseInt @pods-round)])
          :class    (if @new-pods "btn-success" "btn-default")}
         "Pods"]
        [round-select :select-pods pods-round pods-rounds]
        [:button.btn.btn-default
         {:on-click #(dispatch [::events/organizer-mode :seatings])}
         "Seatings"]
        [:button.btn.btn-default
         {:on-click #(dispatch [::events/organizer-mode :clock])}
         "Kello"]
        [:input.form-control
         {:type      "number"
          :on-change #(reset! minutes (-> % .-target .-value))
          :value     @minutes
          :min       0
          :max       99}]
        [:button.btn.btn-default
         {:on-click #(dispatch [::events/organizer-mode :set-clock @minutes])
          :disabled (when @clock-running "disabled")}
         "Aseta"]
        [:button.btn.btn-success
         {:on-click #(dispatch [::events/organizer-mode :start-clock])
          :disabled (when @clock-running "disabled")}
         "Käynnistä"]
        [:button.btn.btn-danger
         {:on-click #(dispatch [::events/organizer-mode :stop-clock])
          :disabled (when-not @clock-running "disabled")}
         "Pysäytä"]]])))

(defn pairing [data even? display-round? pairing?]
  [:div.pairing {:class (cls {:even     even?
                              :odd      (not even?)
                              :no-round (not display-round?)})}
   (when (and display-round? pairing?)
     [:h4 (str "Kierros " (:round_number data))])
   (when-not pairing?
     [:h4 (if (:pod data)
            "Pod"
            "Seating")])
   [:span.table-number (or (:table_number data) (:pod data))]
   (when-not pairing?
     [:span
      [:div.names (or (:team1_name data)
                      (str "Seat " (:seat data)))]])
   (when pairing?
     [:span
      [:div.names
       [:span.player (str (:team1_name data) " (" (:team1_points data) ")")]
       [:span.hidden-xs " - "]
       [:br.hidden-sm.hidden-md.hidden-lg]
       [:span.opponent (str (:team2_name data) " (" (:team2_points data) ")")]]
      [:div.points
       [:span.player (:team1_wins data)]
       [:span.hidden-xs " - "]
       [:br.hidden-sm.hidden-md.hidden-lg]
       [:span.opponent (:team2_wins data)]]])])

(defn pairing-column [data]
  [:div.pairing-column
   (for [[i p] (indexed data)]
     ^{:key (:team1_name p)}
     [pairing p (even? i) false true])])

(defn split-data [data]
  (let [n (count data)
        per-column (Math/ceil (/ n (Math/ceil (/ n 40))))]
    (partition-all per-column data)))

(defn split-pairings [pairings]
  (let [duplicated (sort-by :team1_name (duplicate-pairings pairings))]
    (split-data duplicated)))

(defn pairings []
  (let [pairings (subscribe [::subs/organizer :pairings])
        pairings-round (subscribe [::subs/organizer :pairings-round])
        tournament (subscribe [::subs/organizer :tournament])]
    (fn pairings-render []
      [:div.organizer-pairings
       [:h2 (str (:name @tournament) " - kierros " @pairings-round)]
       (for [[i column] (indexed (split-pairings @pairings))]
         ^{:key (str "pairing-column-" i)}
         [pairing-column column])])))

(defn seating-column [data]
  [:div.seating-column
   (for [[i s] (indexed data)]
     ^{:key (:name s)}
     [:div.seating
      {:class (cls {:even (even? i)
                    :odd  (odd? i)})}
      [:span.table-number (:table_number s)]
      [:span
       [:div.name (:name s)]]])])

(defn seatings []
  (let [seatings (subscribe [::subs/organizer :seatings])
        tournament (subscribe [::subs/organizer :tournament])]
    (fn seatings-render []
      [:div.organizer-seatings
       [:h2 (str (:name @tournament) " - seatings")]
       (for [[i column] (indexed (split-data @seatings))]
         ^{:key (str "seating-column-" i)}
         [seating-column column])])))

(defn pod-column [data]
  [:div.pod-column
   (for [[i s] (indexed data)]
     ^{:key (:team_name s)}
     [:div.seat
      {:class (cls {:even (even? i)
                    :odd  (odd? i)})}
      [:span.pod-number (:pod s)]
      [:span.seat-number (:seat s)]
      [:span
       [:div.name (:team_name s)]]])])

(defn pods []
  (let [pods (subscribe [::subs/organizer :pods])
        tournament (subscribe [::subs/organizer :tournament])]
    (fn pods-render []
      [:div.organizer-pods
       [:h2 (str (:name @tournament) " - pods")]
       (for [[i column] (indexed (split-data @pods))]
         ^{:key (str "pods-column-" i)}
         [pod-column column])])))

(defn standings []
  (let [standings (subscribe [::subs/organizer :standings])
        tournament (subscribe [::subs/organizer :tournament])
        standings-round (subscribe [::subs/organizer :standings-round])]
    (fn standings-render []
      [:div.organizer-standings
       [:h2 (str (:name @tournament) " - kierros " @standings-round)]
       (for [[i column] (indexed (split-data @standings))]
         ^{:key (str "standings-column-" i)}
         [standing-table column])])))

(defn clock []
  (let [c (subscribe [::subs/organizer :clock])]
    (fn clock-render []
      [:div.organizer-clock
       {:class (when (:timeout @c) "timeout")}
       (:text @c)])))
