(ns mtg-pairings-server.components.tournament
  (:require [reagent.core :as reagent]
            [re-frame.core :refer [subscribe dispatch]]
            [reagent-material-ui.components :as ui]
            [reagent-material-ui.icons.expand-more :refer [expand-more]]
            [reagent-material-ui.icons.keyboard-arrow-down :refer [keyboard-arrow-down]]
            [goog.string :as gstring]
            [goog.string.format]
            [mtg-pairings-server.events.pairings :as events]
            [mtg-pairings-server.subscriptions.pairings :as subs]
            [mtg-pairings-server.routes.pairings :refer [tournaments-path tournament-path pairings-path standings-path pods-path seatings-path bracket-path]]
            [mtg-pairings-server.components.paging :refer [with-paging]]
            [mtg-pairings-server.components.filter :refer [filters]]
            [mtg-pairings-server.styles.common :as styles]
            [mtg-pairings-server.util :refer [format-date indexed]]
            [mtg-pairings-server.util.mtg :refer [bye?]]))

;; TODO withStyles
(defn tournament-card-header
  ([data]
   (tournament-card-header data {}))
  ([data opts]
   (let [{:keys [expanded? on-expand]} opts]
     [ui/card-header
      (merge
       {:class     (when on-expand :card-header-expandable)
        :title     (reagent/as-element
                    [:a {:href  (tournament-path {:id (:id data)})
                         :style {:font-size "20px"}}
                     (:name data)])
        :subheader (str (format-date (:day data)) " — " (:organizer data))
        :on-click  on-expand
        :action    (when on-expand
                     (reagent/as-element
                      [ui/icon-button {:class    [:card-header-button
                                                  (when expanded? :card-header-button-expanded)]
                                       :on-click on-expand}
                       [expand-more]]))}
       (dissoc opts :on-expand :expanded?))])))

(defn tournament [data]
  (when data
    [ui/card
     {:class "tournament"}
     [tournament-card-header data]
     [ui/card-content
      {:style {:padding-top 0}}
      (when (:playoff data)
        [:div.tournament-row
         [ui/button
          {:href       (bracket-path {:id (:id data)})
           :variant    :outlined
           :class-name :tournament-button-wide
           :style      {:width "260px"}}
          "Playoff bracket"]])
      (for [r (:round-nums data)]
        ^{:key [(:id data) r]}
        [:div.tournament-row
         [ui/button-group {:variant :outlined}
          (when (contains? (:pairings data) r)
            [ui/button
             {:href       (pairings-path {:id (:id data), :round r})
              :class-name :tournament-button
              :style      {:width "130px"}}
             (str "Pairings " r)])
          (when (contains? (:standings data) r)
            [ui/button
             {:href       (standings-path {:id (:id data), :round r})
              :class-name :tournament-button
              :style      {:width "130px"}}
             (str "Standings " r)])]])
      (when (or (:seatings data)
                (seq (:pods data)))
        [:div.tournament-row
         [ui/button-group {:variant :outlined}
          (when (:seatings data)
            [ui/button
             {:href       (seatings-path {:id (:id data)})
              :class-name :tournament-button
              :style      {:width "130px"}}
             "Seatings"])
          (for [n (:pods data)]
            ^{:key [(:id data) :pods n]}
            [ui/button
             {:href       (pods-path {:id (:id data), :round n})
              :class-name :tournament-button
              :style      {:width "130px"}}
             (str "Pods " n)])]])]]))

(defn newest-tournaments-list []
  (let [tournaments (subscribe [::subs/newest-tournaments])]
    (fn newest-tournaments-list-render []
      [:div#tournaments
       [:h2.newest-header
        "Aktiiviset turnaukset | "
        [:a {:href (tournaments-path)}
         "Turnausarkistoon"]]
       (if-let [ts (seq @tournaments)]
         (for [t ts]
           ^{:key (:id t)}
           [tournament t])
         [:h3.no-active "Ei aktiivisia turnauksia."])])))

(defn tournament-list []
  [:div
   [filters]
   [with-paging ::events/tournaments-page [::subs/tournaments-page] [::subs/filtered-tournaments]
    (fn tournament-list-render [tournaments]
      [:div#tournaments
       (for [t tournaments]
         ^{:key (:id t)}
         [tournament t])])]])

(defn sortable-header [{:keys [class column sort-key dispatch-key]} & children]
  [:th {:class    class
        :style    (when (= column sort-key) {:color (:accent1-color styles/palette)})
        :on-click #(dispatch [dispatch-key column])}
   [keyboard-arrow-down
    {:style {:vertical-align :baseline
             :position       :absolute
             :left           0
             :color          nil}}]
   children])

(defn pairing-row [pairing]
  (let [bye (bye? pairing)]
    [:tr
     [:td.table (when-not bye (:table_number pairing))]
     [:td.players
      [:span.player1 (:team1_name pairing)]
      [:span.player2.hidden-desktop
       (:team2_name pairing)]]
     [:td.players2.hidden-mobile (:team2_name pairing)]
     (if bye
       [:td.points
        [:span.team1-points (:team1_points pairing)]]
       [:td.points
        [:span.team1-points (:team1_points pairing)]
        [:span.hidden-mobile " - "]
        [:span.team2-points (:team2_points pairing)]])
     (if bye
       [:td.result]
       [:td.result
        [:span.team1-wins (:team1_wins pairing)]
        [:span.hidden-mobile " - "]
        [:span.team2-wins (:team2_wins pairing)]])]))

(defn pairings [id round]
  (let [data (subscribe [::subs/sorted-pairings id round])
        sort-key (subscribe [::subs/pairings-sort])]
    (fn pairings-render [id round]
      (when (seq @data)
        [:table.pairings-table
         {:class (when (= @sort-key :team1_name) :player-sorted)}
         [:thead
          [:tr
           [sortable-header {:class        :table
                             :column       :table_number
                             :sort-key     @sort-key
                             :dispatch-key ::events/sort-pairings}
            "Pöytä"]
           [sortable-header {:class        :players
                             :column       :team1_name
                             :sort-key     @sort-key
                             :dispatch-key ::events/sort-pairings}
            ^{:key "player-1-heading"} [:span.hidden-mobile "Pelaaja 1"]
            ^{:key "players-heading"} [:span.hidden-desktop "Pelaajat"]]
           [:th.players2.hidden-mobile "Pelaaja 2"]
           [:th.points "Pist."]
           [:th.result "Tulos"]]]
         [:tbody
          (for [pairing @data]
            ^{:key [(:team1_name pairing)]}
            [pairing-row pairing])]]))))

(defn percentage [n]
  (gstring/format "%.3f" (* 100 n)))

(defn standing-row [standing]
  [:tr
   [:td.rank (:rank standing)]
   [:td.player (:team_name standing)]
   [:td.points (:points standing)]
   [:td.omw (percentage (:omw standing))]
   [:td.ogw (percentage (:pgw standing))]
   [:td.pgw (percentage (:ogw standing))]])

(defn standing-table [data]
  (when (seq data)
    [:table.standings-table
     [:thead
      [:tr
       [:th.rank]
       [:th.players "Pelaaja"]
       [:th.points "Pist."]
       [:th.omw "OMW"]
       [:th.ogw "PGW"]
       [:th.pgw "OGW"]]]
     [:tbody
      (for [standing data]
        ^{:key (:team_name standing)}
        [standing-row standing])]]))

(defn standings [id round]
  (let [data (subscribe [::subs/standings id round])]
    (fn standings-render [id round]
      [standing-table @data])))

(defn pod-row [seat]
  [:tr
   [:td.pod (:pod seat)]
   [:td.seat (:seat seat)]
   [:td.player (:team_name seat)]])

(defn pods [id round]
  (let [data (subscribe [::subs/sorted-pods id round])
        sort-key (subscribe [::subs/pods-sort])]
    (fn pods-render [id round]
      (when (seq @data)
        [:table.pods-table
         [:thead
          [:tr
           [sortable-header {:class        :pod
                             :column       :pod
                             :sort-key     @sort-key
                             :dispatch-key ::events/sort-pods}
            "Pöytä"]
           [:th.seat "Paikka"]
           [sortable-header {:class        :player
                             :column       :team_name
                             :sort-key     @sort-key
                             :dispatch-key ::events/sort-pods}
            "Pelaaja"]]]
         [:tbody
          (for [seat @data]
            ^{:key [(:team_name seat)]}
            [pod-row seat])]]))))

(defn seating-row [seat]
  [:tr
   [:td.table (:table_number seat)]
   [:td.player (:name seat)]])

(defn seatings [id round]
  (let [data (subscribe [::subs/sorted-seatings id round])
        sort-key (subscribe [::subs/seatings-sort])]
    (fn seatings-render [id round]
      (when (seq @data)
        [:table.seatings-table
         [:thead
          [:tr
           [sortable-header {:class        :table
                             :column       :table_number
                             :sort-key     @sort-key
                             :dispatch-key ::events/sort-seatings}
            "Pöytä"]
           [sortable-header {:class        :player
                             :column       :name
                             :sort-key     @sort-key
                             :dispatch-key ::events/sort-seatings}
            "Pelaaja"]]]
         [:tbody
          (for [seat @data]
            ^{:key [(:name seat)]}
            [seating-row seat])]]))))

(defn bracket-match [match]
  [:div.bracket-match
   [:div.team.team1
    {:class (when (> (:team1_wins match) (:team2_wins match))
              :winner)}
    (when (:team1_rank match)
      [:span.rank (str \( (:team1_rank match) \))])
    [:span.name (:team1_name match)]
    [:span.wins (:team1_wins match)]]
   [:div.team.team2
    {:class (when (< (:team1_wins match) (:team2_wins match))
              :winner)}
    (when (:team2_rank match)
      [:span.rank (str \( (:team2_rank match) \))])
    [:span.name (:team2_name match)]
    [:span.wins (:team2_wins match)]]])

(defn bracket [id]
  (let [data (subscribe [::subs/bracket id])]
    (fn bracket-render [id]
      [:div.bracket
       (for [round @data
             :let [num-matches (count round)
                   k (str id "-bracket-" num-matches)]]
         ^{:key k}
         [:div.bracket-round
          {:class (str "matches-" num-matches)}
          [:h3.hidden-desktop
           (str "Top " (* 2 num-matches))]
          (for [match round]
            ^{:key (str k "-table-" (:table_number match))}
            [bracket-match match])])])))
