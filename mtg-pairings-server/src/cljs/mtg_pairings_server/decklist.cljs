(ns mtg-pairings-server.decklist
  (:require [reagent.core :as reagent :refer [atom]]
            [re-frame.core :refer [dispatch-sync subscribe]]
            [cljsjs.material-ui]
            [cljs-react-material-ui.reagent :as ui]
            [mount.core :as m :refer-macros [defstate]]
            [secretary.core :as secretary :include-macros true]
            [accountant.core :as accountant]
            [mtg-pairings-server.events.decklist :as events]
            [mtg-pairings-server.pages.decklist :refer [decklist-organizer decklist-submit]]
            [mtg-pairings-server.routes.decklist :as routes]
            [mtg-pairings-server.subscriptions.common :as subs]
            [mtg-pairings-server.util.material-ui :refer [theme]]))

(defn current-page []
  (let [page (subscribe [::subs/page])]
    (fn []
      [ui/mui-theme-provider
       {:mui-theme theme}
       [:div
        [:div#main-container
         (case (:page @page)
           :decklist-submit [#'decklist-submit]
           (:decklist-organizer :decklist-organizer-tournament :decklist-organizer-view) [#'decklist-organizer @page]
           nil)]]])))

(defn mount-root []
  (reagent/render [current-page] (.getElementById js/document "app")))

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
