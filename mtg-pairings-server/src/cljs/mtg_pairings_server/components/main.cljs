(ns mtg-pairings-server.components.main
  (:require [reagent.core :as reagent :refer [atom]]
            [re-frame.core :refer [subscribe dispatch]]
            [reagent-material-ui.components :as ui]
            [reagent-material-ui.icons.list :refer [list] :rename {list list-icon}]
            [reagent-material-ui.icons.menu :refer [menu] :rename {menu menu-icon}]
            [oops.core :refer [oget]]
            [accountant.core :as accountant]
            [mtg-pairings-server.components.pairings.tournament :refer [tournament-header]]
            [mtg-pairings-server.events.pairings :as events]
            [mtg-pairings-server.routes.pairings :refer [tournaments-path standings-path]]
            [mtg-pairings-server.subscriptions.pairings :as subs]
            [mtg-pairings-server.util :refer [format-date indexed]]
            [mtg-pairings-server.util.material-ui :refer [wrap-on-change]]
            [mtg-pairings-server.util.mtg :refer [bye?]]))

(defn pairing [data pairing?]
  (let [bye (bye? data)]
    [ui/list-item
     [ui/list-item-icon
      [ui/avatar
       (when-not bye
         (or (:table_number data) (:pod data)))]]
     [ui/list-item-text {:class     :mui-pairing
                         :primary   (if pairing?
                                      (str "Kierros " (:round_number data))
                                      (if (:pod data)
                                        (str "Pod " (:pod data))
                                        "Seating"))
                         :secondary (reagent/as-element
                                     (if pairing?
                                       [:<>
                                        [:span.names
                                         [:span.MuiTypography-colorTextPrimary
                                          (str (:team1_name data) " (" (:team1_points data) ")")]
                                         [:span.hidden-mobile " - "]
                                         [:br.hidden-desktop]
                                         [:span (if bye
                                                  (:team2_name data)
                                                  (str (:team2_name data) " (" (:team2_points data) ")"))]]
                                        (when-not bye
                                          [:span.points
                                           [:span (:team1_wins data)]
                                           [:span.hidden-mobile " - "]
                                           [:br.hidden-desktop]
                                           [:span (:team2_wins data)]])]
                                       [:span.names (or (:team1_name data)
                                                        (str "Seat " (:seat data)))]))}]]))

(defn combine-pairings-and-pods [pairings pods]
  (->> (concat pairings pods)
       (sort-by (juxt :round_number :team1_name))
       (reverse)))

(defn own-tournament [t]
  (let [expanded? (atom false)
        on-expand #(swap! expanded? not)]
    (fn own-tournament-render [t]
      [ui/card
       [tournament-header {:data      t
                           :on-expand on-expand
                           :expanded? @expanded?}]
       [ui/collapse {:in @expanded?}
        [ui/card-content
         {:style {:padding-top 0}}
         [ui/list
          [ui/list-item {:button   true
                         :on-click #(accountant/navigate! (standings-path {:id (:id t), :round (:max_standings_round t)}))}
           [ui/list-item-icon
            [list-icon]]
           [ui/list-item-text {:primary (str "Standings, kierros " (:max_standings_round t))}]]
          (for [p (combine-pairings-and-pods (:pairings t) (:pod-seats t))]
            ^{:key [(:id t) (:round_number p) (:id p)]}
            [pairing p (boolean (:team1_name p))])
          (when (:seating t)
            [pairing (:seating t) false])]]]])))

(defn notification []
  (let [text (subscribe [::subs/notification])]
    (fn notification-render []
      [ui/snackbar {:open               (boolean @text)
                    :message            (or @text "")
                    :auto-hide-duration 5000
                    :on-request-close   #(dispatch [::events/notification nil])}])))
