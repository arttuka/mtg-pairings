(ns mtg-pairings-server.components.main
  (:require [reagent.core :refer [atom]]
            [re-frame.core :refer [subscribe dispatch]]
            [mtg-pairings-server.components.organizer :refer [pairing]]
            [mtg-pairings-server.routes :refer [tournaments-path standings-path]]
            [mtg-pairings-server.util.util :refer [format-date indexed]]))

(defn header []
  (let [user (subscribe [:logged-in-user])
        collapsed? (atom true)
        dci-number (atom nil)]
    (fn []
      (if @user
        [:div#header
         [:div.hidden-xs.logged-menu
          [:span (:name @user)]
          [:button {:on-click #(dispatch [:logout])}
           "KIRJAUDU ULOS"]
          [:a {:href (tournaments-path)}
           "KAIKKI TURNAUKSET"]
          [:a {:href "/"}
           "ETUSIVU"]]
         [:div.hidden-sm.hidden-md.hidden-lg
          [:span (:name @user)]
          [:span.pull-right.menu-icon
           {:on-click #(swap! collapsed? not)}
           [:i]]]]
        [:div#header
         [:form
          {:on-submit #(do
                         (dispatch [:login @dci-number])
                         (reset! dci-number nil)
                         (.preventDefault %))}
          [:input
           {:type        "number"
            :placeholder "DCI-numero"
            :value       @dci-number
            :on-change   #(reset! dci-number (-> % .-target .-value))}]
          [:input.pull-left
           {:type  "submit"
            :value "KIRJAUDU"}]
          [:a.hidden-xs
           {:href (tournaments-path)}
           "KAIKKI TURNAUKSET"]
          [:span.pull-right.menu-icon.hidden-sm.hidden-md.hidden-lg
           {:on-click #(swap! collapsed? not)}
           [:i]]]]))))

(defn own-tournament [t]
  (let [collapsed? (atom true)]
    (fn [t]
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
        (for [[i p] (indexed (:pairings t))]
          ^{:key [(:id t) (:round_number p)]}
          [:div.own-tournament-pairing
           [pairing p (even? i) true true]])
        [:div.own-tournament-pairing
         [pairing (:seating t) (even? (count (:pairings t))) true false]]]])))
