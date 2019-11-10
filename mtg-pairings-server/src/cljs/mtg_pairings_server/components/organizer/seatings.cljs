(ns mtg-pairings-server.components.organizer.seatings
  (:require [reagent.core :refer [atom]]
            [re-frame.core :refer [subscribe]]
            [reagent-material-ui.styles :refer [with-styles]]
            [mtg-pairings-server.components.organizer.common :refer [resizing-column header row number-style player-style]]
            [mtg-pairings-server.subscriptions.pairings :as subs]))

(def seating-styles {:table-number number-style
                     :player       player-style})

(defn seating* [{:keys [classes data width]}]
  [row {:style (when width {:width width})}
   [:span {:class (:table-number classes)}
    (:table-number data)]
   [:span {:class (:player classes)}
    (:name data)]])

(def seating ((with-styles seating-styles) seating*))

(defn seatings [menu-hidden?]
  (let [seatings (subscribe [::subs/organizer :seatings])
        tournament (subscribe [::subs/organizer :tournament])]
    (fn [menu-hidden?]
      [:<>
       [header {:variant :h5
                :align   :center}
        (str (:name @tournament) " - seatings")]
       [resizing-column {:items        seatings
                         :component    seating
                         :menu-hidden? menu-hidden?
                         :key-fn       :name}]])))
