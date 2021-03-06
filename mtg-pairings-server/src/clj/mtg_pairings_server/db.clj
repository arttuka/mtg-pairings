(ns mtg-pairings-server.db
  (:require [next.jdbc :as jdbc]
            [next.jdbc.connection :as connection]
            [next.jdbc.prepare :as p]
            [next.jdbc.result-set :as rs]
            [hikari-cp.core :as hikari]
            [honeysql.core :as honeysql]
            [honeysql.format :as format]
            [clj-time.core :as time]
            [clj-time.coerce :as tc]
            [mount.core :refer [defstate]]
            [config.core :refer [env]]
            [mtg-pairings-server.util.honeysql])
  (:import (java.sql Date PreparedStatement Timestamp)
           (org.joda.time LocalDate DateTime)
           (org.postgresql.jdbc PgArray)))

(defn ^:private to-local-date-default-tz
  [date]
  (let [dt (tc/to-date-time date)]
    (tc/to-local-date (time/to-time-zone dt (time/default-time-zone)))))

(extend-protocol p/SettableParameter
  LocalDate
  (set-parameter [v ^PreparedStatement s i]
    (.setDate s i (tc/to-sql-date v)))

  DateTime
  (set-parameter [v ^PreparedStatement s i]
    (.setTimestamp s i (tc/to-sql-time v))))

(extend-protocol rs/ReadableColumn
  Timestamp
  (read-column-by-label [v _] (tc/to-date-time v))
  (read-column-by-index [v _ _] (tc/to-date-time v))

  Date
  (read-column-by-label [v _] (to-local-date-default-tz v))
  (read-column-by-index [v _ _] (to-local-date-default-tz v))

  PgArray
  (read-column-by-label [v _] (vec (.getArray v)))
  (read-column-by-index [v _ _] (vec (.getArray v))))

(def db-spec {:adapter       "postgresql"
              :username      (env :db-user)
              :password      (env :db-password)
              :database-name (env :db-name)
              :server-name   (env :db-host)
              :port-number   (env :db-port)})

(defstate ^{:on-reload :noop} db
  :start (hikari/make-datasource db-spec)
  :stop (hikari/close-datasource @db))

(def ^:dynamic *tx* nil)

(defmacro with-transaction [& body]
  `(jdbc/with-transaction [tx# @db]
     (binding [*tx* tx#]
       ~@body)))

(defn ^:private execute! [q opts]
  (jdbc/execute! *tx*
                 (binding [format/*name-transform-fn* identity]
                   (honeysql/format q :quoting :ansi))
                 opts))

(defn query
  "Executes a query and returns results as a vector of maps"
  [q]
  (execute! q {:builder-fn rs/as-unqualified-lower-maps}))

(defn queryv
  "Executes a query and returns results as a vector of vectors"
  [q]
  (next (execute! q {:builder-fn rs/as-arrays})))

(defn one-or-nil
  [results]
  (let [[result & more] results]
    (when (seq more)
      (throw (ex-info "Expected one result, got more" {:type    ::assertion
                                                       ::found? true})))
    result))

(defn one
  [results]
  (let [result (one-or-nil results)]
    (when-not result
      (throw (ex-info "Expected one result, got zero" {:type    ::assertion
                                                       ::found? false})))
    result))

(defn query-one-or-nil
  "Executes a query and returns one result or nil if none found.
   Throws an exception if query returns more than 1 result."
  [q]
  (one-or-nil (query q)))

(defn query-one
  "Executes a query and returns one result.
   Throws an exception if row count is not 1."
  [q]
  (one (query q)))

(defn query-one-field-or-nil
  "Executes a query and returns the only field of result or nil if none found.
   Throws an exception if query returns more than 1 result."
  [q]
  (first (one-or-nil (queryv q))))

(defn query-one-field
  "Executes a query and returns the only field of result or nil if none found.
   Throws an exception if query returns more than 1 result."
  [q]
  (first (one (queryv q))))

(defn query-field
  "Executes a query that has only one field per row.
   Returns a vector of the values of that field"
  [q]
  (mapv first (queryv q)))
