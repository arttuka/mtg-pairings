(ns mtg-pairings-server.subscriptions.common
  (:require [re-frame.core :refer [reg-sub subscribe]]))

(reg-sub ::mobile?
  (fn [db _]
    (:mobile? db)))

(reg-sub ::page
  (fn [db _]
    (:page db)))

(reg-sub ::language
  (fn [db _]
    (:language db)))
