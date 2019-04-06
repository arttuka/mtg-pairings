(ns mtg-pairings-server.service.card
  (:require [korma.core :as sql]
            [mtg-pairings-server.sql-db :as db]
            [mtg-pairings-server.util.sql :refer [like]]
            [clojure.string :as str]))

(defn search [prefix format]
  (let [name-param (-> prefix
                       (str/replace "%" "")
                       (str/lower-case)
                       (str \%))
        result (cond-> (-> (sql/select* db/card)
                           (sql/fields :name)
                           (sql/where {:lowername [like name-param]})
                           (sql/order :lowername :asc)
                           (sql/limit 10))
                 (= :standard format) (sql/where {:standard true})
                 (= :modern format) (sql/where {:modern true})
                 (= :legacy format) (sql/where {:legacy true})
                 true (sql/exec))]
    (map :name result)))
