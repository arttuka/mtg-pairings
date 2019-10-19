(ns mtg-pairings-server.components.organizer.standings
  (:require [reagent.core :refer [atom]]
            [re-frame.core :refer [subscribe]]
            [reagent-material-ui.styles :refer [with-styles]]
            [goog.string :as gstring]
            [mtg-pairings-server.components.organizer.common :refer [column header row]]
            [mtg-pairings-server.styles.common :refer [ellipsis-overflow]]
            [mtg-pairings-server.subscriptions.pairings :as subs]))

(def standing-styles {:rank    {:text-align  :center
                                :font-weight :bold
                                :flex        "0 0 40px"}
                      :player  (merge {:flex 1}
                                      ellipsis-overflow)
                      :points  {:text-align :center
                                :flex       "0 0 40px"}
                      :omw     {:text-align :center
                                :flex       "0 0 70px"}
                      :pgw-ogw {:text-align :center
                                :flex       "0 0 60px"
                                :font-size  14}})

(defn percentage [n]
  (gstring/format "%.3f" (* 100 n)))

(defn standing* [{:keys [classes data]}]
  [row
   [:span {:class (:rank classes)}
    (:rank data)]
   [:span {:class (:player classes)}
    (:team-name data)]
   [:span {:class (:points classes)}
    (:points data)]
   [:span {:class (:omw classes)}
    (percentage (:omw data))]
   [:span {:class (:pgw-ogw classes)}
    (percentage (:pgw data))]
   [:span {:class (:pgw-ogw classes)}
    (percentage (:ogw data))]])

(def standing ((with-styles standing-styles) standing*))

(defn standings [menu-hidden?]
  (let [standings (subscribe [::subs/organizer :standings])
        tournament (subscribe [::subs/organizer :tournament])
        standings-round (subscribe [::subs/organizer :standings-round])]
    (fn standings-render [menu-hidden?]
      [:<>
       [header {:variant :h5
                :align   :center}
        (str (:name @tournament) " - kierros " @standings-round)]
       [column {:menu-hidden? menu-hidden?}
        (for [s @standings]
          ^{:key (str "standings-" (:rank s))}
          [standing {:data s}])]])))
