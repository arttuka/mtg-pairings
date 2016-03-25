(ns mtg-pairings-server.tournaments
  (:require [korma.core :as sql]
            [mtg-pairings-server.sql-db :as db]
            [mtg-pairings-server.mtg-util :as mtg-util]
            [mtg-pairings-server.util :as util]
            [clojure.tools.reader.edn :as edn]))

(defn user-for-apikey [apikey]
  (try
    (-> (sql/select db/user
          (sql/fields :id)
          (sql/where {:uuid (java.util.UUID/fromString apikey)}))
      first
      :id)
    (catch IllegalArgumentException e
      nil)))

(defn owner-of-tournament [sanction-id]
  (:owner
    (first
      (sql/select db/tournament
        (sql/fields :owner)
        (sql/where {:sanctionid sanction-id})))))

(defn sanctionid->id [sanction-id]
  (:id
    (first
      (sql/select db/tournament
        (sql/fields :id)
        (sql/where {:sanctionid sanction-id})))))

(def ^:private select-tournaments
  (-> (sql/select* db/tournament)
    (sql/fields :rounds :day :name :id
                (sql/raw "exists (select 1 from seating where \"tournament\" = \"tournament\".\"id\") as \"seatings\""))
    (sql/order :day :DESC)
    (sql/order :name :ASC)
    (sql/with db/round
      (sql/fields :num)
      (sql/fields [(sql/sqlfn "not exists" (sql/subselect db/pairing
                                             (sql/join :left db/result
                                                       (= :pairing.id :result.pairing))
                                             (sql/where {:pairing.round :round.id
                                                         :result.pairing nil})))
                   :results])
      (sql/order :num)
      (sql/where
        (sql/sqlfn "exists" (sql/subselect db/pairing
                              (sql/where {:round :round.id})))))
    (sql/with db/standings
      (sql/where {:hidden false})
      (sql/fields [:round :num])
      (sql/order :round))))

(defn ^:private update-round-data [tournament]
  (let [rounds (:round tournament)
        pairings (map :num rounds)
        results (map :num (filter :results rounds))]
    (-> tournament
      (assoc :pairings pairings)
      (assoc :results results)
      (update-in [:standings] #(map :num %))
      (dissoc :round))))

(defn tournament [id]
  (update-round-data (first
                       (-> select-tournaments
                         (sql/where {:id id})
                         (sql/exec)))))

(defn tournaments []
  (let [tourns (sql/exec select-tournaments)]
    (map update-round-data tourns)))

(defn add-tournament [tourn]
  (when-not (first (sql/select db/tournament
                     (sql/where {:sanctionid (:sanctionid tourn)})))
    (sql/insert db/tournament
      (sql/values (select-keys tourn [:name :day :sanctionid :rounds :owner])))))

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

(defn ^:private fix-dci-number [player]
  (update-in player [:dci] mtg-util/add-check-digits))

(defn ^:private fix-dci-numbers [team]
  (update-in team [:players] #(map fix-dci-number %)))

(defn ^:private delete-pods [tournament-id]
  (sql/exec-raw ["DELETE FROM pod_seat USING pod JOIN pod_round ON pod.pod_round = pod_round.id WHERE pod_round.tournament = ? AND pod = pod.id" [tournament-id]])
  (sql/exec-raw ["DELETE FROM pod USING pod_round WHERE pod_round.tournament = ? AND pod_round = pod_round.id" [tournament-id]])
  (sql/delete db/pod-round
    (sql/where {:tournament tournament-id})))

(defn ^:private delete-teams [tournament-id]
  (sql/delete db/team-players
    (sql/where (sql/sqlfn "exists" (sql/subselect db/team
                                     (sql/where {:team_players.team :team.id
                                                 :team.tournament tournament-id})))))
  (sql/delete db/team
    (sql/where {:tournament tournament-id})))

(defn ^:private delete-rounds [tournament-id]
  (sql/delete db/result
    (sql/where (sql/sqlfn "exists" (sql/subselect db/pairing
                                     (sql/where {:id :result.pairing})
                                     (sql/with db/round
                                       (sql/where {:tournament tournament-id}))))))
  (sql/delete db/pairing
    (sql/where (sql/sqlfn "exists" (sql/subselect db/round
                                     (sql/where {:tournament tournament-id
                                                 :id :pairing.round})))))
  (sql/delete db/round
    (sql/where {:tournament tournament-id})))

(defn ^:private delete-seatings [tournament-id]
  (sql/delete db/seating
    (sql/where {:tournament tournament-id})))

(defn add-teams [sanction-id teams]
  (let [tournament-id (sanctionid->id sanction-id)]
    (delete-seatings tournament-id)
    (delete-rounds tournament-id)
    (delete-pods tournament-id)
    (delete-teams tournament-id)
    (let [teams (map fix-dci-numbers teams)]
      (add-players (mapcat :players teams))
      (doseq [team teams
              :let [team-id (add-team (:name team) tournament-id)]]
        (sql/insert db/team-players
          (sql/values (for [player (:players team)]
                        {:team team-id
                         :player (:dci player)})))))))

(defn seatings [tournament-id]
  (sql/select db/seating
    (sql/fields :table_number)
    (sql/with db/team
      (sql/fields :name))
    (sql/where {:tournament tournament-id})
    (sql/order :team.name :ASC)))

(defn standings [tournament-id round-num secret]
  (-> (sql/select db/standings
       (sql/where (and {:tournament tournament-id
                        :round round-num}
                       (or (= secret "secret")
                           (not :hidden)))))
      first
      :standings
      edn/read-string))

(defn standings-for-api [tournament-id round-num secret]
  (map #(select-keys % [:rank :team_name :points :omw :pgw :ogw Double])
       (standings tournament-id round-num secret)))

(defn ^:private get-or-add-round [tournament-id round-num]
  (if-let [old-round (first (sql/select db/round
                              (sql/where {:tournament tournament-id
                                          :num round-num})))]
    (:id old-round)
    (:id (sql/insert db/round
           (sql/values {:tournament tournament-id
                        :num round-num})))))

(defn teams-by-dci [tournament-id]
  (let [team-players (sql/select db/team-players
                       (sql/with db/team
                         (sql/where {:tournament tournament-id})))
        team->players (group-by :team team-players)
        dci->players (into {} (for [[team players] team->players]
                                [(set (map :player players)) team]))]
    (fn [dcis] (dci->players (set (map mtg-util/add-check-digits dcis))))))

(defn add-seatings [sanction-id seatings]
  (let [tournament-id (sanctionid->id sanction-id)]
    (sql/delete db/seating
      (sql/where {:tournament tournament-id}))
    (let [dci->id (teams-by-dci tournament-id)]
      (sql/insert db/seating
        (sql/values (for [seating seatings]
                      {:team (dci->id (:team seating))
                       :table_number (:table_number seating)
                       :tournament tournament-id}))))))

(defn ^:private delete-results [round-id]
  (sql/delete db/result
    (sql/where {:pairing [in (sql/subselect db/pairing
                               (sql/fields :id)
                               (sql/where {:round round-id}))]})))

(defn ^:private delete-pairings [round-id]
  (sql/delete db/pairing
    (sql/where {:round round-id})))

(defn ^:private delete-standings
  ([tournament-id]
    (sql/delete db/standings
      (sql/where {:tournament tournament-id})))
  ([tournament-id round-num]
    (sql/delete db/standings
      (sql/where {:tournament tournament-id
                  :round round-num}))))

(defn add-pairings [sanction-id round-num pairings]
  (let [tournament-id (sanctionid->id sanction-id)
        dci->id (teams-by-dci tournament-id)
        team->points (if-let [standings (standings tournament-id (dec round-num) "secret")]
                       (into {} (for [row standings]
                                  [(:team row) (:points row)]))
                       (constantly 0))
        round-id (get-or-add-round tournament-id round-num)]
    (when (seq pairings)
      (delete-results round-id)
      (delete-pairings round-id)
      (delete-standings tournament-id round-num)
      (sql/insert db/pairing
        (sql/values (for [pairing pairings
                          :let [team1 (dci->id (:team1 pairing))
                                team2 (dci->id (:team2 pairing))]]
                      {:round round-id
                       :team1 team1
                       :team2 team2
                       :team1_points (team->points team1)
                       :team2_points (team->points team2 0)
                       :table_number (:table_number pairing)}))))))

(defn ^:private add-team-where [query key dcis]
  (if (seq dcis)
    (reduce (fn [q dci]
              (sql/where q (sql/sqlfn "exists" (sql/subselect db/team-players
                                                 (sql/where {:team   key
                                                             :player (mtg-util/add-check-digits dci)})))))
            query
            dcis)
    (sql/where query {:team2 nil})))

(defn ^:private find-pairing [round-id {:keys [team1 team2]}]
  (let [query (->
                (sql/select* db/pairing)
                (sql/with db/team1)
                (sql/with db/team2)
                (sql/where {:round round-id})
                (sql/limit 1))]
    (-> query
        (add-team-where :team1.id team1)
        (add-team-where :team2.id team2)
        sql/exec
        first)))

(defn ^:private results-of-round [round-id]
  (for [pairing (sql/select db/pairing
                  (sql/fields [:team1 :team1]
                              [:team2 :team2]
                              [:team1_points :team1_points]
                              [:team2_points :team2_points]
                              :table_number)
                  (sql/with db/team1
                    (sql/fields [:name :team1_name]))
                  (sql/with db/team2
                    (sql/fields [:name :team2_name]))
                  (sql/with db/round
                    (sql/fields [:num :round_number]))
                  (sql/with db/result
                    (sql/fields :team1_wins
                                :team2_wins
                                :draws))
                  (sql/where {:round round-id}))]
    (if-not (:team2 pairing)
      (merge pairing {:team2_name "***BYE***"
                      :team2_points 0})
      pairing)))

(defn ^:private calculate-standings [tournament-id round]
  (let [rounds (sql/select db/round
                 (sql/where {:tournament tournament-id
                             :num [<= round]})
                 (sql/order :num :DESC))
        rounds-results (into {} (for [r rounds]
                                  [(:num r) (results-of-round (:id r))]))
        round-num (:num (first rounds))
        round-id (:id (first rounds))]
    (when (every? :team1_wins (rounds-results round-num))
      (let [std (mtg-util/calculate-standings rounds-results round-num)]
        (sql/insert db/standings
          (sql/values {:standings  (pr-str std)
                       :tournament tournament-id
                       :round      round-num
                       :hidden     false}))))))

(defn add-results [sanction-id round-num results]
  (let [tournament-id (sanctionid->id sanction-id)
        round-id (:id (first (sql/select db/round
                               (sql/where {:tournament tournament-id
                                           :num round-num}))))]
    (when (seq results)
      (delete-results round-id)
      (delete-standings tournament-id round-num)
      (doseq [res results
              :let [pairing-id (:id (find-pairing round-id res))]]
        (sql/insert db/result
          (sql/values {:pairing pairing-id
                       :team1_wins (:team1_wins res)
                       :team2_wins (:team2_wins res)
                       :draws (:draws res)})))
      (calculate-standings tournament-id round-num))))

(defn publish-results [sanction-id round-num]
  (let [tournament-id (sanctionid->id sanction-id)]
    (sql/update db/standings
      (sql/set-fields {:hidden false})
      (sql/where {:tournament tournament-id
                  :round round-num}))))

(defn get-round [tournament-id round-num]
  (let [round-id (:id (first (sql/select db/round
                               (sql/where {:tournament tournament-id
                                           :num round-num}))))]
    (map #(dissoc % :team1 :team2) (results-of-round round-id))))

(defn add-pods [sanction-id pods]
  (let [tournament-id (sanctionid->id sanction-id)
        dci->id (teams-by-dci tournament-id)]
    (delete-pods tournament-id)
    (doseq [pod-round pods
            :let [r (sql/insert db/pod-round
                      (sql/values {:tournament tournament-id}))]
            pod (:pods pod-round)
            :let [p (sql/insert db/pod
                      (sql/values {:pod_round (:id r)
                                   :number (:number pod)}))]
            seat (:seats pod)]
      (sql/insert db/seat
        (sql/values {:pod (:id p)
                     :seat (:seat seat)
                     :team (dci->id (:team seat))})))))

(defn reset-tournament [sanction-id]
  (let [tournament-id (sanctionid->id sanction-id)]
    (doseq [round (doall (map :id (sql/select db/round
                                    (sql/where {:tournament tournament-id})
                                    (sql/order :num :DESC))))]
      (delete-results round)
      (delete-pairings round)
      (sql/delete db/round
        (sql/where {:id round})))
    (delete-seatings tournament-id)
    (delete-pods tournament-id)
    (delete-teams tournament-id)
    (delete-standings tournament-id)))