(ns mtg-pairings-server.subscriptions.decklist
  (:require [re-frame.core :refer [reg-sub subscribe]]))

(reg-sub ::tournament
  (fn [db _]
    (get-in db [:decklist-editor :tournament])))

(reg-sub ::saving?
  (fn [db _]
    (get-in db [:decklist-editor :saving])))

(reg-sub ::saved?
  (fn [db _]
    (get-in db [:decklist-editor :saved])))

(reg-sub ::error?
  (fn [db _]
    (get-in db [:decklist-editor :error])))

(reg-sub ::decklist
  (fn [db _]
    (get-in db [:decklist-editor :decklist])))

(reg-sub ::decklists
  (fn [db _]
    (get-in db [:decklist-editor :decklists])))

(reg-sub ::organizer-tournaments
  (fn [db _]
    (get-in db [:decklist-editor :organizer-tournaments])))

(reg-sub ::organizer-tournament
  (fn [db _]
    (get-in db [:decklist-editor :organizer-tournament])))

(reg-sub ::user
  (fn [db _]
    (get-in db [:decklist-editor :user])))
