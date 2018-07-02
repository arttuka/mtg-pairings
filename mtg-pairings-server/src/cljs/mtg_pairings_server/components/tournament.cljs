(ns mtg-pairings-server.components.tournament
  (:require [re-frame.core :refer [subscribe dispatch]]
            [goog.string :as gstring]
            [goog.string.format]
            [mtg-pairings-server.events :as events]
            [mtg-pairings-server.util.util :refer [format-date indexed]]
            [mtg-pairings-server.routes :refer [tournament-path pairings-path standings-path pods-path seatings-path]]
            [mtg-pairings-server.components.paging :refer [with-paging]]))

(defn tournament-header [id]
  (let [tournament (subscribe [:tournament id])]
    (fn tournament-header-render [id]
      [:h3 [:a {:href (tournament-path {:id id})}
            (str (:name @tournament) " " (format-date (:day @tournament)) " — " (:organizer @tournament))]])))

(defn tournament [data]
  (when data
    [:div.tournament
     [tournament-header (:id data) (:name data) (:day data) (:organizer data)]
     (for [r (:round-nums data)]
       ^{:key [(:id data) r]}
       [:div.tournament-row
        (when (contains? (:pairings data) r)
          [:a.btn.btn-default
           {:href (pairings-path {:id (:id data), :round r})}
           (str "Pairings " r)])
        (when (contains? (:standings data) r)
          [:a.btn.btn-default
           {:href (standings-path {:id (:id data), :round r})}
           (str "Standings " r)])])
     [:div.tournament-row
      (when (:seatings data)
        [:a.btn.btn-default
         {:href (seatings-path {:id (:id data)})}
         "Seatings"])
      (for [n (:pods data)]
        ^{:key [(:id data) :pods n]}
        [:a.btn.btn-default
         {:href (pods-path {:id (:id data), :round n})}
         (str "Pods " n)])]]))

(defn tournament-list []
  [with-paging ::events/tournaments-page [:tournaments-page] [:tournaments]
   (fn tournament-list-render [tournaments]
     [:div#tournaments
      (for [t tournaments]
        ^{:key (:id t)}
        [tournament t])])])

(defn sortable [column sort-key dispatch-key]
  {:class    (when (not= column sort-key) "inactive")
   :on-click #(dispatch [dispatch-key column])})

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
  (let [data (subscribe [:sorted-pairings id round])
        sort-key (subscribe [:pairings-sort])]
    (fn pairings-render [id round]
      [:table.pairings-table
       [:thead
        [:tr
         [:th.table (sortable :table_number @sort-key ::events/sort-pairings)
          [:i.glyphicon.glyphicon-chevron-down.left]
          "Pöytä"]
         [:th.players (sortable :team1_name @sort-key ::events/sort-pairings)
          [:i.glyphicon.glyphicon-chevron-down.left]
          [:span.hidden-xs "Pelaaja 1"]
          [:span.hidden-sm.hidden-md.hidden-lg "Pelaajat"]]
         [:th.players2.hidden-xs "Pelaaja 2"]
         [:th.points "Pisteet"]
         [:th.result "Tulos"]]]
       [:tbody
        (for [[i pairing] (indexed @data)]
          ^{:key [(:team1_name pairing)]}
          [pairing-row (if (even? i) "even" "odd") pairing])]])))

(defn standing-row [cls standing]
  [:tr {:class cls}
   [:td.rank (:rank standing)]
   [:td.player (:team_name standing)]
   [:td.points (:points standing)]
   [:td.omw (gstring/format "%.3f" (:omw standing))]
   [:td.ogw (gstring/format "%.3f" (:pgw standing))]
   [:td.pgw (gstring/format "%.3f" (:ogw standing))]])

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
  (let [data (subscribe [:standings id round])]
    (fn standings-render [id round]
      [standing-table @data])))

(defn pod-row [cls seat]
  [:tr {:class cls}
   [:td.pod (:pod seat)]
   [:td.seat (:seat seat)]
   [:td.player (:team_name seat)]])

(defn pods [id round]
  (let [data (subscribe [:sorted-pods id round])
        sort-key (subscribe [:pods-sort])]
    (fn pods-render [id round]
      [:table.pods-table
       [:thead
        [:tr
         [:th.pod (sortable :pod @sort-key ::events/sort-pods)
          [:i.glyphicon.glyphicon-chevron-down.left]
          "Pöytä"]
         [:th.seat "Paikka"]
         [:th.player (sortable :team_name @sort-key ::events/sort-pods)
          [:i.glyphicon.glyphicon-chevron-down.left]
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
  (let [data (subscribe [:sorted-seatings id round])
        sort-key (subscribe [:seatings-sort])]
    (fn seatings-render [id round]
      [:table.pairings-table
       [:thead
        [:tr
         [:th.table (sortable :table_number @sort-key ::events/sort-seatings)
          [:i.glyphicon.glyphicon-chevron-down.left]
          "Pöytä"]
         [:th.players (sortable :name @sort-key ::events/sort-seatings)
          [:i.glyphicon.glyphicon-chevron-down.left]
          "Pelaaja"]]]
       [:tbody
        (for [[i seat] (indexed @data)]
          ^{:key [(:name seat)]}
          [seating-row (if (even? i) "even" "odd") seat])]])))
