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
            [mtg-pairings-server.pages.organizer :as organizer-pages :refer [organizer-page organizer-menu deck-construction-tables]]
            [mtg-pairings-server.pages.pairings :as pairings-pages :refer [main-page tournament-page tournament-subpage tournaments-page]]
            [mtg-pairings-server.components.organizer :as organizer]
            [mtg-pairings-server.components.main :refer [notification]]
            [mtg-pairings-server.components.pairings.header :refer [header]]
            [mtg-pairings-server.theme :refer [theme-provider]]))

(defn display-header? [page]
  (not (contains? #{::organizer-pages/main
                    ::decklist-pages/submit
                    ::decklist-pages/organizer
                    ::decklist-pages/organizer-tournament
                    ::decklist-pages/organizer-view}
                  page)))

(defn current-page []
  (let [page-data (subscribe [::common-subs/page])
        hide-organizer-menu? (subscribe [::pairings-subs/organizer :menu])]
    (fn []
      (let [{:keys [page id round]} @page-data]
        [:div
         (when (and (= ::organizer-pages/main page)
                    (not @hide-organizer-menu?))
           [organizer/menu])
         (when (display-header? page)
           [header])
         [notification]
         ;; TODO theme
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
            ::organizer-pages/deck-construction [#'deck-construction-tables]
            ::decklist-pages/submit [#'decklist-submit]
            (::decklist-pages/organizer
             ::decklist-pages/organizer-tournament
             ::decklist-pages/organizer-view) [#'decklist-organizer id page]
            nil)]]))))

(defn mount-root []
  (reagent/render [theme-provider [current-page]] (.getElementById js/document "app")))

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

(defstate core
  :start (init!))
