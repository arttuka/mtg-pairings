(ns mtg-pairings-server.routes.decklist
  (:require [re-frame.core :refer [dispatch]]
            [secretary.core :as secretary :include-macros true]
            [mtg-pairings-server.routes.common :refer [dispatch-page]]))

(declare organizer-path
         organizer-print-path
         organizer-view-path
         organizer-new-tournament-path
         organizer-tournament-path
         new-decklist-path
         old-decklist-path)

(let [initialized? (atom false)]
  (defn initialize-routes [prefix]
    (when-not @initialized?
      (secretary/defroute organizer-path (str prefix "/organizer") []
        (dispatch-page :decklist-organizer))

      (secretary/defroute organizer-print-path (str prefix "/organizer/print") []
        (dispatch-page :decklist-organizer-view))

      (secretary/defroute organizer-view-path (str prefix "/organizer/view/:id") [id]
        (dispatch-page :decklist-organizer-view id))

      (secretary/defroute organizer-new-tournament-path (str prefix "/organizer/new") []
        (dispatch [:mtg-pairings-server.events.decklist/clear-tournament])
        (dispatch-page :decklist-organizer-tournament))

      (secretary/defroute organizer-tournament-path (str prefix "/organizer/:id") [id]
        (dispatch-page :decklist-organizer-tournament id))

      (secretary/defroute new-decklist-path (str prefix "/tournament/:id") [id]
        (dispatch-page :decklist-submit))

      (secretary/defroute old-decklist-path (str prefix "/:id") [id]
        (dispatch-page :decklist-submit id))

      (reset! initialized? true))))
