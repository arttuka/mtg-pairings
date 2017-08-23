(ns mtg-pairings-server.core
    (:require [reagent.core :as reagent :refer [atom]]
              [mount.core :as m]
              [secretary.core :as secretary :include-macros true]
              [accountant.core :as accountant]
              [mtg-pairings-server.subscriptions]
              [mtg-pairings-server.events]
              [mtg-pairings-server.pages.main :refer [main-page]]
              [mtg-pairings-server.websocket :as ws]))

;; -------------------------
;; Routes

(def page (atom #'main-page))

(defn current-page []
  [:div [@page]])

(secretary/defroute "/" []
  (reset! page #'main-page))

(secretary/defroute "/about" []
  (reset! page #'about-page))

;; -------------------------
;; Initialize app

(defn mount-root []
  (reagent/render [current-page] (.getElementById js/document "app")))

(defn init! []
  (accountant/configure-navigation!
    {:nav-handler
     (fn [path]
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
