(ns mtg-pairings-server.pages.organizer
  (:require [re-frame.core :refer [subscribe]]
            [mtg-pairings-server.components.organizer.clock :refer [clocks]]
            [mtg-pairings-server.components.organizer.menu :refer [menu]]
            [mtg-pairings-server.components.organizer.pairings :refer [pairings]]
            [mtg-pairings-server.components.organizer.pods :refer [pods]]
            [mtg-pairings-server.components.organizer.seatings :refer [seatings]]
            [mtg-pairings-server.components.organizer.standings :refer [standings]]
            [mtg-pairings-server.subscriptions.pairings :as subs]))

(defn organizer-page []
  (let [organizer-mode (subscribe [::subs/organizer-mode])
        hide-organizer-menu? (subscribe [::subs/organizer :menu])]
    (fn organizer-page-render []
      [:div#organizer-page
       (when-not @hide-organizer-menu? [menu])
       (case @organizer-mode
         :pairings [pairings @hide-organizer-menu?]
         :seatings [seatings @hide-organizer-menu?]
         :pods [pods @hide-organizer-menu?]
         :standings [standings @hide-organizer-menu?]
         :clock [clocks]
         [:div])])))

(defn organizer-menu []
  [:div#organizer-page
   [menu]
   [clocks]])
