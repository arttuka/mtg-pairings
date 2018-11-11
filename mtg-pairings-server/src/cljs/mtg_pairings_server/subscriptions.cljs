(ns mtg-pairings-server.subscriptions
  (:require [re-frame.core :refer [reg-sub subscribe]]
            [cljs-time.core :as time]
            [clojure.string :as str]
            [mtg-pairings-server.util.util :refer [today-or-yesterday?]]
            [mtg-pairings-server.util.mtg-util :refer [duplicate-pairings]]))

(reg-sub ::logged-in-user
  (fn [db _]
    (:logged-in-user db)))

(reg-sub ::player-tournaments
  (fn [db _]
    (:player-tournaments db)))

(reg-sub ::page
  (fn [db _]
    (:page db)))

(reg-sub ::tournaments-page
  (fn [db _]
    (:tournaments-page db)))

(reg-sub ::tournament-filter
  (fn [db [_ filter-key]]
    (get-in db [:tournament-filter filter-key])))

(reg-sub ::tournaments
  (fn [db _]
    (map (:tournaments db) (:tournament-ids db))))

(defn ^:private org-filter [organizer]
  (filter #(or (str/blank? organizer)
               (= organizer (:organizer %)))))

(defn ^:private date-filter [date before?]
  (filter #(or (nil? date)
               (if before?
                 (not (time/after? date (:day %)))
                 (not (time/before? date (:day %)))))))

(reg-sub ::filtered-tournaments
  :<- [::tournaments]
  :<- [::tournament-filter :organizer]
  :<- [::tournament-filter :date-from]
  :<- [::tournament-filter :date-to]
  (fn [[tournaments organizer date-from date-to] _]
    (let [filters (comp (org-filter organizer)
                        (date-filter date-from true)
                        (date-filter date-to false))]
      (sequence filters tournaments))))

(reg-sub ::newest-tournaments
  :<- [::tournaments]
  (fn [tournaments _]
    (filter #(today-or-yesterday? (:day %)) tournaments)))

(reg-sub ::organizers
  :<- [::tournaments]
  (fn [tournaments _]
    (sort (distinct (map :organizer tournaments)))))

(reg-sub ::tournament-count
  (fn [db _]
    (count (:tournaments db))))

(reg-sub ::tournament
  (fn [db [_ id]]
    (get-in db [:tournaments id])))

(reg-sub ::pairings
  (fn [db [_ id round]]
    (get-in db [:pairings id round])))

(reg-sub ::pairings-sort
  (fn [db _]
    (get-in db [:pairings :sort-key])))

(reg-sub ::sorted-pairings
  (fn [[_ id round]]
    [(subscribe [::pairings id round])
     (subscribe [::pairings-sort])])
  (fn [[pairings sort-key] _]
    (cond->> pairings
             (= sort-key :team1_name) duplicate-pairings
             :always (sort-by sort-key))))

(reg-sub ::standings
  (fn [db [_ id round]]
    (get-in db [:standings id round])))

(reg-sub ::pods
  (fn [db [_ id round]]
    (get-in db [:pods id round])))

(reg-sub ::pods-sort
  (fn [db _]
    (get-in db [:pods :sort-key])))

(reg-sub ::sorted-pods
  (fn [[_ id round]]
    [(subscribe [::pods id round])
     (subscribe [::pods-sort])])
  (fn [[pods sort-key] _]
    (sort-by sort-key pods)))

(reg-sub ::seatings
  (fn [db [_ id]]
    (get-in db [:seatings id])))

(reg-sub ::bracket
  (fn [db [_ id]]
    (get-in db [:bracket id])))

(reg-sub ::seatings-sort
  (fn [db _]
    (get-in db [:seatings :sort-key])))

(reg-sub ::sorted-seatings
  (fn [[_ id]]
    [(subscribe [::seatings id])
     (subscribe [::seatings-sort])])
  (fn [[pods sort-key] _]
    (sort-by sort-key pods)))

(reg-sub ::organizer-mode
  (fn [db _]
    (get-in db [:organizer :mode])))

(reg-sub ::organizer
  (fn [db [_ & keys]]
    (get-in (:organizer db) keys)))

(reg-sub ::mobile-menu-collapsed?
  (fn [db _]
    (:mobile-menu-collapsed? db)))
