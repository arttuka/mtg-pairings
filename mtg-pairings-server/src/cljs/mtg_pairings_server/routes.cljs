(ns mtg-pairings-server.routes
  (:require [re-frame.core :refer [dispatch]]
            [secretary.core :as secretary :include-macros true]
            [mtg-pairings-server.events :as events]))

(secretary/defroute "/" []
  (dispatch [::events/page {:page :main}]))

(secretary/defroute tournaments-path "/tournaments" []
  (dispatch [::events/page {:page :tournaments}]))

(secretary/defroute tournament-path "/tournaments/:id" [id]
  (dispatch [::events/page {:page :tournament
                            :id   (js/parseInt id)}]))

(secretary/defroute pairings-path "/tournaments/:id/pairings-:round" [id round]
  (let [id (js/parseInt id)
        round (js/parseInt round)]
    (dispatch [::events/load-pairings id round])
    (dispatch [::events/page {:page  :pairings
                              :id    id
                              :round round}])))

(secretary/defroute standings-path "/tournaments/:id/standings-:round" [id round]
  (let [id (js/parseInt id)
        round (js/parseInt round)]
    (dispatch [::events/load-standings id round])
    (dispatch [::events/page {:page  :standings
                              :id    id
                              :round round}])))

(secretary/defroute pods-path "/tournaments/:id/pods-:round" [id round]
  (let [id (js/parseInt id)
        round (js/parseInt round)]
    (dispatch [::events/load-pods id round])
    (dispatch [::events/page {:page  :pods
                              :id    id
                              :round round}])))

(secretary/defroute seatings-path "/tournaments/:id/seatings" [id]
  (let [id (js/parseInt id)]
    (dispatch [::events/load-seatings id])
    (dispatch [::events/page {:page :seatings
                              :id   id}])))

(secretary/defroute organizer-path "/tournaments/:id/organizer" [id]
  (let [id (js/parseInt id)]
    (dispatch [::events/load-organizer-tournament id])
    (dispatch [::events/page {:page :organizer
                              :id   id}])))

(secretary/defroute organizer-menu-path "/tournaments/:id/organizer/menu" [id]
  (let [id (js/parseInt id)]
    (dispatch [::events/load-organizer-tournament id])
    (dispatch [::events/page {:page :organizer-menu
                              :id   id}])))

(secretary/defroute deck-construction-path "/tournaments/:id/organizer/deck-construction" [id]
  (let [id (js/parseInt id)]
    (dispatch [::events/load-deck-construction id])
    (dispatch [::events/page {:page :organizer-deck-construction
                              :id   id}])))
