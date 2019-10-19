(ns mtg-pairings-server.components.organizer.pods
  (:require [reagent.core :refer [atom]]
            [re-frame.core :refer [subscribe]]
            [reagent-material-ui.styles :refer [with-styles]]
            [mtg-pairings-server.components.organizer.common :refer [column header row]]
            [mtg-pairings-server.styles.common :refer [ellipsis-overflow]]
            [mtg-pairings-server.subscriptions.pairings :as subs]))

(def pod-styles {:number {:text-align  :center
                          :font-weight :bold
                          :font-size   20
                          :flex        "0 0 40px"}
                 :player (merge {:flex 1}
                                ellipsis-overflow)})

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
