(ns mtg-pairings-server.service.decklist
  (:require [clojure.string :as str]
            [clojure.set :refer [rename-keys]]
            [clj-time.core :as time]
            [honeysql.helpers :as sql]
            [mtg-pairings-server.db :as db]
            [mtg-pairings-server.util :refer [indexed]]
            [mtg-pairings-server.util.decklist :refer [types->keyword-set]])
  (:import (java.util Base64 Random)))

(let [random (Random.)
      encoder (.withoutPadding (Base64/getUrlEncoder))]
  (defn generate-id []
    (let [bytes (byte-array 16)]
      (.nextBytes random bytes)
      (.encodeToString encoder bytes))))

(defn search-cards [prefix format]
  (let [name-param (-> prefix
                       (str/replace "%" "")
                       (str/lower-case)
                       (str \%))
        cards (-> (sql/select :name :types)
                  (sql/from :trader_card)
                  (sql/where [:like :%lower.name name-param]
                             format)
                  (sql/order-by [:name :asc])
                  (sql/limit 10)
                  (db/query))]
    (map types->keyword-set cards)))

(defn generate-card->id [cards]
  (into {}
        (-> (sql/select :name :id)
            (sql/from :trader_card)
            (sql/where [:in :name cards])
            (db/queryv))))

(defn format-card [decklist maindeck card quantity]
  {:decklist decklist
   :maindeck maindeck
   :card     card
   :quantity quantity})

(defn get-tournament [id]
  (-> (sql/select :*)
      (sql/from :decklist_tournament)
      (sql/where [:= :id id])
      (db/query-one)
      (update :format keyword)))

(defn format-cards [cards maindeck?]
  (->> cards
       ((if maindeck? filter remove) :maindeck)
       (map types->keyword-set)
       (mapv #(select-keys % [:name :quantity :types]))))

(defn get-decklist [id]
  (when-let [decklist (-> (sql/select :id [:name :deck-name] :first-name :last-name :dci :email :tournament)
                          (sql/from :decklist)
                          (sql/where [:= :id id])
                          (db/query-one-or-nil))]
    (let [cards (-> (sql/select :maindeck :quantity :name :types)
                    (sql/from :decklist_card)
                    (sql/join :trader_card [:= :card :id])
                    (sql/where [:= :decklist id])
                    (sql/order-by [:name :asc])
                    (db/query))
          main (format-cards cards true)
          side (format-cards cards false)
          player (select-keys decklist [:deck-name :first-name :last-name :email :dci])
          email-disabled? (not (str/blank? (:email player)))]
      {:id         id
       :tournament (:tournament decklist)
       :main       main
       :side       side
       :count      {:main (transduce (map :quantity) + 0 main)
                    :side (transduce (map :quantity) + 0 side)}
       :board      :main
       :player     (assoc player :email-disabled? email-disabled?)})))

(defn save-decklist [tournament decklist]
  (let [old-id (:id decklist)
        old-decklist (some-> old-id (get-decklist))
        new-id (generate-id)
        id (or old-id new-id)
        card->id (generate-card->id (map :name
                                         (concat (:main decklist)
                                                 (:side decklist))))
        decklist (-> (:player decklist)
                     (rename-keys {:deck-name :name})
                     (select-keys [:name :first-name :last-name :email :dci])
                     (assoc :id id
                            :tournament tournament
                            :submitted (time/now)))]
    (if old-id
      (do
        (-> (sql/update :decklist)
            (sql/sset decklist)
            (sql/where [:= :id old-id])
            (db/query-one))
        (-> (sql/delete-from :decklist_card)
            (sql/where [:= :decklist old-id])
            (db/query)))
      (-> (sql/insert-into :decklist)
          (sql/values [decklist])
          (db/query)))
    (-> (sql/insert-into :decklist_card)
        (sql/values (concat
                     (for [{:keys [name quantity]} (:main decklist)]
                       {:decklist id
                        :maindeck true
                        :card     (card->id name)
                        :quantity quantity})
                     (for [{:keys [name quantity]} (:side decklist)]
                       {:decklist id
                        :maindeck false
                        :card     (card->id name)
                        :quantity quantity})))
        (db/query))
    {:id          id
     :send-email? (and (not (str/blank? (get-in decklist [:player :email])))
                       (str/blank? (get-in old-decklist [:player :email])))}))

(defn get-organizer-tournaments [user-id]
  (when user-id
    (let [tournaments (-> (sql/select :t.id :t.name :date :format :deadline [:%count.decklist.id :decklist])
                          (sql/from [:decklist_tournament :t])
                          (sql/left-join :decklist [:= :tournament :t.id])
                          (sql/where [:= :user user-id])
                          (sql/group :t.id :t.name :date :format :deadline)
                          (sql/order-by [:date :desc])
                          (db/query))]
      (map #(update % :format keyword) tournaments))))

(defn get-organizer-tournament [id]
  (let [tournament (-> (sql/select :id :user :name :date :format :deadline)
                       (sql/from :decklist_tournament)
                       (sql/where [:= :id id])
                       (db/query-one))
        decklists (-> (sql/select :id :first-name :last-name :dci :submitted)
                      (sql/from :decklist)
                      (sql/where [:= :tournament id])
                      (sql/order-by [:submitted :asc])
                      (db/query))]
    (-> tournament
        (update :format keyword)
        (assoc :decklist decklists))))

(defn format-saved-tournament [tournament]
  (-> tournament
      (select-keys [:name :date :format :deadline])
      (update :format name)))

(defn save-organizer-tournament [user-id tournament]
  (let [old-id (:id tournament)
        existing (when old-id
                   (-> (sql/select :*)
                       (sql/from :decklist_tournament)
                       (sql/where [:= :id old-id])
                       (db/query-one-or-nil)))
        new-id (generate-id)]
    (cond
      (not existing) (do
                       (-> (sql/insert-into :decklist_tournament)
                           (sql/values [(assoc (format-saved-tournament tournament)
                                               :id new-id
                                               :user user-id)])
                           (db/query))
                       new-id)
      (= user-id (:user existing)) (do
                                     (-> (sql/update :decklist_tournament)
                                         (sql/sset (format-saved-tournament tournament))
                                         (sql/where [:= :id old-id])
                                         (db/query-one))
                                     old-id))))

(defn parse-decklist-row [row format]
  (when-not (or (str/blank? row)
                (re-matches #"^[Mm]aindeck.*" row))
    (if-let [[_ quantity name] (re-matches #"(\d+)\s+(.+)" (str/trim row))]
      (if-let [{:keys [name legal types]} (-> (sql/select :name [format :legal] :types)
                                              (sql/from :trader_card)
                                              (sql/where [:= :%lower.name (str/lower-case name)])
                                              (db/query-one-or-nil)
                                              (types->keyword-set))]
        (if legal
          {:name     name
           :quantity (Long/parseLong quantity)
           :types    (set types)}
          {:name  name
           :error :card-not-in-format})
        {:name  name
         :error :card-not-found})
      {:name  row
       :error :invalid-row})))

(defn load-text-decklist [text-decklist format]
  {:pre [(contains? #{:standard :pioneer :modern :legacy} format)]}
  (let [[maindeck sideboard] (str/split text-decklist #"[Ss]ideboard( \(\d*\))?\s*")
        maindeck-cards (keep #(parse-decklist-row % format) (str/split-lines maindeck))
        sideboard-cards (when sideboard
                          (keep #(parse-decklist-row % format) (str/split-lines sideboard)))]
    {:main  (vec maindeck-cards)
     :side  (vec (or sideboard-cards []))
     :count {:main (transduce (keep :quantity) + 0 maindeck-cards)
             :side (transduce (keep :quantity) + 0 sideboard-cards)}
     :board :main}))
