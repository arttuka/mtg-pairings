(ns mtg-pairings-server.service.decklist
  (:require [clojure.string :as str]
            [clj-time.core :as time]
            [korma.core :as sql]
            [korma.db :refer [transaction]]
            [mtg-pairings-server.sql-db :as db]
            [mtg-pairings-server.util :refer [indexed]]
            [mtg-pairings-server.util.sql :as sql-util :refer [like]]))

(defn search-cards [prefix format]
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

(defn generate-card->id [cards]
  (into {}
        (map (juxt :name :id))
        (sql/select db/card
          (sql/where {:name [in cards]}))))

(defn format-card [decklist maindeck card index quantity]
  {:decklist decklist
   :maindeck maindeck
   :card     card
   :index    index
   :quantity quantity})

(defn save-decklist [tournament decklist]
  (transaction
   (let [old-id (:id decklist)
         new-id (sql-util/generate-id)
         card->id (generate-card->id (map :name
                                          (concat (:main decklist)
                                                  (:side decklist))))]
     (when-not old-id
       (sql/insert db/decklist
         (sql/values (-> (select-keys decklist [:name :last-name :first-name :dci :email])
                         (assoc :id new-id
                                :tournament tournament
                                :submitted (time/now))))))
     (when old-id
       (sql-util/update-unique db/decklist
         (sql/set-fields (-> (select-keys decklist [:name :last-name :first-name :dci :email])
                             (assoc :submitted (time/now))))
         (sql/where {:id old-id}))
       (sql/delete db/decklist-card
         (sql/where {:decklist old-id})))
     (sql/insert db/decklist-card
       (sql/values
        (concat
         (for [[index {:keys [name quantity]}] (indexed (:main decklist))]
           (format-card (or old-id new-id) true (card->id name) index quantity))
         (for [[index {:keys [name quantity]}] (indexed (:side decklist))]
           (format-card (or old-id new-id) false (card->id name) index quantity))))))))

(defn get-tournament [id]
  (some-> (sql-util/select-unique-or-nil db/decklist-tournament
            (sql/where {:id id}))
          (update :format keyword)))
