(ns mtg-pairings-server.pages.organizer
  (:require [re-frame.core :refer [subscribe]]
            [mtg-pairings-server.components.organizer :refer [menu pairings seatings pods standings clock]]))

(defn organizer-page []
  (let [organizer-mode (subscribe [:organizer-mode])]
    (fn []
      [:div#organizer-page
       [:style {:type "text/css"}
        "#header { display: none; }"]
       [menu]
       (case @organizer-mode
         :pairings [pairings]
         :seatings [seatings]
         :pods [pods]
         :standings [standings]
         :clock [clock]
         [:div])])))
