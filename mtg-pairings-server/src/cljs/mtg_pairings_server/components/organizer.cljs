(ns mtg-pairings-server.components.organizer
  (:require [reagent.core :refer [atom]]
            [re-frame.core :refer [dispatch subscribe]]
            [mtg-pairings-server.util.util :refer [cls indexed]]
            [mtg-pairings-server.util.mtg-util :refer [duplicate-pairings]]
            [mtg-pairings-server.components.tournament :refer [standing-table]]))

(defn round-select [type a rounds]
  [:select.form-control
   {:on-change #(reset! a (-> % .-target .-value))
    :value     @a}
   (for [round @rounds]
     ^{:key (str type round)}
     [:option {:value round}
      round])])

(defn menu []
  (let [new-pairings (subscribe [:organizer :new-pairings])
        pairings-rounds (subscribe [:organizer :tournament :pairings])
        new-standings (subscribe [:organizer :new-standings])
        standings-rounds (subscribe [:organizer :tournament :standings])
        new-pods (subscribe [:organizer :new-pods])
        pods-rounds (subscribe [:organizer :tournament :pods])
        clock-running (subscribe [:organizer :clock :running])
        pairings-round (atom "1")
        standings-round (atom "1")
        pods-round (atom "1")
        minutes (atom 50)]
    (fn []
      [:div#organizer-menu
       [:div.form-inline
        [:a {:on-click #(dispatch [:popup-organizer-menu])}
         [:i.glyphicon.glyphicon-resize-full]]
        [:button.btn
         {:on-click #(dispatch [:organizer-mode :pairings (js/parseInt @pairings-round)])
          :class    (if @new-pairings "btn-success" "btn-default")}
         "Pairings"]
        [round-select :pairings pairings-round pairings-rounds]
        [:button.btn
         {:on-click #(dispatch [:organizer-mode :standings (js/parseInt @standings-round)])
          :class    (if @new-standings "btn-success" "btn-default")}
         "Standings"]
        [round-select :standings standings-round standings-rounds]
        [:button.btn
         {:on-click #(dispatch [:organizer-mode :pods (js/parseInt @pods-round)])
          :class    (if @new-pods "btn-success" "btn-default")}
         "Pods"]
        [round-select :pods pods-round pods-rounds]
        [:button.btn.btn-default
         {:on-click #(dispatch [:organizer-mode :seatings])}
         "Seatings"]
        [:button.btn.btn-default
         {:on-click #(dispatch [:organizer-mode :clock])}
         "Kello"]
        [:input.form-control
         {:type      "number"
          :on-change #(reset! minutes (-> % .-target .-value))
          :value     @minutes
          :min       0
          :max       99}]
        [:button.btn.btn-default
         {:on-click #(dispatch [:organizer-mode :set-clock @minutes])
          :disabled (when @clock-running "disabled")}
         "Aseta"]
        [:button.btn.btn-success
         {:on-click #(dispatch [:organizer-mode :start-clock])
          :disabled (when @clock-running "disabled")}
         "Käynnistä"]
        [:button.btn.btn-danger
         {:on-click #(dispatch [:organizer-mode :stop-clock])
          :disabled (when-not @clock-running "disabled")}
         "Pysäytä"]]])))

(defn pairing [data even? display-round? pairing?]
  [:div.pairing {:class (cls {:even     even?
                              :odd      (not even?)
                              :no-round (not display-round?)})}
   (when (and display-round? pairing?)
     [:h4 (str "Kierros " (:round_number data))])
   (when-not pairing?
     [:h4 "Seating"])
   [:span.table-number (:table_number data)]
   (when-not pairing?
     [:span
      [:div.names (:team1_name data)]])
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
  (let [pairings (subscribe [:organizer :pairings])
        pairings-round (subscribe [:organizer :pairings-round])
        tournament (subscribe [:organizer :tournament])]
    (fn []
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
  (let [seatings (subscribe [:organizer :seatings])
        tournament (subscribe [:organizer :tournament])]
    (fn []
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
  (let [pods (subscribe [:organizer :pods])
        tournament (subscribe [:organizer :tournament])]
    (fn []
      [:div.organizer-pods
       [:h2 (str (:name @tournament) " - pods")]
       (for [[i column] (indexed (split-data @pods))]
         ^{:key (str "pods-column-" i)}
         [pod-column column])])))

(defn standings []
  (let [standings (subscribe [:organizer :standings])
        tournament (subscribe [:organizer :tournament])
        standings-round (subscribe [:organizer :standings-round])]
    (fn []
      [:div.organizer-standings
       [:h2 (str (:name @tournament) " - kierros " @standings-round)]
       (for [[i column] (indexed (split-data @standings))]
         ^{:key (str "standings-column-" i)}
         [standing-table column])])))

(defn clock []
  (let [c (subscribe [:organizer :clock])]
    (fn []
      [:div.organizer-clock
       {:class (when (:timeout @c) "timeout")}
       (:text @c)])))