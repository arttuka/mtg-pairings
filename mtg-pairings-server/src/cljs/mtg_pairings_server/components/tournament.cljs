(ns mtg-pairings-server.components.tournament
  (:require [reagent.core :as reagent]
            [re-frame.core :refer [subscribe dispatch]]
            [cljsjs.material-ui]
            [cljs-react-material-ui.core]
            [cljs-react-material-ui.reagent :as ui]
            [goog.string :as gstring]
            [goog.string.format]
            [prop-types]
            [mtg-pairings-server.events :as events]
            [mtg-pairings-server.subscriptions :as subs]
            [mtg-pairings-server.util.util :refer [format-date indexed]]
            [mtg-pairings-server.routes :refer [tournaments-path tournament-path pairings-path standings-path pods-path seatings-path bracket-path]]
            [mtg-pairings-server.components.paging :refer [with-paging]]
            [mtg-pairings-server.components.filter :refer [tournament-filters]]
            [mtg-pairings-server.material-ui.util :refer [get-theme]]))

(defn tournament-header [id]
  (let [tournament (subscribe [::subs/tournament id])]
    (fn tournament-header-render [id]
      [:h3 [:a {:href (tournament-path {:id id})}
            (str (:name @tournament) " " (format-date (:day @tournament)) " — " (:organizer @tournament))]])))

(defn tournament [data]
  (when data
    [ui/paper
     {:style {:margin  "10px"
              :padding "10px"}}
     [tournament-header (:id data) (:name data) (:day data) (:organizer data)]
     (when (:playoff data)
       [:div.tournament-row
        [ui/raised-button
         {:label "Playoff bracket"
          :href  (bracket-path {:id (:id data)})
          :style {:width "260px"}}]])
     (for [r (:round-nums data)]
       ^{:key [(:id data) r]}
       [:div.tournament-row
        (when (contains? (:pairings data) r)
          [ui/raised-button
           {:label (str "Pairings " r)
            :href  (pairings-path {:id (:id data), :round r})
            :style {:width "130px"}}])
        (when (contains? (:standings data) r)
          [ui/raised-button
           {:label (str "Standings " r)
            :href  (standings-path {:id (:id data), :round r})
            :style {:width "130px"}}])])
     [:div.tournament-row
      (when (:seatings data)
        [ui/raised-button
         {:label "Seatings"
          :href  (seatings-path {:id (:id data)})
          :style {:width "130px"}}])
      (for [n (:pods data)]
        ^{:key [(:id data) :pods n]}
        [ui/raised-button
         {:label (str "Pods " n)
          :href  (pods-path {:id (:id data), :round n})
          :style {:width "130px"}}])]]))

(defn newest-tournaments-list []
  (let [tournaments (subscribe [::subs/newest-tournaments])]
    (fn newest-tournaments-list-render []
      [:div#tournaments
       [:h2 "Aktiiviset turnaukset | " [:a {:href (tournaments-path)}
                                        "Turnausarkistoon"]]
       (if-let [ts (seq @tournaments)]
         (for [t ts]
           ^{:key (:id t)}
           [tournament t])
         [:h3 "Ei aktiivisia turnauksia."])])))

(defn tournament-list []
  [:div
   [tournament-filters]
   [with-paging ::events/tournaments-page [::subs/tournaments-page] [::subs/filtered-tournaments]
    (fn tournament-list-render [tournaments]
      [:div#tournaments
       (for [t tournaments]
         ^{:key (:id t)}
         [tournament t])])]])

(defn sortable-header [{:keys [class column sort-key dispatch-key]} & children]
  (reagent/create-class
   {:context-types  #js {:muiTheme prop-types/object.isRequired}
    :reagent-render (fn sortable-header-render [{:keys [class column sort-key dispatch-key]} & children]
                      (let [palette (:palette (get-theme (reagent/current-component)))]
                        [:th {:class    class
                              :style    (when (= column sort-key) {:color (:accent1Color palette)})
                              :on-click #(dispatch [dispatch-key column])}
                         [:i.glyphicon.glyphicon-chevron-down.left]
                         children]))}))

(defn pairing-row [cls pairing]
  [:tr {:class cls}
   [:td.table (:table_number pairing)]
   [:td.players
    (:team1_name pairing)
    [:br.hidden-sm.hidden-md.hidden-lg]
    [:span.hidden-sm.hidden-md.hidden-lg
     (:team2_name pairing)]]
   [:td.players2.hidden-xs (:team2_name pairing)]
   [:td.points
    (:team1_points pairing)
    [:span.hidden-xs " - "]
    [:br.hidden-sm.hidden-md.hidden-lg]
    [:span (:team2_points pairing)]]
   [:td.result
    (:team1_wins pairing)
    [:span.hidden-xs " - "]
    [:br.hidden-sm.hidden-md.hidden-lg]
    [:span (:team2_wins pairing)]]])

(defn pairings [id round]
  (let [data (subscribe [::subs/sorted-pairings id round])
        sort-key (subscribe [::subs/pairings-sort])]
    (fn pairings-render [id round]
      [:table.pairings-table
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
          ^{:key "player-1-heading"} [:span.hidden-xs "Pelaaja 1"]
          ^{:key "players-heading"} [:span.hidden-sm.hidden-md.hidden-lg "Pelaajat"]]
         [:th.players2.hidden-xs "Pelaaja 2"]
         [:th.points "Pisteet"]
         [:th.result "Tulos"]]]
       [:tbody
        (for [[i pairing] (indexed @data)]
          ^{:key [(:team1_name pairing)]}
          [pairing-row (if (even? i) "even" "odd") pairing])]])))

(defn percentage [n]
  (gstring/format "%.3f" (* 100 n)))

(defn standing-row [cls standing]
  [:tr {:class cls}
   [:td.rank (:rank standing)]
   [:td.player (:team_name standing)]
   [:td.points (:points standing)]
   [:td.omw (percentage (:omw standing))]
   [:td.ogw (percentage (:pgw standing))]
   [:td.pgw (percentage (:ogw standing))]])

(defn standing-table [data]
  [:table.standings-table
   [:thead
    [:tr
     [:th.rank "Sija"]
     [:th.players "Pelaaja"]
     [:th.points "Pisteet"]
     [:th.omw "OMW"]
     [:th.ogw "PGW"]
     [:th.pgw "OGW"]]]
   [:tbody
    (for [[i standing] (indexed data)]
      ^{:key (:team_name standing)}
      [standing-row (if (even? i) "even" "odd") standing])]])

(defn standings [id round]
  (let [data (subscribe [::subs/standings id round])]
    (fn standings-render [id round]
      [standing-table @data])))

(defn pod-row [cls seat]
  [:tr {:class cls}
   [:td.pod (:pod seat)]
   [:td.seat (:seat seat)]
   [:td.player (:team_name seat)]])

(defn pods [id round]
  (let [data (subscribe [::subs/sorted-pods id round])
        sort-key (subscribe [::subs/pods-sort])]
    (fn pods-render [id round]
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
        (for [[i seat] (indexed @data)]
          ^{:key [(:team_name seat)]}
          [pod-row (if (even? i) "even" "odd") seat])]])))

(defn seating-row [cls seat]
  [:tr {:class cls}
   [:td.table (:table_number seat)]
   [:td.players (:name seat)]])

(defn seatings [id round]
  (let [data (subscribe [::subs/sorted-seatings id round])
        sort-key (subscribe [::subs/seatings-sort])]
    (fn seatings-render [id round]
      [:table.pairings-table
       [:thead
        [:tr
         [sortable-header {:class        :table
                           :column       :table_number
                           :sort-key     @sort-key
                           :dispatch-key ::events/sort-seatings}
          "Pöytä"]
         [sortable-header {:class        :players
                           :column       :name
                           :sort-key     @sort-key
                           :dispatch-key ::events/sort-seatings}
          "Pelaaja"]]]
       [:tbody
        (for [[i seat] (indexed @data)]
          ^{:key [(:name seat)]}
          [seating-row (if (even? i) "even" "odd") seat])]])))

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
          [:h3.hidden-sm.hidden-md.hidden-lg
           (str "Top " (* 2 num-matches))]
          (for [match round]
            ^{:key (str k "-table-" (:table_number match))}
            [bracket-match match])])])))
