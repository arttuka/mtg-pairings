(ns mtg-pairings-server.routes
  (:require [re-frame.core :refer [dispatch]]
            [secretary.core :as secretary :include-macros true]
            [mtg-pairings-server.events :as events]))

(defn dispatch-page
  ([page]
   (dispatch-page page nil nil))
  ([page id]
   (dispatch-page page id nil))
  ([page id round]
   (dispatch [::events/page {:page  page
                             :id    id
                             :round round}])))

(secretary/defroute "/" []
  (dispatch-page :main))

(secretary/defroute tournaments-path "/tournaments" []
  (dispatch-page :tournaments))

(secretary/defroute tournament-path "/tournaments/:id" [id]
  (let [id (js/parseInt id)]
    (dispatch [::events/load-tournament id])
    (dispatch-page :tournament id)))

(secretary/defroute pairings-path "/tournaments/:id/pairings-:round" [id round]
  (let [id (js/parseInt id)
        round (js/parseInt round)]
    (dispatch [::events/load-pairings id round])
    (dispatch-page :pairings id round)))

(secretary/defroute standings-path "/tournaments/:id/standings-:round" [id round]
  (let [id (js/parseInt id)
        round (js/parseInt round)]
    (dispatch [::events/load-standings id round])
    (dispatch-page :standings id round)))

(secretary/defroute pods-path "/tournaments/:id/pods-:round" [id round]
  (let [id (js/parseInt id)
        round (js/parseInt round)]
    (dispatch [::events/load-pods id round])
    (dispatch-page :pods id round)))

(secretary/defroute seatings-path "/tournaments/:id/seatings" [id]
  (let [id (js/parseInt id)]
    (dispatch [::events/load-seatings id])
    (dispatch-page :seatings id)))

(secretary/defroute bracket-path "/tournaments/:id/bracket" [id]
  (let [id (js/parseInt id)]
    (dispatch [::events/load-bracket id])
    (dispatch-page :bracket id)))

(secretary/defroute organizer-path "/tournaments/:id/organizer" [id]
  (let [id (js/parseInt id)]
    (dispatch [::events/load-organizer-tournament id])
    (dispatch-page :organizer id)))

(secretary/defroute organizer-menu-path "/tournaments/:id/organizer/menu" [id]
  (let [id (js/parseInt id)]
    (dispatch [::events/load-organizer-tournament id])
    (dispatch-page :organizer-menu id)))

(secretary/defroute deck-construction-path "/tournaments/:id/organizer/deck-construction" [id]
  (let [id (js/parseInt id)]
    (dispatch [::events/load-deck-construction id])
    (dispatch-page :organizer-deck-construction id)))

(secretary/defroute decklist-organizer-path "/decklist/organizer" []
  (dispatch-page :decklist-organizer))

(secretary/defroute decklist-organizer-print-path "/decklist/organizer/print" []
  (dispatch-page :decklist-organizer-view))

(secretary/defroute decklist-organizer-view-path "/decklist/organizer/view/:id" [id]
  (dispatch-page :decklist-organizer-view id))

(secretary/defroute decklist-organizer-new-tournament-path "/decklist/organizer/new" []
  (dispatch-page :decklist-organizer-tournament))

(secretary/defroute decklist-organizer-tournament-path "/decklist/organizer/:id" [id]
  (dispatch-page :decklist-organizer-tournament id))

(secretary/defroute new-decklist-submit-path "/decklist/tournament/:id" [id]
  (dispatch-page :decklist-submit))

(secretary/defroute old-decklist-submit-path "/decklist/:id" [id]
  (dispatch-page :decklist-submit id))
