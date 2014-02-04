(ns mtg-pairings-server.sql-db
  (:require [korma.core :as sql]
            korma.db
            [clojure.walk :refer [postwalk]]
            [clojure.tools.reader.edn :as edn]
            [clj-time.core :as time]
            [clj-time.coerce :as time-coerce]
            [mtg-pairings-server.mtg-util :as mtg-util]
            [mtg-pairings-server.util :as util]))

(defn ^:private create-korma-db
  [db-properties]
  (korma.db/default-connection
    (korma.db/create-db
      (korma.db/postgres db-properties))))

(defn ^:private convert-instances-of
  [cls f m]
  (postwalk (fn [x]
              (if (instance? cls x)
                (f x)
                x))
            m))

(defn ^:private to-local-date-default-tz
  [date]
  (let [dt (time-coerce/to-date-time date)]
    (time-coerce/to-local-date (time/to-time-zone dt (time/default-time-zone)))))

(def sql-date->joda-date
  (partial convert-instances-of java.sql.Date to-local-date-default-tz))

(def joda-date->sql-date
  (partial convert-instances-of org.joda.time.LocalDate time-coerce/to-sql-date))

(declare tournament player team round pairing result standings team-players)

(sql/defentity team-players
  (sql/table :team_players))

(sql/defentity tournament
  (sql/pk :id)
  (sql/has-many team
    {:fk :tournament})
  (sql/has-many round
    {:fk :tournament})
  (sql/has-one standings
    {:fk :tournament})
  (sql/prepare joda-date->sql-date)
  (sql/transform sql-date->joda-date))

(sql/defentity player
  (sql/pk :dci)
  (sql/many-to-many team
    :team_players
    {:lfk :player, :rfk :team}))

(sql/defentity team
  (sql/pk :id)
  (sql/belongs-to tournament
    {:fk :tournament})
  (sql/many-to-many player
    :team_players
    {:lfk :team, :rfk :player}))

(sql/defentity round
  (sql/pk :id)
  (sql/belongs-to tournament
    {:fk :tournament})
  (sql/has-many pairing
    {:fk :round}))

(sql/defentity team1
  (sql/pk :id)
  (sql/table :team :team1))

(sql/defentity team2
  (sql/pk :id)
  (sql/table :team :team2))

(sql/defentity pairing
  (sql/pk :id)
  (sql/belongs-to team1
    {:fk :team1})
  (sql/belongs-to team2
    {:fk :team2})
  (sql/belongs-to round
    {:fk :round})
  (sql/has-one result
    {:fk :pairing}))

(sql/defentity result
  (sql/pk :pairing)
  (sql/belongs-to pairing
    {:fk :pairing}))

(sql/defentity standings
  (sql/pk :tournament)
  (sql/belongs-to tournament
    {:fk :tournament}))

(defn ^:private sql-tournament [id]
  (first 
    (sql/select tournament
     (sql/where {:id id}))))

(defn ^:private sql-tournaments []
  (sql/select tournament
    (sql/order :day :DESC)
    (sql/order :name :ASC)))

(defn ^:private sql-player [dci]
  (first
    (sql/select player
      (sql/where {:dci dci}))))

(defn ^:private sql-add-tournament [tourn]
  (let [tourn (sql/insert tournament
                (sql/values (select-keys tourn [:name :day :rounds])))]
    (:id tourn)))

(defn sql-add-players [players]
  (let [old-players (->> (sql/select player)
                      (map :dci)
                      set)
        new-players (->> players
                      (remove #(old-players (:dci %)))
                      (map #(select-keys % [:name :dci])))]
    (when (seq new-players) 
      (sql/insert player
        (sql/values new-players)))))

(defn sql-add-team [name tournament-id]
  (let [t (sql/insert team
            (sql/values {:name name
                         :tournament tournament-id}))]
    (:id t)))

(defn ^:private sql-add-teams [tournament-id teams]
  (sql-add-players (mapcat :players teams))
  (doseq [team teams
          :let [team-id (sql-add-team (:name team) tournament-id)]]
    (sql/insert team-players
      (sql/values (for [player (:players team)]
                    {:team team-id
                     :player (:dci player)})))))

(defn ^:private sql-standings [tournament-id round-num]
  (-> (sql/select standings
       (sql/where {:tournament tournament-id
                   :round round-num}))
      first
      :standings
      edn/read-string))

(defn ^:private sql-add-round [tournament-id round-num]
  (let [round (sql/insert round 
                (sql/values {:tournament tournament-id
                             :num round-num}))]
    (:id round)))

(defn ^:private sql-add-pairings [tournament-id round-num pairings]
  (let [teams (sql/select team
                (sql/where {:tournament tournament-id}))
        name->id (into {} (for [team teams]
                            [(:name team) (:id team)]))
        team->points (if-let [standings (sql-standings tournament-id (dec round-num))]
                       (into {} (for [row standings]
                                  [(:team row) (:points row)]))
                       (constantly 0))
        round-id (sql-add-round tournament-id round-num)]
    (sql/insert pairing
      (sql/values (for [pairing pairings
                        :let [team1 (name->id (:team1 pairing))
                              team2 (name->id (:team2 pairing))]]
                    {:round round-id
                     :team1 team1
                     :team2 team2
                     :team1_points (team->points team1)
                     :team2_points (team->points team2)})))))

(defn ^:private sql-find-pairing [round-id team1 team2]
  (first (sql/select pairing
           (sql/where {:round round-id
                       :team1 team1
                       :team2 team2}))))

(defn ^:private sql-add-results [tournament-id round-num results]
  (let [round-id (:id (first (sql/select round
                               (sql/where {:tournament tournament-id
                                           :num round-num}))))
        name->id (into {} (for [team (sql/select team
                                       (sql/where {:tournament tournament-id}))]
                            [(:name team) (:id team)]))]
    (doseq [res results
            :let [pairing-id (:id (sql-find-pairing round-id 
                                                    (name->id (:team1 res))
                                                    (name->id (:team2 res))))]]
      (sql/insert result
        (sql/values {:pairing pairing-id
                     :team1_wins (:wins res)
                     :team2_wins (:losses res)
                     :draws (:draws res)})))))

(defn ^:private results-of-round [round-id]
  (sql/select pairing
    (sql/fields [:team1 :team-1] 
                [:team2 :team-2])
    (sql/with team1
      (sql/fields [:name :team-1-name]))
    (sql/with team2
      (sql/fields [:name :team-2-name]))
    (sql/with result
      (sql/fields [:team1_wins :wins] 
                  [:team2_wins :losses]
                  :draws))
    (sql/where {:round round-id})))

(defn ^:private sql-calculate-standings [tournament-id]
  (let [rounds (sql/select round
                 (sql/where {:tournament tournament-id})
                 (sql/order :num :DESC))
        rounds-results (into {} (for [r rounds]
                                  [(:num r) (results-of-round (:id r))]))
        round-num (:num (first rounds))
        round-id (:id (first rounds))
        _ (clojure.pprint/pprint rounds-results)
        std (mtg-util/calculate-standings rounds-results round-num)]
    (sql/insert standings 
      (sql/values {:standings (pr-str std)
                   :tournament tournament-id
                   :round round-id}))))

(defrecord ^:private SqlDb []
  mtg-pairings-server.db/DB
  (tournament [this id]
    (sql-tournament id))
  (tournaments [this]
    (sql-tournaments))
  (player [this dci]
    (sql-player dci))
  (add-tournament [this tournament]
    (sql-add-tournament tournament))
  (add-teams [this tournament-id teams]
    (sql-add-teams tournament-id teams))
  (add-pairings [this tournament-id round-num pairings]
    (sql-add-pairings tournament-id round-num pairings))
  (add-results [this tournament-id round-num results]
    (sql-add-results tournament-id round-num results)
    (sql-calculate-standings tournament-id)))

(defn create-db [db-properties]
  (create-korma-db db-properties)
  (->SqlDb))
