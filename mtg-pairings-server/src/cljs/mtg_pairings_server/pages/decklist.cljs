(ns mtg-pairings-server.pages.decklist
  (:require [reagent.core :as reagent :refer [atom]]
            [re-frame.core :refer [subscribe dispatch]]
            [cljsjs.material-ui]
            [cljs-react-material-ui.reagent :as ui]
            [mtg-pairings-server.components.decklist.organizer :as organizer]
            [mtg-pairings-server.components.decklist.submit :as submit]
            [mtg-pairings-server.subscriptions.decklist :as subs]))

(defn decklist-submit []
  [submit/decklist-submit])

(defn loading-indicator []
  [ui/circular-progress
   {:size  36
    :style {:margin "24px"}}])

(defn decklist-organizer [id page]
  (let [user (subscribe [::subs/user])]
    (fn decklist-organizer-render [id page]
      (case @user
        nil [loading-indicator]
        false [organizer/login]
        (case page
          ::organizer-tournament [organizer/tournament id]
          ::organizer [organizer/all-tournaments]
          ::organizer-view (if id
                             [organizer/view-decklist]
                             [organizer/view-decklists]))))))
