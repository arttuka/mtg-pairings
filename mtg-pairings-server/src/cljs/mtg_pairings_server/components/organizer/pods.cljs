(ns mtg-pairings-server.components.organizer.pods
  (:require [reagent.core :refer [atom]]
            [re-frame.core :refer [subscribe]]
            [reagent-material-ui.styles :refer [with-styles]]
            [mtg-pairings-server.components.organizer.common :refer [column header row number-style player-style]]
            [mtg-pairings-server.subscriptions.pairings :as subs]
            [mtg-pairings-server.util.styles :refer [ellipsis-overflow]]))

(def pod-styles {:number number-style
                 :player player-style})

(defn pod* [{:keys [classes data]}]
  [row
   [:span {:class (:number classes)}
    (:pod data)]
   [:span {:class (:number classes)}
    (:seat data)]
   [:span {:class (:player classes)}
    (:team-name data)]])

(def pod ((with-styles pod-styles) pod*))

(defn pods [menu-hidden?]
  (let [pods (subscribe [::subs/organizer :pods])
        tournament (subscribe [::subs/organizer :tournament])]
    (fn pods-render [menu-hidden?]
      [:<>
       [header {:variant :h5
                :align   :center}
        (str (:name @tournament) " - pods")]
       [column {:menu-hidden? menu-hidden?}
        (for [p @pods]
          ^{:key (:team_name p)}
          [pod {:data p}])]])))
