(ns mtg-pairings-server.components.main
  (:require [reagent.core :as reagent :refer [atom]]
            [re-frame.core :refer [subscribe dispatch]]
            [cljsjs.material-ui]
            [cljs-react-material-ui.core :refer [get-mui-theme]]
            [cljs-react-material-ui.reagent :as ui]
            [cljs-react-material-ui.icons :as icons]
            [oops.core :refer [oget]]
            [accountant.core :as accountant]
            [mtg-pairings-server.events :as events]
            [mtg-pairings-server.routes :refer [tournaments-path standings-path]]
            [mtg-pairings-server.subscriptions :as subs]
            [mtg-pairings-server.util.util :refer [format-date indexed]]))

(defn pairing [data pairing?]
  [ui/list-item
   {:class                :mui-pairing
    :left-avatar          (reagent/as-element [ui/avatar
                                               {:background-color (oget (get-mui-theme) "palette" "primary1Color")}
                                               (or (:table_number data) (:pod data))])
    :primary-text         (if pairing?
                            (str "Kierros " (:round_number data))
                            (if (:pod data) "Pod" "Seating"))
    :secondary-text       (reagent/as-element
                            (if pairing?
                              [:div
                               [:div.names
                                [:span {:style {:color "rgba(0, 0, 0, 0.87)"}}
                                 (str (:team1_name data) " (" (:team1_points data) ")")]
                                [:span.hidden-xs " - "]
                                [:br.hidden-sm.hidden-md.hidden-lg]
                                [:span (str (:team2_name data) " (" (:team2_points data) ")")]]
                               [:div.points
                                [:span (:team1_wins data)]
                                [:span.hidden-xs " - "]
                                [:br.hidden-sm.hidden-md.hidden-lg]
                                [:span (:team2_wins data)]]]
                              [:div.names (or (:team1_name data)
                                              (str "Seat " (:seat data)))]))
    :secondary-text-lines 2}])

(defn header []
  (let [user (subscribe [::subs/logged-in-user])
        dci-number (atom "")]
    (fn header-render []
      [ui/app-bar
       {:id                 :header
        :title              (when @user (:name @user))
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
                                              :on-click     #(accountant/navigate! "/")}]
                               [ui/menu-item {:primary-text "Turnausarkisto"
                                              :on-click     #(accountant/navigate! (tournaments-path))}]
                               (when @user
                                 [ui/menu-item {:primary-text "Kirjaudu ulos"
                                                :on-click     #(dispatch [::events/logout])}])])}])))

(defn combine-pairings-and-pods [pairings pods]
  (->> (concat pairings pods)
       (sort-by (juxt :round_number :team1_name))
       (reverse)))

(defn own-tournament [t]
  (let [primary (oget (get-mui-theme) "palette" "primary1Color")]
    (fn own-tournament-render [t]
      [ui/card
       [ui/card-header
        {:title                  (str (format-date (:day t)) " - " (:name t))
         :act-as-expander        true
         :show-expandable-button true}]
       [ui/card-text
        {:expandable true}
        [ui/list
         [ui/list-item
          {:primary-text (str "Standings, kierros " (:max_standings_round t))
           :style        {:color primary}
           :left-avatar  (reagent/as-element [ui/avatar
                                              {:background-color primary
                                               :icon             (reagent/as-element [icons/action-list])}])
           :on-click     #(accountant/navigate! (standings-path {:id (:id t), :round (:max_standings_round t)}))}]
         (for [p (combine-pairings-and-pods (:pairings t) (:pod-seats t))]
           ^{:key [(:id t) (:round_number p) (:id p)]}
           [pairing p (boolean (:team1_name p))])
         (when (:seating t)
           [pairing (:seating t) false])]]])))
