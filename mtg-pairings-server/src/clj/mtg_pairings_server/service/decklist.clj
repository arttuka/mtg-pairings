(ns mtg-pairings-server.service.decklist
  (:require [clojure.string :as str]
            [clojure.set :refer [rename-keys]]
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
     (if old-id
       (do
         (sql-util/update-unique db/decklist
           (sql/set-fields (-> (:player decklist)
                               (rename-keys {:deck-name :name})
                               (select-keys [:name :first-name :last-name :email :dci])
                               (assoc :submitted (time/now))))
           (sql/where {:id old-id}))
         (sql/delete db/decklist-card
           (sql/where {:decklist old-id})))
       (sql/insert db/decklist
         (sql/values (-> (:player decklist)
                         (rename-keys {:deck-name :name})
                         (select-keys [:name :first-name :last-name :email :dci])
                         (assoc :id new-id
                                :tournament tournament
                                :submitted (time/now))))))
     (sql/insert db/decklist-card
       (sql/values
        (concat
         (for [[index {:keys [name quantity]}] (indexed (:main decklist))]
           (format-card (or old-id new-id) true (card->id name) index quantity))
         (for [[index {:keys [name quantity]}] (indexed (:side decklist))]
           (format-card (or old-id new-id) false (card->id name) index quantity)))))
     (or old-id new-id))))

(defn get-tournament [id]
  (sql-util/select-unique db/decklist-tournament
    (sql/where {:id id})))

(defn format-cards [cards maindeck?]
  (->> cards
       ((if maindeck? filter remove) :maindeck)
       (sort-by :index)
       (mapv #(select-keys % [:name :quantity]))))

(defn get-decklist [id]
  (let [decklist (sql-util/select-unique db/decklist
                   (sql/fields :id [:name :deck-name] :first-name :last-name :dci :email :tournament)
                   (sql/with db/decklist-card
                     (sql/fields :maindeck :index :quantity)
                     (sql/with db/card
                       (sql/fields :name)))
                   (sql/where {:id id}))
        main (format-cards (:decklist_card decklist) true)
        side (format-cards (:decklist_card decklist) false)
        player (select-keys decklist [:deck-name :first-name :last-name :email :dci])]
    {:id         id
     :tournament (:tournament decklist)
     :main       main
     :side       side
     :count      {:main (transduce (map :quantity) + 0 main)
                  :side (transduce (map :quantity) + 0 side)}
     :board      :main
     :player     player}))

(defn get-organizer-tournaments []
  (let [tournaments (sql/select db/decklist-tournament
                      (sql/fields :id :name :date :format :deadline)
                      (sql/with db/decklist
                        (sql/fields :id)))]
    (map #(update % :decklist count) tournaments)))

(defn get-organizer-tournament [id]
  (sql-util/select-unique db/decklist-tournament
    (sql/fields :id :name :date :format :deadline)
    (sql/with db/decklist
      (sql/fields :id :first-name :last-name :dci :submitted)
      (sql/order :submitted :asc))
    (sql/where {:id id})))

(defn format-saved-tournament [tournament]
  (-> tournament
      (select-keys [:name :date :format :deadline])
      (update :format name)))

(defn save-organizer-tournament [tournament]
  (let [old-id (:id tournament)
        new-id (sql-util/generate-id)]
    (if old-id
      (sql-util/update-unique db/decklist-tournament
        (sql/set-fields (format-saved-tournament tournament))
        (sql/where {:id old-id}))
      (sql/insert db/decklist-tournament
        (sql/values (-> (format-saved-tournament tournament)
                        (assoc :id new-id)))))
    (or old-id new-id)))
