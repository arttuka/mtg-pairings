(ns mtg-pairings-server.core
  (:require [reagent.core :as reagent :refer [atom]]
            [re-frame.core :refer [dispatch dispatch-sync subscribe]]
            [mount.core :as m]
            [secretary.core :as secretary :include-macros true]
            [accountant.core :as accountant]
            [mtg-pairings-server.subscriptions]
            [mtg-pairings-server.events]
            [mtg-pairings-server.pages.main :refer [main-page]]
            [mtg-pairings-server.pages.tournament :refer [tournament-page pairings-page standings-page
                                                          pods-page seatings-page tournaments-page]]
            [mtg-pairings-server.pages.organizer :refer [organizer-page]]
            [mtg-pairings-server.components.main :refer [header]]
            [mtg-pairings-server.websocket :as ws]))

(defn current-page []
  (let [page (subscribe [:page])]
    (fn []
      [:div
       [header]
       (case (:page @page)
         :main [#'main-page]
         :tournaments [#'tournaments-page]
         :tournament [#'tournament-page (:id @page)]
         :pairings [#'pairings-page (:id @page) (:round @page)]
         :standings [#'standings-page (:id @page) (:round @page)]
         :pods [#'pods-page (:id @page) (:round @page)]
         :seatings [#'seatings-page (:id @page)]
         :organizer [#'organizer-page]
         nil)])))

(defn mount-root []
  (reagent/render [current-page] (.getElementById js/document "app")))

(defn init! []
  (dispatch-sync [:initialize])
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
