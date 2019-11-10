(ns mtg-pairings-server.components.organizer.pairings
  (:require [reagent.core :as reagent :refer [atom]]
            [reagent.ratom :refer-macros [reaction]]
            [re-frame.core :refer [subscribe]]
            [reagent-material-ui.components :as ui]
            [reagent-material-ui.styles :refer [with-styles]]
            [mtg-pairings-server.components.organizer.common :refer [resizing-column header row number-style player-style]]
            [mtg-pairings-server.subscriptions.pairings :as subs]
            [mtg-pairings-server.util.mtg :refer [bye? duplicate-pairings]]
            [mtg-pairings-server.util.styles :refer [ellipsis-overflow]]))

(defn pairing-styles [{:keys [palette]}]
  {:bye-row      (let [color (get-in palette [:primary 100])]
                   {:background-color  color
                    "&:nth-child(odd)" {:background-color color}})
   :table-number number-style
   :player       player-style
   :points       {:flex       "0 0 25px"
                  :text-align :center}})

(defn pairing* [{:keys [classes data width]}]
  (let [bye (bye? data)]
    [row {:class-name (when bye (:bye-row classes))
          :style      (when width {:width width})}
     [:span {:class (:table-number classes)}
      (when-not bye (:table-number data))]
     [:span {:class (:player classes)}
      (:team-1-name data)]
     [:span {:class (:points classes)}
      (:team-1-points data)]
     [:span {:class (:player classes)}
      (:team-2-name data)]
     [:span {:class (:points classes)}
      (when-not bye (:team-2-points data))]]))

(def pairing ((with-styles pairing-styles) pairing*))

(defn pairings [menu-hidden?]
  (let [pairings (subscribe [::subs/organizer :pairings])
        duplicated-pairings (reaction (sort-by :team1_name (duplicate-pairings @pairings)))
        pairings-round (subscribe [::subs/organizer :pairings-round])
        tournament (subscribe [::subs/organizer :tournament])]
    (fn [menu-hidden?]
      [:<>
       [header {:variant :h5
                :align   :center}
        (str (:name @tournament) " - kierros " @pairings-round)]
       [resizing-column {:items        duplicated-pairings
                         :component    pairing
                         :menu-hidden? menu-hidden?
                         :key-fn       :team1_name}]])))

