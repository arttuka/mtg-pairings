(ns mtg-pairings-server.components.main
  (:require [reagent.core :refer [atom]]
            [re-frame.core :refer [subscribe dispatch]]
            [mtg-pairings-server.routes :refer [tournaments-path]]))

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
