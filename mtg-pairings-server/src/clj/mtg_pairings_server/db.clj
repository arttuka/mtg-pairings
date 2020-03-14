(ns mtg-pairings-server.db
  (:require [next.jdbc :as jdbc]
            [next.jdbc.sql :as sql]
            [next.jdbc.sql.builder :as builder]
            [next.jdbc.connection :as connection]
            [next.jdbc.prepare :as p]
            [next.jdbc.result-set :as rs]
            [honeysql.core :as honeysql]
            [honeysql.format :as format]
            [clj-time.core :as time]
            [clj-time.coerce :as tc]
            [mount.core :refer [defstate]]
            [config.core :refer [env]]
            [mtg-pairings-server.util.honeysql])
  (:import (java.sql Date PreparedStatement Timestamp)
           (org.joda.time LocalDate DateTime)
           (org.postgresql.jdbc PgArray)
           (com.zaxxer.hikari HikariDataSource)))

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

(def db-spec {:dbtype   "postgresql"
              :username (env :db-user)
              :password (env :db-password)
              :dbname   (env :db-name)
              :host     (env :db-host)
              :port     (env :db-port)})

(defstate ^{:on-reload :noop} db
  :start (connection/->pool HikariDataSource db-spec)
  :stop (.close ^HikariDataSource @db))

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

(defn insert! [table row]
  (sql/insert! *tx* table row {:builder-fn rs/as-unqualified-lower-maps
                               :column-fn #(str \" % \")}))

(defn insert-multi! [table cols rows]
  (sql/insert-multi! *tx* table cols rows {:builder-fn rs/as-unqualified-lower-maps
                                           :column-fn #(str \" % \")}))

(defn update! [table v where]
  (sql/update! *tx* table v where {:column-fn #(str \" % \")}))

(defn update-one!
  "Updates exactly one row. Throws an exception if row count is not 1."
  [table v where]
  (let [query (builder/for-update table v where {:suffix "RETURNING *"
                                                 :column-fn #(str \" % \")})
        results (jdbc/execute! *tx* query {:builder-fn rs/as-unqualified-lower-maps})]
    (assert (= (count results) 1) (str "Expected one updated row, got " (count results)))
    (first results)))

(defn delete! [table where]
  (sql/delete! *tx* table where {:column-fn #(str \" % \")}))

(defn delete-one!
  "Deletes exactly one row. Throws an exception if row count is not 1."
  [table where]
  (let [count (::jdbc/update-count (delete! table where))]
    (assert (= count 1) (str "Expected one deleted row, got " count))
    count))

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

(defn query-one-field
  "Executes a query and returns the only field of result or nil if none found.
   Throws an exception if query returns more than 1 result."
  [q]
  (first (one-or-nil (queryv q))))

(defn query-field
  ([q]
   (mapv first (queryv q)))
  ([q k]
   (mapv k (query q))))
