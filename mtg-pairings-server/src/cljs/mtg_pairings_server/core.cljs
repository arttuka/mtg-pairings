(ns ^:figwheel-hooks mtg-pairings-server.core
  (:require [reagent.core :as reagent :refer [atom]]
            [re-frame.core :refer [dispatch-sync subscribe clear-subscription-cache!]]
            [cljsjs.material-ui]
            [cljs-react-material-ui.core :refer [get-mui-theme]]
            [cljs-react-material-ui.reagent :as ui]
            [mount.core :refer-macros [defstate]]
            [secretary.core :as secretary :include-macros true]
            [accountant.core :as accountant]
            mtg-pairings-server.routes
            mtg-pairings-server.util.event-listener
            [mtg-pairings-server.subscriptions :as subs]
            [mtg-pairings-server.events :as events]
            [mtg-pairings-server.pages.decklist :refer [decklist-submit]]
            [mtg-pairings-server.pages.main :refer [main-page]]
            [mtg-pairings-server.pages.tournament :refer [tournament-page tournament-subpage tournaments-page]]
            [mtg-pairings-server.pages.organizer :refer [organizer-page organizer-menu deck-construction-tables]]
            [mtg-pairings-server.components.organizer :as organizer]
            [mtg-pairings-server.components.main :refer [header notification]]))

(def theme (get-mui-theme
            {:palette {:primary1-color      "#90caf9"
                       :primary2-color      "#5d99c6"
                       :primary3-color      "#c3fdff"
                       :accent1-color       "#ec407a"
                       :accent2-color       "#b4004e"
                       :accent3-color       "#ff77a9"
                       :picker-header-color "#5d99c6"}}))

(defn display-header? [page]
  (not (contains? #{:organizer :decklist-submit} (:page page))))

(defn current-page []
  (let [page (subscribe [::subs/page])
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
           :decklist-submit [#'decklist-submit]
           nil)]]])))

(defn mount-root []
  (reagent/render [current-page] (.getElementById js/document "app")))

(defn ^:after-load figwheel-reload []
  (clear-subscription-cache!)
  (events/connect!)
  (mount-root))

(defn init! []
  (dispatch-sync [::events/initialize])
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
