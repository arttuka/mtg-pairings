(ns mtg-pairings-server.util.sql
  (:require [korma.core :as sql]
            [korma.db :as db]
            [korma.sql.engine :as eng]))

(defn unique-or-nil
  [results]
  (let [[result & more] results]
    (when (seq more)
      (throw (ex-info "Expected one result, got more" {:type    ::assertion
                                                       ::found? true})))
    result))

(defn unique
  [results]
  (let [result (unique-or-nil results)]
    (when-not result
      (throw (ex-info "Expected one result, got zero" {:type    ::assertion
                                                       ::found? false})))
    result))

(defmacro select-unique-or-nil
  "Wraps korma.core/select, returns unique result or nil if none found. Throws if result is not unique."
  [entity & body]
  `(sql/select ~entity ~@body (sql/post-query unique-or-nil)))

(defmacro select-unique
  "Wraps korma.core/select, returns unique result. Throws if result is not unique."
  [entity & body]
  `(sql/select ~entity ~@body (sql/post-query unique)))

(defmacro update-unique
  "Wraps korma.core/update, updates exactly one row. Throws if row count is not 1. Returns the number of updated rows (1)."
  [entity & body]
  `(db/transaction
    (let [count# (-> (sql/update* ~entity)
                     ~@body
                     (dissoc :results)
                     sql/exec)]
      (assert (= count# 1) (str "Expected one updated row, got " count#))
      count#)))

(defmacro delete-unique
  "Wraps korma.core/delete, deletes exactly one row. Throws if row count is not 1. Returns the number of deleted rows (1)."
  [entity & body]
  `(db/transaction
    (let [count# (-> (sql/delete* ~entity)
                     ~@body
                     (dissoc :results)
                     sql/exec)]
      (assert (= count# 1) (str "Expected one deleted row, got " count#))
      count#)))

(defn like
  [k v]
  (eng/infix k "LIKE" v))
