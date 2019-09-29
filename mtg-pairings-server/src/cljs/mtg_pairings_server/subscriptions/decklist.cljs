(ns mtg-pairings-server.subscriptions.decklist
  (:require [re-frame.core :refer [reg-sub subscribe]]
            [clojure.string :as str]
            [cljs-time.core :as time]
            [cljs-time.coerce :as coerce]
            [mtg-pairings-server.i18n.decklist :as i18n]
            [mtg-pairings-server.subscriptions.common :as common-subs]
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

(reg-sub ::only-upcoming?
  (fn [db _]
    (get-in db [:decklist-editor :tournament-sort :only-upcoming])))

(reg-sub ::organizer-tournaments
  (fn [db _]
    (get-in db [:decklist-editor :organizer-tournaments])))

(reg-sub ::filtered-organizer-tournaments
  :<- [::organizer-tournaments]
  :<- [::only-upcoming?]
  (fn [[tournaments only-upcoming?] _]
    (if only-upcoming?
      (remove #(time/before? (:date %) (time/today)) tournaments)
      tournaments)))

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

(reg-sub ::translate
  :<- [::common-subs/language]
  (fn [language _]
    (partial i18n/translate language)))
