(ns mtg-pairings-server.pages.pairings
  (:require [re-frame.core :refer [subscribe]]
            [reagent.core :as reagent :refer [with-let]]
            [reagent-material-ui.components :as ui]
            [reagent-material-ui.styles :refer [with-styles]]
            [mtg-pairings-server.components.pairings.bracket :refer [bracket]]
            [mtg-pairings-server.components.pairings.pairings-table :refer [pairings-table]]
            [mtg-pairings-server.components.pairings.player :refer [own-tournament pairing]]
            [mtg-pairings-server.components.pairings.pods-table :refer [pods-table]]
            [mtg-pairings-server.components.pairings.seatings-table :refer [seatings-table]]
            [mtg-pairings-server.components.pairings.standings-table :refer [standings-table]]
            [mtg-pairings-server.components.pairings.tournament :refer [tournament tournament-header]]
            [mtg-pairings-server.components.pairings.tournament-list :refer [newest-tournaments-list tournament-list]]
            [mtg-pairings-server.subscriptions.pairings :as subs]
            [mtg-pairings-server.util :refer [format-time-only]]
            [mtg-pairings-server.util.styles :refer [on-desktop on-mobile]]))

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
  (with-let [user (subscribe [::subs/logged-in-user])
             player-tournaments (subscribe [::subs/player-tournaments])
             translate (subscribe [::subs/translate])]
    (cond
      (not @user)
      [newest-tournaments-list]

      (seq @player-tournaments)
      [ui/list
       (let [latest-pairing (get-latest-pairing @player-tournaments)]
         [:<>
          [ui/list-item
           [ui/list-item-text {:primary                  (@translate :front-page.newest-pairing)
                               :secondary                (:tournament latest-pairing)
                               :primary-typography-props {:variant "h5"}}]]
          [pairing {:data      latest-pairing
                    :divider   true
                    :translate @translate}]])
       (for [t @player-tournaments]
         ^{:key [:tournament (:id t)]}
         [own-tournament {:tournament t}])]

      :else
      [ui/circular-progress {:style     {:margin  "48px auto 0"
                                         :display :block}
                             :size      100
                             :thickness 5}])))

(defn tournaments-page []
  [tournament-list])

(defn tournament-page [id]
  (let [data (subscribe [::subs/tournament id])]
    (fn tournament-page-render [id]
      [tournament {:data @data}])))

(defn tournament-subpage-styles [{:keys [spacing]}]
  {:root   {on-desktop {:width (+ 820 (spacing 2))}}
   :mobile {on-desktop {:display :none}}})

(defn tournament-subpage* [{:keys [classes tournament-id type round]}]
  (with-let [tournament (subscribe [::subs/tournament tournament-id])
             translate (subscribe [::subs/translate])]
    (let [round-title (@translate (str "common." (name type)) round)
          round-started (when (= "pairings" type)
                          (when-let [start-time (get-in @tournament [:round-times round])]
                            (@translate :common.started (format-time-only start-time))))]
      [ui/card {:classes (dissoc classes :mobile)}
       [tournament-header {:data       @tournament
                           :round-data {:title   round-title
                                        :started round-started}}]
       [ui/card-content
        {:style {:padding-top 0}}
        [ui/typography {:variant :h5
                        :classes {:root (:mobile classes)}}
         round-title]
        (when round-started
          [ui/typography {:variant :body1
                          :color   :textSecondary
                          :classes {:root (:mobile classes)}}
           round-started])
        (case type
          "pairings" [pairings-table {:tournament-id tournament-id
                                      :round         round}]
          "standings" [standings-table {:tournament-id tournament-id
                                        :round         round}]
          "pods" [pods-table {:tournament-id tournament-id
                              :round         round}]
          "seatings" [seatings-table {:tournament-id tournament-id}]
          "bracket" [bracket {:tournament-id tournament-id}])]])))

(def tournament-subpage ((with-styles tournament-subpage-styles) tournament-subpage*))
