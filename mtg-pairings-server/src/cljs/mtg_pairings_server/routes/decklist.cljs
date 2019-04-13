(ns mtg-pairings-server.routes.decklist
  (:require [re-frame.core :refer [dispatch]]
            [secretary.core :as secretary :include-macros true]
            [mtg-pairings-server.events.decklist :as events]
            [mtg-pairings-server.routes.common :refer [dispatch-page]]))

(let [initialized? (atom false)]
  (defn initialize-routes [prefix]
    (when-not @initialized?
      (secretary/defroute decklist-organizer-path (str prefix "/organizer") []
        (dispatch-page :decklist-organizer))

      (secretary/defroute decklist-organizer-print-path (str prefix "/organizer/print") []
        (dispatch-page :decklist-organizer-view))

      (secretary/defroute decklist-organizer-view-path (str prefix "/organizer/view/:id") [id]
        (dispatch-page :decklist-organizer-view id))

      (secretary/defroute decklist-organizer-new-tournament-path (str prefix "/organizer/new") []
        (dispatch [::events/clear-tournament])
        (dispatch-page :decklist-organizer-tournament))

      (secretary/defroute decklist-organizer-tournament-path (str prefix "/organizer/:id") [id]
        (dispatch-page :decklist-organizer-tournament id))

      (secretary/defroute new-decklist-submit-path (str prefix "/tournament/:id") [id]
        (dispatch-page :decklist-submit))

      (secretary/defroute old-decklist-submit-path (str prefix "/:id") [id]
        (dispatch-page :decklist-submit id))

      (reset! initialized? true))))
