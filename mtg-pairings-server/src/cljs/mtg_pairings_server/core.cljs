(ns mtg-pairings-server.core
  (:require [reagent.core :as reagent :refer [atom]]
            [re-frame.core :refer [dispatch dispatch-sync subscribe clear-subscription-cache!]]
            [cljsjs.material-ui]
            [cljs-react-material-ui.core :refer [get-mui-theme]]
            [cljs-react-material-ui.reagent :as ui]
            [mount.core :as m]
            [secretary.core :as secretary :include-macros true]
            [accountant.core :as accountant]
            [mtg-pairings-server.subscriptions :as subs]
            [mtg-pairings-server.events :as events]
            [mtg-pairings-server.pages.main :refer [main-page]]
            [mtg-pairings-server.pages.tournament :refer [tournament-page pairings-page standings-page
                                                          pods-page seatings-page tournaments-page
                                                          bracket-page]]
            [mtg-pairings-server.pages.organizer :refer [organizer-page organizer-menu deck-construction-tables]]
            [mtg-pairings-server.components.main :refer [header mobile-menu]]
            [mtg-pairings-server.websocket :as ws]))

(defn current-page []
  (let [page (subscribe [::subs/page])]
    (fn []
      [ui/mui-theme-provider
       {:mui-theme (get-mui-theme)}
       [:div
        [header]
        [mobile-menu]
        (case (:page @page)
          :main [#'main-page]
          :tournaments [#'tournaments-page]
          :tournament [#'tournament-page (:id @page)]
          :pairings [#'pairings-page (:id @page) (:round @page)]
          :standings [#'standings-page (:id @page) (:round @page)]
          :pods [#'pods-page (:id @page) (:round @page)]
          :seatings [#'seatings-page (:id @page)]
          :bracket [#'bracket-page (:id @page)]
          :organizer [#'organizer-page]
          :organizer-menu [#'organizer-menu]
          :organizer-deck-construction [#'deck-construction-tables]
          nil)]])))

(defn mount-root []
  (clear-subscription-cache!)
  (reagent/render [current-page] (.getElementById js/document "app")))

(defn init! []
  (dispatch-sync [::events/initialize])
  (accountant/configure-navigation!
    {:nav-handler
     (fn [path & more]
       (secretary/dispatch! path))
     :path-exists?
     (fn [path]
       (secretary/locate-route path))})
  (accountant/dispatch-current!)
  (mount-root))

(m/defstate core
  :start (init!))
