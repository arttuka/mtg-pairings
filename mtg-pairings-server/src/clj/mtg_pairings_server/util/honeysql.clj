(ns mtg-pairings-server.util.honeysql
  (:require [honeysql.core :as sql]
            [honeysql.format :as format]
            [honeysql.helpers :as helpers]))

(defmethod format/format-clause :not-exists [[_ table-expr] _]
  (str "NOT EXISTS " (format/to-sql table-expr)))

(defmethod format/format-clause :using [[_ tables] _]
  (str "USING " (format/comma-join (map format/to-sql tables))))

(format/register-clause! :using 81)

(defn using [m & tables]
  (assoc m :using (helpers/collify tables)))
