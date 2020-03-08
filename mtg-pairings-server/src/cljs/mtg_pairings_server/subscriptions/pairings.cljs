(ns mtg-pairings-server.subscriptions.pairings
  (:require [re-frame.core :refer [reg-sub subscribe]]
            [cljs-time.core :as time]
            [mtg-pairings-server.i18n.pairings :as i18n]
            [mtg-pairings-server.subscriptions.common :as common-subs]
            [mtg-pairings-server.util.mtg :refer [duplicate-pairings]]))

(reg-sub ::logged-in-user
  (fn [db _]
    (:logged-in-user db)))

(reg-sub ::player-tournaments
  (fn [db _]
    (:player-tournaments db)))

(reg-sub ::tournaments-page
  (fn [db _]
    (:tournaments-page db)))

(reg-sub ::max-players
  (fn [db _]
    (:max-players db)))

(reg-sub ::tournament-filter
  (fn [db [_ filter-key]]
    (get-in db [:tournament-filter filter-key])))

(reg-sub ::tournaments
  (fn [db _]
    (map (:tournaments db) (:tournament-ids db))))

(defn ^:private org-filter [organizer]
  (filter #(or (= organizer "all-organizers")
               (= organizer (:organizer %)))))

(defn ^:private date-filter [min max]
  (filter #(and (or (nil? min)
                    (not (time/after? min (:day %))))
                (or (nil? max)
                    (not (time/before? max (:day %)))))))

(defn ^:private player-filter [min max]
  (filter #(<= min (:players %) max)))

(reg-sub ::filtered-tournaments
  :<- [::tournaments]
  :<- [::tournament-filter :organizer]
  :<- [::tournament-filter :date-from]
  :<- [::tournament-filter :date-to]
  :<- [::tournament-filter :players]
  (fn [[tournaments organizer date-from date-to [min-players max-players]] _]
    (when (seq tournaments)
      (let [filters (comp (org-filter organizer)
                          (date-filter date-from date-to)
                          (player-filter min-players max-players))]
        (sequence filters tournaments)))))

(reg-sub ::filters-active
  (fn [db _]
    (:filters-active db)))

(reg-sub ::active-tournaments
  (fn [db _]
    (map (:tournaments db) (:active-tournament-ids db))))

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

(reg-sub ::clock-running
  (fn [db _]
    (let [selected-clock (or (get-in db [:organizer :selected-clock])
                             (when (= 1 (count (get-in db [:organizer :clock])))
                               0))]
      (get-in db [:organizer :clock selected-clock :running] false))))

(reg-sub ::notification
  (fn [db _]
    (:notification db)))

(reg-sub ::translate
  :<- [::common-subs/language]
  (fn [language _]
    (partial i18n/translate language)))
