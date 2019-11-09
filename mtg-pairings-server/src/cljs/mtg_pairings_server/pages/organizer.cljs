(ns mtg-pairings-server.pages.organizer
  (:require [re-frame.core :refer [subscribe]]
            [mtg-pairings-server.components.organizer.clock :refer [clock]]
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
         :clock [clock]
         [:div])])))

(defn organizer-menu []
  [:div#organizer-page
   [:style {:type "text/css"}
    "#header { display: none !important; }"]
   [menu]])
