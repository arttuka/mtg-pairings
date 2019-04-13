(ns mtg-pairings-server.pairings
  (:require [reagent.core :as reagent :refer [atom]]
            [re-frame.core :refer [dispatch-sync subscribe clear-subscription-cache!]]
            [cljsjs.material-ui]
            [cljs-react-material-ui.reagent :as ui]
            [mount.core :as m :refer-macros [defstate]]
            [secretary.core :as secretary :include-macros true]
            [accountant.core :as accountant]
            [mtg-pairings-server.components.main :refer [header notification]]
            [mtg-pairings-server.components.organizer :as organizer]
            [mtg-pairings-server.events.pairings :as events]
            [mtg-pairings-server.pages.main :refer [main-page]]
            [mtg-pairings-server.pages.tournament :refer [tournament-page tournament-subpage tournaments-page]]
            [mtg-pairings-server.pages.organizer :refer [organizer-page organizer-menu deck-construction-tables]]
            [mtg-pairings-server.routes.pairings]
            [mtg-pairings-server.subscriptions.common :as common-subs]
            [mtg-pairings-server.subscriptions.pairings :as subs]
            [mtg-pairings-server.util.event-listener]
            [mtg-pairings-server.util.material-ui :refer [theme]]))

(defn display-header? [page]
  (not (contains? #{:organizer}
                  (:page page))))

(defn current-page []
  (let [page (subscribe [::common-subs/page])
        hide-organizer-menu? (subscribe [::subs/organizer :menu])]
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
           nil)]]])))

(defn mount-root []
  (reagent/render [current-page] (.getElementById js/document "app")))

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
