(ns mtg-pairings-server.pages.main
  (:require [re-frame.core :refer [subscribe]]
            [cljsjs.material-ui]
            [cljs-react-material-ui.reagent :as ui]
            [mtg-pairings-server.components.main :refer [own-tournament pairing]]
            [mtg-pairings-server.components.tournament :refer [newest-tournaments-list]]
            [mtg-pairings-server.subscriptions :as subs]))

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
