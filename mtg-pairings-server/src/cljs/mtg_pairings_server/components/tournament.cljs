(ns mtg-pairings-server.components.tournament
  (:require [re-frame.core :refer [subscribe]]
            [mtg-pairings-server.util.util :refer [format-date]]))

(defn tournament [data]
  [:div.tournament
   [:h3 (str (:name data) " " (format-date (:day data)) " — " (:organizer data))]
   (for [r (:round-nums data)]
     ^{:key [(:id data) r]}
     [:div.tournament-row
      (when (contains? (:pairings data) r)
        [:a.btn.btn-default
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
       (str "Pods " n)])]])

(defn tournament-list []
  (let [tournaments (subscribe [:tournaments])]
    (fn []
      [:div#tournaments
       (for [t @tournaments]
         ^{:key (:id t)}
         [tournament t])])))
