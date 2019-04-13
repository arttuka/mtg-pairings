(ns ^:figwheel-hooks mtg-pairings-server.core
  (:require [reagent.core :as reagent :refer [atom]]
            [re-frame.core :refer [dispatch-sync subscribe clear-subscription-cache!]]
            [cljsjs.material-ui]
            [cljs-react-material-ui.reagent :as ui]
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
            [mtg-pairings-server.pages.decklist :refer [decklist-organizer decklist-submit]]
            [mtg-pairings-server.pages.main :refer [main-page]]
            [mtg-pairings-server.pages.tournament :refer [tournament-page tournament-subpage tournaments-page]]
            [mtg-pairings-server.pages.organizer :refer [organizer-page organizer-menu deck-construction-tables]]
            [mtg-pairings-server.components.organizer :as organizer]
            [mtg-pairings-server.components.main :refer [header notification]]
            [mtg-pairings-server.util.material-ui :refer [theme]]))

(defn display-header? [page]
  (not (contains? #{:organizer :decklist-submit :decklist-organizer
                    :decklist-organizer-tournament :decklist-organizer-view}
                  (:page page))))

(defn current-page []
  (let [page (subscribe [::common-subs/page])
        hide-organizer-menu? (subscribe [::pairings-subs/organizer :menu])]
    (fn []
      [ui/mui-theme-provider
       {:mui-theme theme}
       [:div
        (when (and (= :organizer (:page @page))
                   (not @hide-organizer-menu?))
          [organizer/menu])
        (when (display-header? @page)
          [header])
        [notification]
        [:div#main-container
         (case (:page @page)
           :main [#'main-page]
           :tournaments [#'tournaments-page]
           :tournament [#'tournament-page (:id @page)]
           (:pairings :standings :pods :seatings :bracket) [#'tournament-subpage (:id @page) (:page @page) (:round @page)]
           :organizer [#'organizer-page]
           :organizer-menu [#'organizer-menu]
           :organizer-deck-construction [#'deck-construction-tables]
           :decklist-submit [#'decklist-submit]
           (:decklist-organizer :decklist-organizer-tournament :decklist-organizer-view) [#'decklist-organizer @page]
           nil)]]])))

(defn mount-root []
  (reagent/render [current-page] (.getElementById js/document "app")))

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
