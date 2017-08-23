(ns mtg-pairings-server.core
    (:require [reagent.core :as reagent :refer [atom]]
              [mount.core :as m]
              [secretary.core :as secretary :include-macros true]
              [accountant.core :as accountant]
              [mtg-pairings-server.websocket]))

;; -------------------------
;; Views

(defn home-page []
  [:div [:h2 "Welcome to mtg-pairings-server"]
   [:div [:a {:href "/about"} "go to about page"]]])

(defn about-page []
  [:div [:h2 "About mtg-pairings-server"]
   [:div [:a {:href "/"} "go to the home page"]]])

;; -------------------------
;; Routes

(def page (atom #'home-page))

(defn current-page []
  [:div [@page]])

(secretary/defroute "/" []
  (reset! page #'home-page))

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

(m/defstate core
  :start (init!))
