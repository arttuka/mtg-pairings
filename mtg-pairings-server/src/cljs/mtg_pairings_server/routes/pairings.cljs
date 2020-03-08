(ns mtg-pairings-server.routes.pairings
  (:require [re-frame.core :refer [dispatch]]
            [secretary.core :as secretary :include-macros true]
            [mtg-pairings-server.events.pairings :as events]
            [mtg-pairings-server.routes.common :refer [dispatch-page]]))

(defonce initial-pageload? (atom true))

(secretary/defroute "/" []
  (when-not @initial-pageload?
    (dispatch [::events/load-active-tournaments]))
  (dispatch-page :mtg-pairings-server.pages.pairings/main))

(secretary/defroute tournaments-path "/tournaments" []
  (dispatch [::events/load-tournaments])
  (dispatch-page :mtg-pairings-server.pages.pairings/tournaments))

(secretary/defroute multi-organizer-path "/tournaments/organizer" []
  (dispatch [::events/load-organizer-tournaments])
  (dispatch-page :mtg-pairings-server.pages.organizer/main))

(secretary/defroute multi-organizer-menu-path "/tournaments/organizer/menu" []
  (dispatch [::events/load-organizer-tournaments])
  (dispatch-page :mtg-pairings-server.pages.organizer/menu))

(secretary/defroute tournament-path "/tournaments/:id" [id]
  (let [id (js/parseInt id)]
    (dispatch [::events/load-tournament id])
    (dispatch-page :mtg-pairings-server.pages.pairings/tournament id)))

(secretary/defroute pairings-path "/tournaments/:id/pairings-:round" [id round]
  (let [id (js/parseInt id)
        round (js/parseInt round)]
    (dispatch [::events/load-pairings id round])
    (dispatch-page :mtg-pairings-server.pages.pairings/pairings id round)))

(secretary/defroute standings-path "/tournaments/:id/standings-:round" [id round]
  (let [id (js/parseInt id)
        round (js/parseInt round)]
    (dispatch [::events/load-standings id round])
    (dispatch-page :mtg-pairings-server.pages.pairings/standings id round)))

(secretary/defroute pods-path "/tournaments/:id/pods-:round" [id round]
  (let [id (js/parseInt id)
        round (js/parseInt round)]
    (dispatch [::events/load-pods id round])
    (dispatch-page :mtg-pairings-server.pages.pairings/pods id round)))

(secretary/defroute seatings-path "/tournaments/:id/seatings" [id]
  (let [id (js/parseInt id)]
    (dispatch [::events/load-seatings id])
    (dispatch-page :mtg-pairings-server.pages.pairings/seatings id)))

(secretary/defroute bracket-path "/tournaments/:id/bracket" [id]
  (let [id (js/parseInt id)]
    (dispatch [::events/load-bracket id])
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
    (dispatch [::events/load-deck-construction id])
    (dispatch-page :mtg-pairings-server.pages.organizer/deck-construction id)))

