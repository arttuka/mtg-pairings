(ns mtg-pairings-server.pages.pairings
  (:require [re-frame.core :refer [subscribe]]
            [cljsjs.material-ui]
            [cljs-react-material-ui.reagent :as ui]
            [mtg-pairings-server.components.main :refer [own-tournament pairing]]
            [mtg-pairings-server.components.tournament :refer [newest-tournaments-list tournament-list tournament-card-header tournament pairings standings pods seatings bracket]]
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
      (if @user
        [:div#own-tournaments
         (if (seq @player-tournaments)
           (let [latest-pairing (get-latest-pairing @player-tournaments)]
             [ui/card
              [ui/card-header
               {:title    "Uusin pairing"
                :subtitle (:tournament latest-pairing)
                :style    {:padding-bottom 0}}]
              [ui/card-text
               {:style {:padding-top    0
                        :padding-bottom 0}}
               [pairing latest-pairing (some? (:team2_name latest-pairing))]]])
           [ui/circular-progress {:style     {:margin  "48px auto 0"
                                              :display :block}
                                  :size      100
                                  :thickness 5}])
         (for [t @player-tournaments]
           ^{:key [:tournament (:id t)]}
           [own-tournament t])]
        [newest-tournaments-list]))))

(defn tournaments-page []
  [tournament-list])

(defn tournament-page [id]
  (let [data (subscribe [::subs/tournament id])]
    (fn tournament-page-render [id]
      [tournament @data])))

(defn tournament-subpage [id type round]
  (let [tournament (subscribe [::subs/tournament id])]
    (fn tournament-subpage-render [id type round]
      [ui/card
       [tournament-card-header @tournament]
       [ui/card-text
        {:style {:padding-top 0}}
        (case type
          ::pairings [pairings id round]
          ::standings [standings id round]
          ::pods [pods id round]
          ::seatings [seatings id]
          ::bracket [bracket id])]])))
