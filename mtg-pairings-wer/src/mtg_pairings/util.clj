(ns mtg-pairings.util
  (:require [clj-time.core :as time]
            [clj-time.coerce :as time-coerce]
            [clojure.string :as string]
            [clojure.set :as set]
            [clojure.walk :refer [postwalk]]))

(defn filename
  [name]
  (fn [f]
    (= (.getName f) name)))

(defn zip [& colls]
  (let [firsts (mapv first colls)
        rests (map rest colls)]
    (lazy-seq
      (when (not-every? nil? firsts)
        (cons firsts (apply zip rests))))))

(defn sanction-id [tournament]
  (if-not (string/blank? (:SanctionId tournament))
    (:SanctionId tournament)
    (str (:TournamentId tournament) "-" (:StartDate tournament))))

(defn changed-keys [old-map new-map]
  (let [ks (set/union (set (keys old-map)) (set (keys new-map)))]
    (for [k (sort ks)
          :when (not= (old-map k) (new-map k))]
      k)))

(defn map-keys [f m]
  (into {} (for [[k v] m]
             [(f k) v])))

(defn ^:private convert-instances-of
  [cls f m]
  (postwalk (fn [x]
              (if (instance? cls x)
                (f x)
                x))
            m))

(defn ^:private to-local-date-default-tz
  [date]
  (let [dt (time-coerce/to-date-time date)]
    (time-coerce/to-local-date (time/to-time-zone dt (time/default-time-zone)))))

(def java-date->joda-date
  (partial convert-instances-of java.util.Date to-local-date-default-tz))

(def joda-date->java-date
  (partial convert-instances-of org.joda.time.LocalDate time-coerce/to-date))