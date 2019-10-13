(ns mtg-pairings-server.pairings
  (:require [reagent.core :as reagent :refer [atom]]
            [re-frame.core :refer [dispatch-sync subscribe clear-subscription-cache!]]
            [mount.core :as m :refer-macros [defstate]]
            [secretary.core :as secretary :include-macros true]
            [accountant.core :as accountant]
            [mtg-pairings-server.components.main :refer [notification]]
            [mtg-pairings-server.components.organizer :as organizer]
            [mtg-pairings-server.components.pairings.header :refer [header]]
            [mtg-pairings-server.events.pairings :as events]
            [mtg-pairings-server.pages.pairings :as pairings-pages :refer [main-page tournament-page tournament-subpage tournaments-page]]
            [mtg-pairings-server.pages.organizer :as organizer-pages :refer [organizer-page organizer-menu deck-construction-tables]]
            [mtg-pairings-server.routes.pairings]
            [mtg-pairings-server.subscriptions.common :as common-subs]
            [mtg-pairings-server.subscriptions.pairings :as subs]
            [mtg-pairings-server.theme :refer [theme-provider]]
            [mtg-pairings-server.util.event-listener]))

(defn display-header? [page]
  (not (contains? #{::organizer-pages/main} page)))

(defn current-page []
  (let [page-data (subscribe [::common-subs/page])
        hide-organizer-menu? (subscribe [::subs/organizer :menu])]
    (fn []
      (let [{:keys [page id round]} @page-data]
        [:div
         (when (and (= ::organizer-pages/main page)
                    (not @hide-organizer-menu?))
           [organizer/menu])
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
            ::organizer-pages/deck-construction [#'deck-construction-tables]
            nil)]]))))

(defn mount-root []
  (reagent/render [theme-provider [current-page]] (.getElementById js/document "app")))

(defn init! []
  (dispatch-sync [::events/initialize])
  (events/connect!)
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

;;ignore println statements in prod
(set! *print-fn* (fn [& _]))

(m/start)
