(ns ^:figwheel-hooks mtg-pairings-server.core
  (:require [reagent.core :as reagent :refer [atom]]
            [re-frame.core :refer [dispatch-sync subscribe clear-subscription-cache!]]
            [mount.core :refer-macros [defstate]]
            [secretary.core :as secretary :include-macros true]
            [accountant.core :as accountant]
            [mtg-pairings-server.routes.decklist :as decklist-routes]
            mtg-pairings-server.routes.pairings
            mtg-pairings-server.util.event-listener
            [mtg-pairings-server.subscriptions.common :as common-subs]
            [mtg-pairings-server.subscriptions.decklist :as decklist-subs]
            [mtg-pairings-server.subscriptions.pairings :as pairings-subs]
            [mtg-pairings-server.events.decklist :as decklist-events]
            [mtg-pairings-server.events.pairings :as pairings-events]
            [mtg-pairings-server.pages.decklist :as decklist-pages :refer [decklist-organizer decklist-submit]]
            [mtg-pairings-server.pages.organizer :as organizer-pages :refer [organizer-page organizer-menu]]
            [mtg-pairings-server.pages.pairings :as pairings-pages :refer [main-page tournament-page tournament-subpage tournaments-page]]
            [mtg-pairings-server.components.notification :refer [notification]]
            [mtg-pairings-server.components.organizer.deck-construction :refer [deck-construction-tables]]
            [mtg-pairings-server.components.pairings.header :refer [header]]
            [mtg-pairings-server.components.providers :refer [providers]]))

(defn display-header? [page]
  (and page
       (not (contains? #{::organizer-pages/main
                         ::organizer-pages/menu
                         ::organizer-pages/deck-construction
                         ::decklist-pages/submit
                         ::decklist-pages/organizer
                         ::decklist-pages/organizer-tournament
                         ::decklist-pages/organizer-view}
                       page))))

(defn current-page []
  (let [page-data (subscribe [::common-subs/page])]
    (fn []
      (let [{:keys [page id round]} @page-data]
        [:<>
         (when (display-header? page)
           [header])
         [notification]
         [:div#main-container
          (case page
            ::pairings-pages/main [#'main-page]
            ::pairings-pages/tournaments [#'tournaments-page]
            ::pairings-pages/tournament [#'tournament-page id]
            (::pairings-pages/pairings
             ::pairings-pages/standings
             ::pairings-pages/pods
             ::pairings-pages/seatings
             ::pairings-pages/bracket) [#'tournament-subpage id page round]
            ::organizer-pages/main [#'organizer-page]
            ::organizer-pages/menu [#'organizer-menu]
            ::organizer-pages/deck-construction [deck-construction-tables]
            ::decklist-pages/submit [#'decklist-submit]
            (::decklist-pages/organizer
             ::decklist-pages/organizer-tournament
             ::decklist-pages/organizer-view) [#'decklist-organizer id page]
            nil)]]))))

(defn mount-root []
  (reagent/render [providers [current-page]] (.getElementById js/document "app")))

(defn ^:after-load figwheel-reload []
  (clear-subscription-cache!)
  (pairings-events/connect!)
  (decklist-events/connect!)
  (mount-root))

(defn init! []
  (dispatch-sync [::pairings-events/initialize])
  (dispatch-sync [::decklist-events/initialize])
  (decklist-routes/initialize-routes "/decklist")
  (pairings-events/connect!)
  (decklist-events/connect!)
  (accountant/configure-navigation!
   {:nav-handler
    (fn [path]
      (secretary/dispatch! path))
    :path-exists?
    (fn [path]
      (secretary/locate-route path))})
  (accountant/dispatch-current!)
  (mount-root))

(defstate ^{:on-reload :noop} core
  :start (init!))
