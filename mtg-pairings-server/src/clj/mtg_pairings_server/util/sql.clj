(ns mtg-pairings-server.util.sql
  (:require [korma.core :as sql]))

(defn unique-or-nil
  [results]
  (let [[result & more] results]
    (assert (empty? more) "Expected one result, got more")
    result))

(defn unique
  [results]
  (let [result (unique-or-nil results)]
    (assert result "Expected one result, got zero")
    result))

(defn select-unique-or-nil*
  "Replaces korma.core/select*, creates a query that returns unique result or nil if none found. Throws if result is not unique."
  [entity]
  (-> (sql/select* entity) (sql/post-query unique-or-nil)))

(defn select-unique*
  "Replaces korma.core/select*, creates a query that returns unique result. Throws if result is not unique."
  [entity]
  (-> (sql/select* entity) (sql/post-query unique)))

(defmacro select-unique-or-nil
  "Wraps korma.core/select, returns unique result or nil if none found. Throws if result is not unique."
  [entity & body]
  `(sql/select ~entity (sql/post-query unique-or-nil) ~@body))

(defmacro select-unique
  "Wraps korma.core/select, returns unique result. Throws if result is not unique."
  [entity & body]
  `(sql/select ~entity (sql/post-query unique) ~@body))

(defmacro update-unique
  "Wraps korma.core/update, updates exactly one row. Throws if row count is not 1. Returns the number of updated rows (1)."
  [entity & body]
  `(let [count# (-> (sql/update* ~entity)
                  ~@body
                  (dissoc :results)
                  sql/exec)
         count# (if (sequential? count#) ;; JDBC:n vanha versio palauttaa vektorin, uusi pelkän luvun
                  (first count#)
                  count#)]
     (assert (= count# 1) (str "Expected one updated row, got " count#))
     count#))

(defmacro delete-unique
  "Wraps korma.core/delete, deletes exactly one row. Throws if row count is not 1. Returns the number of deleted rows (1)."
  [entity & body]
  `(let [count# (-> (sql/delete* ~entity)
                  ~@body
                  (dissoc :results)
                  sql/exec)
         count# (if (sequential? count#) ;; JDBC:n vanha versio palauttaa vektorin, uusi pelkän luvun
                  (first count#)
                  count#)]
     (assert (= count# 1) (str "Expected one deleted row, got " count#))
     count#))
