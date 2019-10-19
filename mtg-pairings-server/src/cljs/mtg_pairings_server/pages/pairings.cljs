(ns mtg-pairings-server.pages.pairings
  (:require [re-frame.core :refer [subscribe]]
            [reagent-material-ui.components :as ui]
            [mtg-pairings-server.components.pairings.bracket :refer [bracket]]
            [mtg-pairings-server.components.pairings.pairings-table :refer [pairings-table]]
            [mtg-pairings-server.components.pairings.player :refer [own-tournament pairing]]
            [mtg-pairings-server.components.pairings.pods-table :refer [pods-table]]
            [mtg-pairings-server.components.pairings.seatings-table :refer [seatings-table]]
            [mtg-pairings-server.components.pairings.standings-table :refer [standings-table]]
            [mtg-pairings-server.components.pairings.tournament :refer [tournament tournament-header]]
            [mtg-pairings-server.components.pairings.tournament-list :refer [newest-tournaments-list tournament-list]]
            [mtg-pairings-server.subscriptions.pairings :as subs]))

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
  (let [user (subscribe [::subs/logged-in-user])
        player-tournaments (subscribe [::subs/player-tournaments])]
    (fn main-page-render []
      (cond
        (not @user)
        [newest-tournaments-list]

        (seq @player-tournaments)
        [ui/list
         (let [latest-pairing (get-latest-pairing @player-tournaments)]
           [:<>
            [ui/list-item
             [ui/list-item-text {:primary                  "Uusin pairing"
                                 :secondary                (:tournament latest-pairing)
                                 :primary-typography-props {:variant "h5"}}]]
            [pairing {:data    latest-pairing
                      :divider true}]])
         (for [t @player-tournaments]
           ^{:key [:tournament (:id t)]}
           [own-tournament {:tournament t}])]

        :else
        [ui/circular-progress {:style     {:margin  "48px auto 0"
                                           :display :block}
                               :size      100
                               :thickness 5}]))))

(defn tournaments-page []
  [tournament-list])

(defn tournament-page [id]
  (let [data (subscribe [::subs/tournament id])]
    (fn tournament-page-render [id]
      [tournament {:data @data}])))

(defn tournament-subpage [id type round]
  (let [tournament (subscribe [::subs/tournament id])]
    (fn tournament-subpage-render [id type round]
      [ui/card
       [tournament-header {:data @tournament}]
       [ui/card-content
        {:style {:padding-top 0}}
        (case type
          ::pairings [pairings-table {:tournament-id id
                                      :round         round}]
          ::standings [standings-table {:tournament-id id
                                        :round         round}]
          ::pods [pods-table {:tournament-id id
                              :round         round}]
          ::seatings [seatings-table {:tournament-id id}]
          ::bracket [bracket {:tournament-id id}])]])))
