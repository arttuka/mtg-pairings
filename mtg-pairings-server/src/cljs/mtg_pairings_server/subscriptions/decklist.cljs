(ns mtg-pairings-server.subscriptions.decklist
  (:require [re-frame.core :refer [reg-sub subscribe]]
            [clojure.string :as str]
            [cljs-time.coerce :as coerce]
            [mtg-pairings-server.util.decklist :refer [by-type]]))

(reg-sub ::tournament
  (fn [db _]
    (get-in db [:decklist-editor :tournament])))

(reg-sub ::saving?
  (fn [db _]
    (get-in db [:decklist-editor :saving])))

(reg-sub ::saved?
  (fn [db _]
    (get-in db [:decklist-editor :saved])))

(reg-sub ::decklist
  (fn [db _]
    (get-in db [:decklist-editor :decklist])))

(reg-sub ::decklist-by-type
  :<- [::decklist]
  (fn [decklist _]
    (by-type decklist)))

(reg-sub ::decklists
  (fn [db _]
    (get-in db [:decklist-editor :decklists])))

(reg-sub ::decklists-by-type
  :<- [::decklists]
  (fn [decklists _]
    (map by-type decklists)))

(reg-sub ::organizer-tournaments
  (fn [db _]
    (get-in db [:decklist-editor :organizer-tournaments])))

(reg-sub ::organizer-tournament
  (fn [db _]
    (get-in db [:decklist-editor :organizer-tournament])))

(reg-sub ::decklist-sort
  (fn [db _]
    (get-in db [:decklist-editor :sort])))

(reg-sub ::organizer-decklists
  :<- [::organizer-tournament]
  :<- [::decklist-sort]
  (fn [[tournament sort-data] _]
    (let [decklists (:decklist tournament)
          keyfn (case (:key sort-data)
                  :name (juxt (comp str/lower-case :last-name)
                              (comp str/lower-case :first-name))
                  :submitted (comp coerce/to-long :submitted))
          sorted-decklists (sort-by keyfn decklists)]
      (vec (if (:ascending sort-data)
             sorted-decklists
             (reverse sorted-decklists))))))

(reg-sub ::user
  (fn [db _]
    (get-in db [:decklist-editor :user])))

(reg-sub ::error
  (fn [db [_ k]]
    (get-in db [:decklist-editor :error k])))

(reg-sub ::loaded?
  (fn [db _]
    (get-in db [:decklist-editor :loaded])))
