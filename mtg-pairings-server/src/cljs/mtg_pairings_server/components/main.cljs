(ns mtg-pairings-server.components.main
  (:require [reagent.core :as reagent :refer [atom]]
            [re-frame.core :refer [subscribe dispatch]]
            [cljsjs.material-ui]
            [cljs-react-material-ui.reagent :as ui]
            [cljs-react-material-ui.icons :as icons]
            [accountant.core :as accountant]
            [mtg-pairings-server.components.organizer :refer [pairing]]
            [mtg-pairings-server.events :as events]
            [mtg-pairings-server.routes :refer [tournaments-path standings-path]]
            [mtg-pairings-server.subscriptions :as subs]
            [mtg-pairings-server.util.util :refer [format-date indexed]]))

(defn header []
  (let [user (subscribe [::subs/logged-in-user])
        dci-number (atom "")]
    (fn header-render []
      [ui/app-bar
       {:title              (when @user (:name @user))
        :icon-element-right (when-not @user
                              (reagent/as-element [:div
                                                   [ui/text-field
                                                    {:hint-text   "DCI-numero"
                                                     :hint-style  {:color :white}
                                                     :input-style {:color :white}
                                                     :value       @dci-number
                                                     :on-change   (fn [_ new-value]
                                                                    (reset! dci-number new-value))}]
                                                   [ui/flat-button
                                                    {:label       "Kirjaudu"
                                                     :label-style {:color :white}
                                                     :on-click    #(do (dispatch [::events/login @dci-number])
                                                                       (reset! dci-number ""))}]]))
        :icon-element-left  (reagent/as-element
                              [ui/icon-menu
                               {:icon-button-element (reagent/as-element
                                                       [ui/icon-button
                                                        [icons/navigation-menu {:color :white}]])}
                               [ui/menu-item {:primary-text "Etusivu"
                                              :on-click #(accountant/navigate! "/")}]
                               [ui/menu-item {:primary-text "Turnausarkisto"
                                              :on-click #(accountant/navigate! (tournaments-path))}]
                               (when @user
                                 [ui/menu-item {:primary-text "Kirjaudu ulos"
                                                :on-click     #(dispatch [::events/logout])}])])}])))

(defn combine-pairings-and-pods [pairings pods]
  (->> (concat pairings pods)
       (sort-by (juxt :round_number :team1_name))
       (reverse)))

(defn own-tournament [t]
  (let [collapsed? (atom true)]
    (fn own-tournament-render [t]
      [:div.own-tournament.border-top
       [:div.own-tournament-header
        {:on-click #(swap! collapsed? not)}
        [:h3
         [:i.pull-left.glyphicon
          {:class (if @collapsed? "glyphicon-chevron-right" "glyphicon-chevron-down")}]
         [:span.date (format-date (:day t)) " - "]
         (:name t)]]
       [:div.own-tournament-content
        {:class (when @collapsed? "hidden")}
        [:a {:href (standings-path {:id (:id t), :round (:max_standings_round t)})}
         (str "Standings, kierros " (:max_standings_round t))
         [:i.pull-right.glyphicon.glyphicon-chevron-right]]
        (for [[i p] (indexed (combine-pairings-and-pods (:pairings t) (:pod-seats t)))]
          ^{:key [(:id t) (:round_number p) (:id p)]}
          [:div.own-tournament-pairing
           [pairing p (even? i) true (boolean (:team1_name p))]])
        (when (:seating t)
          [:div.own-tournament-pairing
           [pairing (:seating t) (even? (+ (count (:pairings t))
                                           (count (:pod-seats t))))
            true false]])]])))
