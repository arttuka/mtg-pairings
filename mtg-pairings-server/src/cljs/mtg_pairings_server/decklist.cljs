(ns mtg-pairings-server.decklist
  (:require [cljsjs.react]
            [cljsjs.react.dom]
            [reagent.core :as reagent :refer [atom]]
            [reagent.dom :as dom]
            [re-frame.core :refer [dispatch-sync subscribe]]
            [mount.core :as m :refer-macros [defstate]]
            [secretary.core :as secretary :include-macros true]
            [accountant.core :as accountant]
            [mtg-pairings-server.components.providers :refer [providers]]
            [mtg-pairings-server.events.decklist :as events]
            [mtg-pairings-server.pages.decklist :as decklist-pages :refer [decklist-organizer decklist-submit]]
            [mtg-pairings-server.routes.decklist :as routes]
            [mtg-pairings-server.subscriptions.common :as subs]))

(defn current-page []
  (let [page-data (subscribe [::subs/page])]
    (fn []
      (let [{:keys [page id]} @page-data]
        [:div#main-container
         (case page
           ::decklist-pages/submit [#'decklist-submit]
           (::decklist-pages/organizer
            ::decklist-pages/organizer-tournament
            ::decklist-pages/organizer-view) [#'decklist-organizer id page]
           nil)]))))

(defn mount-root []
  (dom/render [providers [current-page]] (.getElementById js/document "app")))

(defn init! []
  (dispatch-sync [::events/initialize])
  (routes/initialize-routes "")
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
