(ns mtg-pairings-server.subscriptions
  (:require [re-frame.core :refer [reg-sub subscribe]]
            [mtg-pairings-server.util.mtg-util :refer [reverse-match]]))

(reg-sub :page
  (fn [db _]
    (:page db)))

(reg-sub :tournaments
  (fn [db _]
    (map (:tournaments db) (:tournament-ids db))))

(reg-sub :tournament
  (fn [db [_ id]]
    (get-in db [:tournaments id])))

(reg-sub :pairings
  (fn [db [_ id round]]
    (get-in db [:pairings id round])))

(reg-sub :pairings-sort
  (fn [db _]
    (get-in db [:pairings :sort-key])))

(reg-sub :sorted-pairings
  (fn [[_ id round]]
    [(subscribe [:pairings id round])
     (subscribe [:pairings-sort])])
  (fn [[pairings sort-key] _]
    (cond->> pairings
             (= sort-key :team1_name) (concat (map reverse-match pairings))
             (= sort-key :team1_name) (remove #(= "***BYE***" (:team1_name %)))
             :always (sort-by sort-key))))

(reg-sub :standings
  (fn [db [_ id round]]
    (get-in db [:standings id round])))
