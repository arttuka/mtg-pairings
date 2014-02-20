(ns mtg-pairings-server.tournaments
  (:require [korma.core :as sql]
            [mtg-pairings-server.sql-db :as db]
            [mtg-pairings-server.mtg-util :as mtg-util]
            [mtg-pairings-server.util :as util]
            [clojure.tools.reader.edn :as edn]))

(defn user-for-request [request]
  (when-let [uuid (get-in request [:params "key"])]
    (try
      (-> (sql/select db/user
            (sql/fields :id)
            (sql/where {:uuid (java.util.UUID/fromString uuid)}))
        first
        :id)
      (catch IllegalArgumentException e
        nil))))

(defn owner-of-tournament [id]
  (:owner 
    (first 
      (sql/select db/tournament
        (sql/fields :owner)
        (sql/where {:id id})))))

(def ^:private select-tournaments
  (-> (sql/select* db/tournament)
    (sql/fields :rounds :day :name :id
                (sql/raw "exists (select 1 from seating where \"tournament\" = \"tournament\".\"id\") as \"seatings\""))
    (sql/order :day :DESC)
    (sql/order :name :ASC)
    (sql/with db/round
      (sql/fields :num)
      (sql/order :num)
      (sql/where
        (sql/sqlfn exists (sql/subselect db/pairing
                            (sql/where {:round :round.id})))))
    (sql/with db/standings
      (sql/fields [:round :num])
      (sql/order :round))))

(defn ^:private update-round-data [tournament]
  (-> tournament 
    (update-in [:round] #(map :num %))
    (update-in [:standings] #(map :num %))))

(defn tournament [id]
  (update-round-data (first 
                       (-> select-tournaments
                         (sql/where {:id id})
                         (sql/exec)))))

(defn tournaments []
  (let [tourns (sql/exec select-tournaments)]
    (map update-round-data tourns)))

(defn add-tournament [tourn]
  (let [tourn (sql/insert db/tournament
                (sql/values (select-keys tourn [:name :day :rounds :owner])))]
    (:id tourn)))

(defn add-players [players]
  (let [old-players (->> (sql/select db/player)
                      (map :dci)
                      set)
        new-players (->> players
                      (remove #(old-players (:dci %)))
                      (map #(select-keys % [:name :dci])))]
    (when (seq new-players) 
      (sql/insert db/player
        (sql/values new-players)))))

(defn ^:private add-team [name tournament-id]
  (let [t (sql/insert db/team
            (sql/values {:name name
                         :tournament tournament-id}))]
    (:id t)))

(defn add-teams [tournament-id teams]
  (add-players (mapcat :players teams))
  (doseq [team teams
          :let [team-id (add-team (:name team) tournament-id)]]
    (sql/insert db/team-players
      (sql/values (for [player (:players team)]
                    {:team team-id
                     :player (:dci player)})))))

(defn seatings [tournament-id]
  (sql/select db/seating
    (sql/fields :table_number)
    (sql/with db/team
      (sql/fields :name))
    (sql/where {:tournament tournament-id})
    (sql/order :team.name :ASC)))

(defn standings [tournament-id round-num]
  (-> (sql/select db/standings
       (sql/where {:tournament tournament-id
                   :round round-num}))
      first
      :standings
      edn/read-string))

(defn ^:private add-round [tournament-id round-num]
  (let [round (sql/insert db/round 
                (sql/values {:tournament tournament-id
                             :num round-num}))]
    (:id round)))

(defn teams-by-name [tournament-id]
  (let [teams (sql/select db/team
                (sql/where {:tournament tournament-id}))]
    (into {} (for [team teams]
               [(:name team) (:id team)]))))

(defn add-seatings [tournament-id seatings]
  (sql/delete db/seating
    (sql/where {:tournament tournament-id}))
  (let [name->id (teams-by-name tournament-id)] 
    (sql/insert db/seating
      (sql/values (for [seating seatings]
                    {:team (name->id (:team seating))
                     :table_number (:table_number seating)
                     :tournament tournament-id})))))

(defn add-pairings [tournament-id round-num pairings]
  (let [name->id (teams-by-name tournament-id)
        team->points (if-let [standings (standings tournament-id (dec round-num))]
                       (into {} (for [row standings]
                                  [(:team row) (:points row)]))
                       #(when % 0))
        round-id (add-round tournament-id round-num)]
    (sql/insert db/pairing
      (sql/values (for [pairing pairings
                        :let [team1 (name->id (:team1 pairing))
                              team2 (name->id (:team2 pairing))]]
                    {:round round-id
                     :team1 team1
                     :team2 team2
                     :team1_points (team->points team1)
                     :team2_points (team->points team2)
                     :table_number (:table_number pairing)})))))

(defn ^:private find-pairing [round-id table_number]
  (first (sql/select db/pairing
           (sql/where {:round round-id
                       :table_number table_number}))))

(defn ^:private results-of-round [round-id]
  (for [pairing (sql/select db/pairing
                  (sql/fields [:team1 :team_1] 
                              [:team2 :team_2]
                              [:team1_points :team_1_points]
                              [:team2_points :team_2_points]
                              :table_number)
                  (sql/with db/team1
                    (sql/fields [:name :team_1_name]))
                  (sql/with db/team2
                    (sql/fields [:name :team_2_name]))
                  (sql/with db/result
                    (sql/fields [:team1_wins :wins] 
                                [:team2_wins :losses]
                                :draws))
                  (sql/where {:round round-id}))]
    (if-not (:team_2 pairing)
      (merge pairing {:team_2_name "***BYE***"
                      :team_2_points 0})
      pairing)))

(defn ^:private calculate-standings [tournament-id]
  (let [rounds (sql/select db/round
                 (sql/where {:tournament tournament-id})
                 (sql/order :num :DESC))
        rounds-results (into {} (for [r rounds]
                                  [(:num r) (results-of-round (:id r))]))
        round-num (:num (first rounds))
        round-id (:id (first rounds))
        std (mtg-util/calculate-standings rounds-results round-num)]
    (sql/insert db/standings 
     (sql/values {:standings (pr-str std)
                  :tournament tournament-id
                  :round round-num}))))

(defn add-results [tournament-id round-num results]
  (let [round-id (:id (first (sql/select db/round
                               (sql/where {:tournament tournament-id
                                           :num round-num}))))]
    (doseq [res results
            :let [pairing-id (:id (find-pairing round-id (:table_number res)))]]
      (sql/insert db/result
        (sql/values {:pairing pairing-id
                     :team1_wins (:team1_wins res)
                     :team2_wins (:team2_wins res)
                     :draws (:draws res)})))
    (calculate-standings tournament-id)))

(defn get-round [tournament-id round-num]
  (let [round-id (:id (first (sql/select db/round
                               (sql/where {:tournament tournament-id
                                           :num round-num}))))]
    (results-of-round round-id)))