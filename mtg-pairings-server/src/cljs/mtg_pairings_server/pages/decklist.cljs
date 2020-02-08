(ns mtg-pairings-server.pages.decklist
  (:require [reagent.core :as reagent :refer [atom]]
            [re-frame.core :refer [subscribe dispatch]]
            [reagent-material-ui.components :as ui]
            [mtg-pairings-server.components.decklist.organizer :as organizer]
            [mtg-pairings-server.components.decklist.header :refer [header]]
            [mtg-pairings-server.components.decklist.submit :as submit]
            [mtg-pairings-server.components.decklist.tournament :refer [tournament]]
            [mtg-pairings-server.components.decklist.tournament-list :refer [tournament-list]]
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
      [:div#decklist-organizer
       [header]
       (case @user
         nil [loading-indicator]
         false [organizer/login]
         (case page
           ::organizer-tournament [tournament {:id id}]
           ::organizer [tournament-list]
           ::organizer-view (if id
                              [organizer/view-decklist]
                              [organizer/view-decklists])))])))
