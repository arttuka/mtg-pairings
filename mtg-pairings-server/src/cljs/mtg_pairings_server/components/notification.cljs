(ns mtg-pairings-server.components.notification
  (:require [reagent.core :as reagent :refer [atom]]
            [re-frame.core :refer [subscribe dispatch]]
            [reagent-material-ui.core.snackbar :refer [snackbar]]
            [mtg-pairings-server.events.pairings :as events]
            [mtg-pairings-server.subscriptions.pairings :as subs]))

(defn notification []
  (let [text (subscribe [::subs/notification])
        on-close #(dispatch [::events/notification nil])]
    (fn []
      [snackbar {:open               (boolean @text)
                 :message            (or @text "")
                 :auto-hide-duration 5000
                 :on-close           on-close}])))
