(ns mtg-pairings-server.components.tournament
  (:require [re-frame.core :refer [subscribe dispatch]]
            [mtg-pairings-server.util.util :refer [format-date indexed]]
            [mtg-pairings-server.routes :refer [tournament-path pairings-path]]))

(defn tournament-header [id name day organizer]
  [:h3 [:a {:href (tournament-path id)}
        (str name " " (format-date day) " — " organizer)]])

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
           (str "Standings " r)])])
     [:div.tournament-row
      (when (:seatings data)
        [:a.btn.btn-default "Seatings"])
      (for [n (:pods data)]
        ^{:key [(:id data) :pods n]}
        [:a.btn.btn-default
         (str "Pods " n)])]]))

(defn tournament-list []
  (let [tournaments (subscribe [:tournaments])]
    (fn []
      [:div#tournaments
       (for [t @tournaments]
         ^{:key (:id t)}
         [tournament t])])))

(defn sortable [column sort-key]
  {:class (when (not= column sort-key) "inactive")
   :on-click #(dispatch [:sort-pairings column])})

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
    (fn [id round]
      [:table.pairings-table
       [:thead
        [:tr
         [:th.table (sortable :table_number @sort-key)
          [:i.glyphicon.glyphicon-chevron-down.left]
          "Pöytä"]
         [:th.players (sortable :team1_name @sort-key)
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
