(ns mtg-pairings-server.core
    (:require [reagent.core :as reagent :refer [atom]]
              [re-frame.core :refer [dispatch subscribe]]
              [mount.core :as m]
              [secretary.core :as secretary :include-macros true]
              [accountant.core :as accountant]
              [mtg-pairings-server.subscriptions]
              [mtg-pairings-server.events]
              [mtg-pairings-server.pages.main :refer [main-page]]
              [mtg-pairings-server.pages.tournament :refer [tournament-page]]
              [mtg-pairings-server.websocket :as ws]))

;; -------------------------
;; Routes

(defn current-page []
  (let [page (subscribe [:page])]
    (fn []
      [:div (case (:page @page)
              :main [#'main-page]
              :tournament [#'tournament-page (:id @page)]
              nil)])))

(secretary/defroute "/" []
  (dispatch [:page {:page :main}]))

(secretary/defroute "/tournaments/:id" [id]
  (dispatch [:page {:page :tournament
                    :id (js/parseInt id)}]))

;; -------------------------
;; Initialize app

(defn mount-root []
  (reagent/render [current-page] (.getElementById js/document "app")))

(defn init! []
  (accountant/configure-navigation!
    {:nav-handler
     (fn [path & more]
       (secretary/dispatch! path))
     :path-exists?
     (fn [path]
       (secretary/locate-route path))})
  (accountant/dispatch-current!)
  (mount-root))

(defmethod ws/event-handler :chsk/state
  [{:keys [?data]}]
  (let [[_ new-state] ?data]
    (when (:first-open? new-state)
      (ws/send! [:client/connect]))))

(m/defstate core
  :start (init!))
