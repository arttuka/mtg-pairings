(ns mtg-pairings-server.pages.main
  (:require [re-frame.core :refer [subscribe]]
            [mtg-pairings-server.components.main :refer [own-tournament]]
            [mtg-pairings-server.components.organizer :refer [pairing]]
            [mtg-pairings-server.components.tournament :refer [tournament-list]]))

(defn get-latest-pairing [player-tournaments]
  (let [t (first player-tournaments)
        pod-seat (first (:pod-seats t))
        pairing (first (:pairings t))
        seating (:seating t)
        selected (if (and pod-seat
                          (or (not pairing)
                              (> (:round_number pod-seat) (:round_number pairing))))
                   pod-seat
                   (or pairing seating))]
    (assoc selected
      :tournament (:name t)
      :day (:day t))))

(defn main-page []
  (let [user (subscribe [:logged-in-user])
        player-tournaments (subscribe [:player-tournaments])]
    (fn main-page-render []
      (if @user
        [:div
         (when (seq @player-tournaments)
           (let [latest-pairing (get-latest-pairing @player-tournaments)]
             [:div.newest-pairing
              [:h3 "Uusin pairing:"]
              [:span.newest-tournament-name (:tournament latest-pairing)]
              [pairing latest-pairing true true (some? (:team2_name latest-pairing))]]))
         (for [t @player-tournaments]
           ^{:key [:tournament (:id t)]}
           [own-tournament t])]
        [tournament-list]))))
