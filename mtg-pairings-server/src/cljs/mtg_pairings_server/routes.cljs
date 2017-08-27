(ns mtg-pairings-server.routes
  (:require [re-frame.core :refer [dispatch]]
            [secretary.core :as secretary :include-macros true]))

(secretary/defroute "/" []
  (dispatch [:page {:page :main}]))

(secretary/defroute tournament-path "/tournaments/:id" [id]
  (dispatch [:page {:page :tournament
                    :id   (js/parseInt id)}]))

(secretary/defroute pairings-path "/tournaments/:id/pairings-:round" [id round]
  (let [id (js/parseInt id)
        round (js/parseInt round)]
    (dispatch [:load-pairings id round])
    (dispatch [:page {:page  :pairings
                      :id    id
                      :round round}])))

(secretary/defroute standings-path "/tournaments/:id/standings-:round" [id round]
  (let [id (js/parseInt id)
        round (js/parseInt round)]
    (dispatch [:load-standings id round])
    (dispatch [:page {:page  :standings
                      :id    id
                      :round round}])))

(secretary/defroute pods-path "/tournaments/:id/pods-:round" [id round]
  (let [id (js/parseInt id)
        round (js/parseInt round)]
    (dispatch [:load-pods id round])
    (dispatch [:page {:page  :pods
                      :id    id
                      :round round}])))

(secretary/defroute seatings-path "/tournaments/:id/seatings" [id]
  (let [id (js/parseInt id)]
    (dispatch [:load-seatings id])
    (dispatch [:page {:page  :seatings
                      :id    id}])))
