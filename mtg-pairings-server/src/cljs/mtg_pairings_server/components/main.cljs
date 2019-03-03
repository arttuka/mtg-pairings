(ns mtg-pairings-server.components.main
  (:require [reagent.core :as reagent :refer [atom]]
            [re-frame.core :refer [subscribe dispatch]]
            [cljsjs.material-ui]
            [cljs-react-material-ui.core]
            [cljs-react-material-ui.reagent :as ui]
            [cljs-react-material-ui.icons :as icons]
            [prop-types]
            [oops.core :refer [oget]]
            [accountant.core :as accountant]
            [mtg-pairings-server.events :as events]
            [mtg-pairings-server.routes :refer [tournaments-path standings-path]]
            [mtg-pairings-server.subscriptions :as subs]
            [mtg-pairings-server.util.util :refer [format-date indexed]]
            [mtg-pairings-server.material-ui.util :refer [get-theme]]))

(defn pairing [data pairing?]
  (reagent/create-class
   {:context-types  #js {:muiTheme prop-types/object.isRequired}
    :reagent-render (fn [data pairing?]
                      (let [palette (:palette (get-theme (reagent/current-component)))
                            bye? (= (:table_number data) 0)]
                        [ui/list-item
                         {:class                :mui-pairing
                          :left-avatar          (reagent/as-element [ui/avatar
                                                                     {:background-color (if bye?
                                                                                          (:accent3Color palette)
                                                                                          (:primary1Color palette))
                                                                      :color            (:textColor palette)}
                                                                     (when-not bye?
                                                                       (or (:table_number data) (:pod data)))])
                          :primary-text         (if pairing?
                                                  (str "Kierros " (:round_number data))
                                                  (if (:pod data)
                                                    (str "Pod " (:pod data))
                                                    "Seating"))
                          :secondary-text       (reagent/as-element
                                                 (if pairing?
                                                   [:div
                                                    [:span.names
                                                     [:span {:style {:color "rgba(0, 0, 0, 0.87)"}}
                                                      (str (:team1_name data) " (" (:team1_points data) ")")]
                                                     [:span.hidden-mobile " - "]
                                                     [:br.hidden-desktop]
                                                     [:span (if bye?
                                                              (:team2_name data)
                                                              (str (:team2_name data) " (" (:team2_points data) ")"))]]
                                                    (when-not bye?
                                                      [:span.points
                                                       [:span (:team1_wins data)]
                                                       [:span.hidden-mobile " - "]
                                                       [:br.hidden-desktop]
                                                       [:span (:team2_wins data)]])]
                                                   [:span.names (or (:team1_name data)
                                                                    (str "Seat " (:seat data)))]))
                          :secondary-text-lines 2}]))}))

(defn header []
  (let [user (subscribe [::subs/logged-in-user])
        dci-number (atom "")
        login! (fn []
                 (dispatch [::events/login @dci-number])
                 (reset! dci-number ""))]
    (reagent/create-class
     {:context-types  #js {:muiTheme prop-types/object.isRequired}
      :reagent-render (fn header-render []
                        (let [palette (:palette (get-theme (reagent/current-component)))]
                          [ui/app-bar
                           {:id                 :header
                            :title              (when @user (:name @user))
                            :title-style        {:color (:secondaryTextColor palette)}
                            :icon-element-right (when-not @user
                                                  (reagent/as-element [:div
                                                                       [ui/text-field
                                                                        {:floating-label-text        "DCI-numero"
                                                                         :value                      @dci-number
                                                                         :on-change                  (fn [_ new-value]
                                                                                                       (reset! dci-number new-value))
                                                                         :on-key-down                (fn [e]
                                                                                                       (when (= 13 (oget e "keyCode"))
                                                                                                         (login!)))
                                                                         :underline-focus-style      {:border-color (:accent3Color palette)}
                                                                         :floating-label-focus-style {:color (:accent3Color palette)}
                                                                         :style                      {:margin-top "-24px"
                                                                                                      :width      "160px"}}]
                                                                       [ui/raised-button
                                                                        {:label         "Kirjaudu"
                                                                         :on-click      login!
                                                                         :overlay-style {:background-color :white}
                                                                         :style         {:margin-left "12px"}}]]))
                            :icon-element-left  (reagent/as-element
                                                 [ui/icon-menu
                                                  {:icon-button-element (reagent/as-element
                                                                         [ui/icon-button
                                                                          [icons/navigation-menu]])}
                                                  [ui/menu-item {:primary-text "Etusivu"
                                                                 :on-click     #(accountant/navigate! "/")}]
                                                  [ui/menu-item {:primary-text "Turnausarkisto"
                                                                 :on-click     #(accountant/navigate! (tournaments-path))}]
                                                  (when @user
                                                    [ui/menu-item {:primary-text "Kirjaudu ulos"
                                                                   :on-click     #(dispatch [::events/logout])}])])}]))})))

(defn combine-pairings-and-pods [pairings pods]
  (->> (concat pairings pods)
       (sort-by (juxt :round_number :team1_name))
       (reverse)))

(defn own-tournament [t]
  (reagent/create-class
   {:context-types  #js {:muiTheme prop-types/object.isRequired}
    :reagent-render (fn own-tournament-render [t]
                      (let [palette (:palette (get-theme (reagent/current-component)))]
                        [ui/card
                         [ui/card-header
                          {:title                  (str (format-date (:day t)) " - " (:name t))
                           :act-as-expander        true
                           :show-expandable-button true}]
                         [ui/card-text
                          {:expandable true
                           :style      {:padding-top 0}}
                          [ui/list
                           [ui/list-item
                            {:primary-text (str "Standings, kierros " (:max_standings_round t))
                             :left-avatar  (reagent/as-element [ui/avatar
                                                                {:icon             (reagent/as-element [icons/action-list])
                                                                 :background-color (:primary1Color palette)}])
                             :on-click     #(accountant/navigate! (standings-path {:id (:id t), :round (:max_standings_round t)}))}]
                           (for [p (combine-pairings-and-pods (:pairings t) (:pod-seats t))]
                             ^{:key [(:id t) (:round_number p) (:id p)]}
                             [pairing p (boolean (:team1_name p))])
                           (when (:seating t)
                             [pairing (:seating t) false])]]]))}))

(defn notification []
  (let [text (subscribe [::subs/notification])]
    (fn notification-render []
      [ui/snackbar {:open               (boolean @text)
                    :message            (or @text "")
                    :auto-hide-duration 5000
                    :on-request-close   #(dispatch [::events/notification nil])}])))
