(ns mtg-pairings-server.pages.decklist
  (:require [reagent.core :as reagent :refer [atom]]
            [re-frame.core :refer [subscribe dispatch]]
            [mtg-pairings-server.components.decklist.organizer :as organizer]
            [mtg-pairings-server.components.decklist.submit :as submit]
            [mtg-pairings-server.subscriptions :as subs]))

(defn decklist-submit []
  [submit/decklist-submit])

(defn decklist-organizer [page]
  (let [user (subscribe [::subs/decklist-organizer-user])]
    (fn decklist-organizer-render [page]
      (if @user
        (case (:page page)
          :decklist-organizer-tournament [organizer/tournament (:id page)]
          :decklist-organizer [organizer/all-tournaments]
          :decklist-organizer-view (if (:id page)
                                     [organizer/view-decklist]
                                     [organizer/view-decklists]))
        [organizer/login]))))
