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
        player (select-keys decklist [:deck-name :first-name :last-name :email :dci])
        email-disabled? (not (str/blank? (:email player)))]
    {:id         id
     :tournament (:tournament decklist)
     :main       main
     :side       side
     :count      {:main (transduce (map :quantity) + 0 main)
                  :side (transduce (map :quantity) + 0 side)}
     :board      :main
     :player     (assoc player :email-disabled? email-disabled?)}))

(defn save-decklist [tournament decklist]
  (transaction
   (let [old-id (:id decklist)
         old-decklist (some-> old-id (get-decklist))
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
     {:id          (or old-id new-id)
      :send-email? (and (not (str/blank? (get-in decklist [:player :email])))
                        (str/blank? (get-in old-decklist [:player :email])))})))

(defn get-organizer-tournaments [user-id]
  (when user-id
    (let [tournaments (sql/select db/decklist-tournament
                        (sql/fields :id :name :date :format :deadline)
                        (sql/with db/decklist
                          (sql/fields :id))
                        (sql/where {:user user-id}))]
      (map #(update % :decklist count) tournaments))))

(defn get-organizer-tournament [id]
  (-> (sql-util/select-unique db/decklist-tournament
        (sql/fields :id :user :name :date :format :deadline)
        (sql/with db/decklist
          (sql/fields :id :first-name :last-name :dci :submitted)
          (sql/order :submitted :asc))
        (sql/where {:id id}))
      (update :decklist vec)))

(defn format-saved-tournament [tournament]
  (-> tournament
      (select-keys [:name :date :format :deadline])
      (update :format name)))

(defn save-organizer-tournament [user-id tournament]
  (let [old-id (:id tournament)
        existing (when old-id
                   (sql-util/select-unique-or-nil db/decklist-tournament
                     (sql/where {:id old-id})))
        new-id (sql-util/generate-id)]
    (cond
      (not existing) (do
                       (sql/insert db/decklist-tournament
                         (sql/values (-> (format-saved-tournament tournament)
                                         (assoc :id new-id
                                                :user user-id))))
                       new-id)
      (= user-id (:user existing)) (do
                                     (sql-util/update-unique db/decklist-tournament
                                       (sql/set-fields (format-saved-tournament tournament))
                                       (sql/where {:id old-id}))
                                     old-id))))

(defn parse-decklist-row [row format]
  (when-not (str/blank? row)
    (if-let [[_ quantity name] (re-matches #"(\d+)\s+(.+)" (str/trim row))]
      (if-let [{:keys [name legal]} (sql-util/select-unique-or-nil db/card
                                      (sql/fields :name [format :legal])
                                      (sql/where {:lowername (str/lower-case name)}))]
        (if legal
          {:name     name
           :quantity (Long/parseLong quantity)}
          {:name  name
           :error "Ei sallittu tässä formaatissa"})
        {:name  name
         :error "Korttia ei löydy"})
      {:name  row
       :error "Virheellinen rivi"})))

(defn load-text-decklist [text-decklist format]
  {:pre [(contains? #{:standard :modern :legacy} format)]}
  (let [[maindeck sideboard] (str/split text-decklist #"[Ss]ideboard\s*")
        maindeck-cards (keep #(parse-decklist-row % format) (str/split-lines maindeck))
        sideboard-cards (when sideboard
                          (keep #(parse-decklist-row % format) (str/split-lines sideboard)))]
    {:main  (vec maindeck-cards)
     :side  (vec (or sideboard-cards []))
     :count {:main (transduce (keep :quantity) + 0 maindeck-cards)
             :side (transduce (keep :quantity) + 0 sideboard-cards)}
     :board :main}))
