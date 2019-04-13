(ns mtg-pairings-server.routes.decklist
  (:require [re-frame.core :refer [dispatch]]
            [secretary.core :as secretary :include-macros true]
            [mtg-pairings-server.events.decklist :as events]
            [mtg-pairings-server.routes.common :refer [dispatch-page]]))

(secretary/defroute decklist-organizer-path "/decklist/organizer" []
  (dispatch-page :decklist-organizer))

(secretary/defroute decklist-organizer-print-path "/decklist/organizer/print" []
  (dispatch-page :decklist-organizer-view))

(secretary/defroute decklist-organizer-view-path "/decklist/organizer/view/:id" [id]
  (dispatch-page :decklist-organizer-view id))

(secretary/defroute decklist-organizer-new-tournament-path "/decklist/organizer/new" []
  (dispatch [::events/clear-tournament])
  (dispatch-page :decklist-organizer-tournament))

(secretary/defroute decklist-organizer-tournament-path "/decklist/organizer/:id" [id]
  (dispatch-page :decklist-organizer-tournament id))

(secretary/defroute new-decklist-submit-path "/decklist/tournament/:id" [id]
  (dispatch-page :decklist-submit))

(secretary/defroute old-decklist-submit-path "/decklist/:id" [id]
  (dispatch-page :decklist-submit id))
