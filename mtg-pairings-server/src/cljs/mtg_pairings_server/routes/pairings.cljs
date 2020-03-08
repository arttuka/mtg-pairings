(ns mtg-pairings-server.routes.pairings
  (:require [re-frame.core :refer [dispatch]]
            [secretary.core :as secretary :include-macros true]
            [mtg-pairings-server.events.pairings :as events]
            [mtg-pairings-server.routes.common :refer [dispatch-page]]
            [mtg-pairings-server.websocket :as ws]))

(defonce initial-pageload? (atom true))

(secretary/defroute "/" []
  (when-not @initial-pageload?
    (ws/send! [:client/active-tournaments]))
  (dispatch-page :mtg-pairings-server.pages.pairings/main))

(secretary/defroute tournaments-path "/tournaments" []
  (dispatch [::events/load-tournaments])
  (dispatch-page :mtg-pairings-server.pages.pairings/tournaments))

(secretary/defroute multi-organizer-path "/tournaments/organizer" []
  (ws/send! [:client/organizer-tournaments])
  (dispatch-page :mtg-pairings-server.pages.organizer/main))

(secretary/defroute multi-organizer-menu-path "/tournaments/organizer/menu" []
  (ws/send! [:client/organizer-tournaments])
  (dispatch-page :mtg-pairings-server.pages.organizer/menu))

(secretary/defroute tournament-path "/tournaments/:id" [id]
  (let [id (js/parseInt id)]
    (ws/send! [:client/tournament id])
    (dispatch-page :mtg-pairings-server.pages.pairings/tournament id)))

(secretary/defroute pairings-path "/tournaments/:id/pairings-:round" [id round]
  (let [id (js/parseInt id)
        round (js/parseInt round)]
    (ws/send! [:client/pairings [id round]])
    (dispatch-page :mtg-pairings-server.pages.pairings/pairings id round)))

(secretary/defroute standings-path "/tournaments/:id/standings-:round" [id round]
  (let [id (js/parseInt id)
        round (js/parseInt round)]
    (ws/send! [:client/standings [id round]])
    (dispatch-page :mtg-pairings-server.pages.pairings/standings id round)))

(secretary/defroute pods-path "/tournaments/:id/pods-:round" [id round]
  (let [id (js/parseInt id)
        round (js/parseInt round)]
    (ws/send! [:client/pods [id round]])
    (dispatch-page :mtg-pairings-server.pages.pairings/pods id round)))

(secretary/defroute seatings-path "/tournaments/:id/seatings" [id]
  (let [id (js/parseInt id)]
    (ws/send! [:client/seatings id])
    (dispatch-page :mtg-pairings-server.pages.pairings/seatings id)))

(secretary/defroute bracket-path "/tournaments/:id/bracket" [id]
  (let [id (js/parseInt id)]
    (ws/send! [:client/bracket id])
    (dispatch-page :mtg-pairings-server.pages.pairings/bracket id)))

(secretary/defroute organizer-path "/tournaments/:id/organizer" [id]
  (let [id (js/parseInt id)]
    (dispatch [::events/load-organizer-tournament id])
    (dispatch-page :mtg-pairings-server.pages.organizer/main id)))

(secretary/defroute organizer-menu-path "/tournaments/:id/organizer/menu" [id]
  (let [id (js/parseInt id)]
    (dispatch [::events/load-organizer-tournament id])
    (dispatch-page :mtg-pairings-server.pages.organizer/menu id)))

(secretary/defroute deck-construction-path "/tournaments/:id/organizer/deck-construction" [id]
  (let [id (js/parseInt id)]
    (ws/send! [:client/deck-construction id])
    (dispatch-page :mtg-pairings-server.pages.organizer/deck-construction id)))

