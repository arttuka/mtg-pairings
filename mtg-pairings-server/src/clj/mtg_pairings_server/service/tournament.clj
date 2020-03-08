(ns mtg-pairings-server.service.tournament
  (:require [clojure.set :refer [rename-keys union]]
            [clojure.edn :as edn]
            [clj-time.core :as time]
            [korma.core :as sql]
            [mtg-pairings-server.sql-db :as db]
            [mtg-pairings-server.util.mtg :as mtg-util]
            [mtg-pairings-server.util.sql :as sql-util]
            [mtg-pairings-server.util :as util])
  (:import (java.util UUID)))

(defn user-for-apikey [apikey]
  (try
    (:id (sql-util/select-unique-or-nil db/user
           (sql/fields :id)
           (sql/where {:uuid (UUID/fromString apikey)})))
    (catch IllegalArgumentException _
      nil)))

(defn owner-of-tournament [sanction-id]
  (:owner
   (sql-util/select-unique-or-nil db/tournament
     (sql/fields :owner)
     (sql/where {:sanctionid sanction-id}))))

(defn sanctionid->id [sanction-id]
  (:id
   (sql-util/select-unique-or-nil db/tournament
     (sql/fields :id)
     (sql/where {:sanctionid sanction-id}))))

(defn id->sanctionid [id]
  (:sanctionid
   (sql-util/select-unique-or-nil db/tournament
     (sql/fields :sanctionid)
     (sql/where {:id id}))))

(def ^:private select-tournaments
  (->
   (sql/select* db/tournament)
   (sql/fields :rounds :day :name :organizer :id :modified
               (sql/raw "exists (select 1 from seating where \"tournament\" = \"tournament\".\"id\") as \"seatings\"")
               (sql/raw "exists (select 1 from round where \"tournament\" = \"tournament\".\"id\" and playoff) as \"playoff\""))
   (sql/order :day :DESC)
   (sql/order :name :ASC)))

(defn ^:private update-round-data [tournament]
  (let [rounds (:round tournament)
        round-times (into {} (map (juxt :num :created)) rounds)
        pairings (map :num rounds)
        results (map :num (filter :results rounds))
        pods (range 1 (inc (count (:pod_round tournament))))]
    (->
     tournament
     (assoc :pairings pairings
            :results results
            :pods pods
            :round-times round-times)
     (update :standings #(map :num %))
     (dissoc :round :pod_round))))

(defn format-client-tournament [tournament]
  (let [pairings (set (:pairings tournament))
        standings (set (:standings tournament))
        round-nums (sort > (union pairings standings))]
    (-> tournament
        (dissoc :modified :rounds)
        (assoc :pairings pairings
               :standings standings
               :round-nums round-nums))))

(defn tournament [id]
  (->
   select-tournaments
   (sql/with db/round
     (sql/fields :num :created)
     (sql/fields [(sql/sqlfn "not exists" (sql/subselect db/pairing
                                            (sql/where {:round      :round.id
                                                        :team1_wins nil})))
                  :results])
     (sql/order :num)
     (sql/where
      (and (sql/sqlfn "exists" (sql/subselect db/pairing
                                 (sql/where {:round :round.id})))
           (not :playoff))))
   (sql/with db/standings
     (sql/where {:hidden false})
     (sql/fields [:round :num])
     (sql/order :round))
   (sql/with db/pod-round
     (sql/fields :id)
     (sql/order :id))
   (sql/join db/team)
   (sql/aggregate (count :*) :players :id)
   (sql/where {:id id})
   (sql/post-query sql-util/unique)
   (sql/exec)
   update-round-data))

(defn client-tournament [id]
  (format-client-tournament (tournament id)))

(defn tournaments [{:keys [active? modified]
                    :or   {active? false, modified nil}}]
  (let [tourns (cond-> select-tournaments
                 active? (sql/where {:day [>= (sql/raw "current_date - 1")]})
                 modified (sql/where {(sql/sqlfn :date_trunc "milliseconds" :modified) [> modified]})
                 true (sql/exec))
        ids (when (or active? modified)
              (map :id tourns))
        add-tournament-where (fn [query]
                               (cond-> query
                                 ids (sql/where {:tournament [in ids]})))
        teams (into {} (for [{:keys [tournament count]} (sql/select db/team
                                                          (sql/fields :tournament)
                                                          (sql/aggregate (count :*) :count :tournament))]
                         [tournament count]))
        rounds (util/group-kv :tournament
                              #(select-keys % [:num :results :created])
                              (-> (sql/select* db/round)
                                  (sql/fields :tournament :num :created)
                                  (sql/fields [(sql/sqlfn "not exists" (sql/subselect db/pairing
                                                                         (sql/where {:round      :round.id
                                                                                     :team1_wins nil})))
                                               :results])
                                  (sql/order :num)
                                  (sql/where
                                   (and (sql/sqlfn "exists" (sql/subselect db/pairing
                                                              (sql/where {:round :round.id})))
                                        (not :playoff)))
                                  (add-tournament-where)
                                  (sql/exec)))
        standings (util/group-kv :tournament
                                 #(select-keys % [:num])
                                 (-> (sql/select* db/standings)
                                     (sql/where {:hidden false})
                                     (sql/fields :tournament [:round :num])
                                     (sql/order :round)
                                     (add-tournament-where)
                                     (sql/exec)))
        pod-rounds (util/group-kv :tournament
                                  :id
                                  (-> (sql/select* db/pod-round)
                                      (sql/fields :tournament :id)
                                      (sql/order :id)
                                      (add-tournament-where)
                                      (sql/exec)))]
    (for [t tourns
          :let [id (:id t)]]
      (update-round-data
       (assoc t
              :round (get rounds id [])
              :standings (get standings id [])
              :pod_round (get pod-rounds id [])
              :players (get teams id 0))))))

(defn client-tournaments
  ([]
   (client-tournaments {}))
  ([opts]
   (let [tourns (tournaments opts)]
     {:tournaments (map format-client-tournament tourns)
      :modified    (when (seq tourns)
                     (reduce util/max-date (map :modified tourns)))})))

(defn organizer-tournaments []
  (for [t (-> select-tournaments
              (sql/where {:day (sql/raw "CURRENT_DATE")})
              (sql/exec))]
    (select-keys t [:id :name :organizer])))

(defn add-tournament [tourn]
  (if (seq (sql/select db/tournament
             (sql/where {:sanctionid (:sanctionid tourn)})))
    (do
      (sql-util/update-unique db/tournament
        (sql/set-fields (select-keys tourn [:name :organizer :day :rounds]))
        (sql/where {:sanctionid (:sanctionid tourn)
                    :owner      (:owner tourn)}))
      (sql-util/select-unique db/tournament
        (sql/where {:sanctionid (:sanctionid tourn)})))
    (sql/insert db/tournament
      (sql/values (select-keys tourn [:name :organizer :day :sanctionid :rounds :owner])))))

(defn save-tournament [sanctionid tourn]
  (let [tournament-id (sanctionid->id sanctionid)]
    (sql-util/update-unique db/tournament
      (sql/set-fields tourn)
      (sql/where {:id tournament-id}))))

(defn update-tournament-modified [id]
  (sql-util/update-unique db/tournament
    (sql/set-fields {:modified (sql/sqlfn :now)})
    (sql/where {:id id})))

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
  (:id (sql/insert db/team
         (sql/values {:name       name
                      :tournament tournament-id}))))

(defn ^:private fix-dci-number [player]
  (update player :dci mtg-util/add-check-digits))

(defn ^:private fix-dci-numbers [team]
  (update team :players #(map fix-dci-number %)))

(defn ^:private delete-pods [tournament-id]
  (sql/exec-raw ["DELETE FROM pod_seat USING pod JOIN pod_round ON pod.pod_round = pod_round.id WHERE pod_round.tournament = ? AND pod = pod.id" [tournament-id]])
  (sql/exec-raw ["DELETE FROM pod USING pod_round WHERE pod_round.tournament = ? AND pod_round = pod_round.id" [tournament-id]])
  (sql/delete db/pod-round
    (sql/where {:tournament tournament-id})))

(defn ^:private delete-teams [tournament-id]
  (sql/delete db/team-players
    (sql/where (sql/sqlfn "exists" (sql/subselect db/team
                                     (sql/where {:team_players.team :team.id
                                                 :team.tournament   tournament-id})))))
  (sql/delete db/team
    (sql/where {:tournament tournament-id})))

(defn ^:private delete-rounds [tournament-id]
  (sql/delete db/pairing
    (sql/where (sql/sqlfn "exists" (sql/subselect db/round
                                     (sql/where {:tournament tournament-id
                                                 :id         :pairing.round})))))
  (sql/delete db/standings
    (sql/where {:tournament tournament-id})))

(defn ^:private delete-seatings [tournament-id]
  (sql/delete db/seating
    (sql/where {:tournament tournament-id})))

(defn ^:private has-duplicate-names? [tournament-id]
  (:duplicate_names
   (sql-util/select-unique-or-nil db/tournament
     (sql/fields :duplicate_names)
     (sql/where {:id tournament-id}))))

(defn ^:private get-duplicate-names [tournament-id]
  (when (has-duplicate-names? tournament-id)
    (let [players (sql/select db/team
                    (sql/join :inner db/team-players {:team.id :team_players.team})
                    (sql/fields :id :name [:team_players.player :dci])
                    (sql/where (sql/sqlfn "exists" (sql/subselect [db/team :team2]
                                                     (sql/where {:team2.name       :team.name
                                                                 :team2.id         [not= :team.id]
                                                                 :team2.tournament tournament-id}))))
                    (sql/where {:tournament tournament-id}))]
      (into {} (for [{:keys [id name dci]} players]
                 [id (str "..." (subs dci 6) " " name)])))))

(defn add-teams [sanction-id teams]
  (let [tournament-id (sanctionid->id sanction-id)]
    (delete-seatings tournament-id)
    (delete-pods tournament-id)
    (delete-rounds tournament-id)
    (delete-teams tournament-id)
    (let [teams (map fix-dci-numbers teams)
          duplicate-names? (->> (frequencies (map :name teams))
                                (vals)
                                (some #(< 1 %))
                                (boolean))]
      (add-players (mapcat :players teams))
      (doseq [team teams
              :let [team-id (add-team (:name team) tournament-id)]]
        (sql/insert db/team-players
          (sql/values (for [player (:players team)]
                        {:team   team-id
                         :player (:dci player)}))))
      (sql-util/update-unique db/tournament
        (sql/set-fields {:duplicate_names duplicate-names?})
        (sql/where {:id tournament-id})))))

(defn seatings [tournament-id]
  (seq
   (sql/select db/seating
     (sql/fields :table_number)
     (sql/with db/team
       (sql/fields :name))
     (sql/where {:tournament tournament-id})
     (sql/order :team.name :ASC))))

(defn pods [tournament-id number]
  (let [pod-rounds (map :id (sql/select db/pod-round
                              (sql/fields :id)
                              (sql/where {:tournament tournament-id})
                              (sql/order :id)))]
    (when (<= number (count pod-rounds))
      (sql/select db/seat
        (sql/fields :seat)
        (sql/with db/team
          (sql/fields [:name :team_name]))
        (sql/with db/pod
          (sql/fields [:number :pod])
          (sql/where {:pod_round (nth pod-rounds (dec number))}))
        (sql/order :pod.number)
        (sql/order :seat)))))

(defn latest-pods [tournament-id]
  (sql/select db/seat
    (sql/fields :seat)
    (sql/with db/team
      (sql/fields [:name :team_name]))
    (sql/with db/pod
      (sql/fields [:number :pod])
      (sql/where {:pod_round (sql/subselect db/pod-round
                               (sql/fields :id)
                               (sql/where {:tournament tournament-id})
                               (sql/order :id :DESC)
                               (sql/limit 1))}))
    (sql/order :pod.number)
    (sql/order :seat)))

(defn standings [tournament-id round-num hidden?]
  (-> (sql-util/select-unique-or-nil db/standings
        (sql/where (and {:tournament tournament-id
                         :round      round-num}
                        (or hidden?
                            (not :hidden)))))
      :standings
      edn/read-string))

(defn standings-for-api [tournament-id round-num hidden?]
  (seq
   (map #(select-keys % [:rank :team_name :points :omw :pgw :ogw])
        (standings tournament-id round-num hidden?))))

(defn ^:private get-or-add-round [tournament-id round-num playoff?]
  (if-let [old-round (sql-util/select-unique-or-nil db/round
                       (sql/where {:tournament tournament-id
                                   :num        round-num}))]
    (:id old-round)
    (:id (sql/insert db/round
           (sql/values {:tournament tournament-id
                        :num        round-num
                        :playoff    playoff?
                        :created    (time/now)})))))

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
    (when (seq seatings)
      (let [dci->id (teams-by-dci tournament-id)]
        (sql/insert db/seating
          (sql/values (for [seating seatings]
                        {:team         (dci->id (:team seating))
                         :table_number (:table_number seating)
                         :tournament   tournament-id}))))
      (update-tournament-modified tournament-id))))

(defn ^:private delete-results [round-id]
  (sql/update db/pairing
    (sql/set-fields {:team1_wins nil
                     :team2_wins nil
                     :draws      nil})
    (sql/where {:round round-id})))

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
                 :round      round-num}))))

(defn add-pairings [sanction-id round-num playoff? pairings]
  (let [tournament-id (sanctionid->id sanction-id)
        dci->id (teams-by-dci tournament-id)
        team->points (if-let [standings (standings tournament-id (dec round-num) true)]
                       (into {} (for [row standings]
                                  [(:team row) (:points row)]))
                       (constantly 0))
        round-id (get-or-add-round tournament-id round-num playoff?)]
    (when (seq pairings)
      (delete-pairings round-id)
      (delete-standings tournament-id round-num)
      (sql/insert db/pairing
        (sql/values (for [pairing pairings
                          :let [team1 (dci->id (:team1 pairing))
                                team2 (dci->id (:team2 pairing))]]
                      {:round        round-id
                       :team1        team1
                       :team2        team2
                       :team1_points (team->points team1)
                       :team2_points (team->points team2 0)
                       :table_number (:table_number pairing)})))
      (update-tournament-modified tournament-id))))

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
  (sql-util/select-unique-or-nil db/pairing
    (sql/with db/team1)
    (sql/with db/team2)
    (sql/where {:round round-id})
    (add-team-where :team1.id team1)
    (add-team-where :team2.id team2)))

(defn ^:private results-of-round [duplicate-names round-id]
  (let [results (sql/select db/pairing
                  (sql/fields :team1
                              :team2
                              :team1_points
                              :team2_points
                              [(sql/sqlfn :COALESCE :team2.name "***BYE***") :team2_name]
                              :table_number
                              :team1_wins
                              :team2_wins
                              :draws)
                  (sql/with db/team1
                    (sql/fields [:name :team1_name]))
                  (sql/with db/team2
                    (sql/fields))
                  (sql/with db/round
                    (sql/fields [:num :round_number]))
                  (sql/where {:round round-id})
                  (sql/order :table_number :ASC))]
    (if duplicate-names
      (for [{:keys [team1 team2] :as result} results]
        (cond-> result
          (contains? duplicate-names team1) (assoc :team1_name (get duplicate-names team1))
          (contains? duplicate-names team2) (assoc :team2_name (get duplicate-names team2))))
      results)))

(defn ^:private calculate-standings [tournament-id round]
  (let [rounds (sql/select db/round
                 (sql/where {:tournament tournament-id
                             :num        [<= round]})
                 (sql/order :num :DESC))
        duplicate-names (get-duplicate-names tournament-id)
        rounds-results (into {} (for [r rounds]
                                  [(:num r) (results-of-round duplicate-names (:id r))]))
        round-num (:num (first rounds))]
    (let [std (mtg-util/calculate-standings rounds-results round-num)]
      (sql/insert db/standings
        (sql/values {:standings  (pr-str std)
                     :tournament tournament-id
                     :round      round-num
                     :hidden     (not-every? :team1_wins (rounds-results round-num))})))))

(defn add-results [sanction-id round-num results]
  (let [tournament-id (sanctionid->id sanction-id)
        {round-id :id, playoff? :playoff} (sql-util/select-unique db/round
                                            (sql/where {:tournament tournament-id
                                                        :num        round-num}))]
    (when (seq results)
      (delete-results round-id)
      (delete-standings tournament-id round-num)
      (doseq [res results
              :let [pairing-id (:id (find-pairing round-id res))]]
        (sql-util/update-unique db/pairing
          (sql/set-fields {:team1_wins (:team1_wins res)
                           :team2_wins (:team2_wins res)
                           :draws      (:draws res)})
          (sql/where {:id pairing-id})))
      (when-not playoff?
        (calculate-standings tournament-id round-num))
      (update-tournament-modified tournament-id))))

(defn publish-results [sanction-id round-num]
  (let [tournament-id (sanctionid->id sanction-id)]
    (sql-util/update-unique db/standings
      (sql/set-fields {:hidden false})
      (sql/where {:tournament tournament-id
                  :round      round-num}))
    (update-tournament-modified tournament-id)))

(defn get-round [tournament-id round-num]
  (let [duplicate-names (get-duplicate-names tournament-id)
        round-id (:id (sql-util/select-unique db/round
                        (sql/where {:tournament tournament-id
                                    :num        round-num})))]
    (map #(dissoc % :team1 :team2) (results-of-round duplicate-names round-id))))

(defn delete-round [sanction-id round-num]
  (let [tournament-id (sanctionid->id sanction-id)
        round-id (:id (sql-util/select-unique db/round
                        (sql/fields :id)
                        (sql/where {:tournament tournament-id
                                    :num        round-num})))]
    (sql/delete db/standings
      (sql/where {:tournament tournament-id
                  :round      round-num}))
    (sql/delete db/pairing
      (sql/where {:round round-id}))))

(defn add-pods [sanction-id pods]
  (let [tournament-id (sanctionid->id sanction-id)
        dci->id (teams-by-dci tournament-id)]
    (delete-pods tournament-id)
    (doseq [pod-round pods
            :let [round (sql-util/select-unique db/round
                          (sql/where {:num        (:round pod-round)
                                      :tournament tournament-id}))
                  r (sql/insert db/pod-round
                      (sql/values {:tournament tournament-id
                                   :round      (:id round)}))]
            pod (:pods pod-round)
            :let [p (sql/insert db/pod
                      (sql/values {:pod_round (:id r)
                                   :number    (:number pod)}))]
            seat (:seats pod)]
      (sql/insert db/seat
        (sql/values {:pod  (:id p)
                     :seat (:seat seat)
                     :team (dci->id (:team seat))})))
    (update-tournament-modified tournament-id)))

(defn ^:private delete-tournament-data [tournament-id]
  (delete-pods tournament-id)
  (doseq [round (doall (map :id (sql/select db/round
                                  (sql/where {:tournament tournament-id})
                                  (sql/order :num :DESC))))]
    (delete-pairings round))
  (delete-seatings tournament-id)
  (delete-teams tournament-id)
  (delete-standings tournament-id))

(defn reset-tournament [sanction-id]
  (delete-tournament-data (sanctionid->id sanction-id)))

(defn delete-tournament [sanction-id]
  (let [tournament-id (sanctionid->id sanction-id)]
    (delete-tournament-data tournament-id)
    (sql/delete db/round
      (sql/where {:tournament tournament-id}))
    (sql-util/delete-unique db/tournament
      (sql/where {:id tournament-id}))))

(defn ^:private get-matches-by-team [tournament-id]
  (let [results (sql/select db/pairing
                  (sql/fields :team1
                              :team2
                              :team1_points
                              :team2_points
                              :table_number
                              :team1_wins
                              :team2_wins
                              :draws)
                  (sql/with db/team1
                    (sql/fields [:name :team1_name]))
                  (sql/with db/team2
                    (sql/fields [:name :team2_name]))
                  (sql/with db/round
                    (sql/fields [:num :round_number])
                    (sql/where {:tournament tournament-id})))
        all-results (sort-by :round_number >
                             (mapcat (fn [result]
                                       (let [result (if-not (:team2 result)
                                                      (merge result {:team2_name   "***BYE***"
                                                                     :team2_points 0})
                                                      result)]
                                         [result (mtg-util/reverse-match result)])) results))]
    (group-by :team1_name all-results)))

(defn coverage [tournament-id]
  (let [round-num (:num (sql-util/select-unique db/round
                          (sql/where {:tournament tournament-id})
                          (sql/order :num :desc)
                          (sql/limit 1)))
        pairings (get-round tournament-id round-num)
        matches-by-team (get-matches-by-team tournament-id)
        standings (standings-for-api tournament-id round-num true)]
    {:pairings  pairings
     :matches   matches-by-team
     :standings standings}))

(defn deck-construction-seatings [tournament-id pod-seats]
  (let [n (apply max (map :pod pod-seats))]
    (for [s pod-seats
          :let [seat (dec (:seat s))
                pod (dec (:pod s))]]
      {:team         (:team s)
       :table_number (-> (* n seat)
                         (+ pod)
                         (/ 2)
                         int
                         inc)
       :tournament   tournament-id})))

(defn generate-deck-construction-seatings [sanctionid pod-round]
  (let [tournament-id (sanctionid->id sanctionid)
        pod-rounds (map :id (sql/select db/pod-round
                              (sql/fields :id)
                              (sql/where {:tournament tournament-id})
                              (sql/order :id)))
        round-id (nth pod-rounds (dec pod-round))
        pod-seats (sql/select db/seat
                    (sql/fields :seat)
                    (sql/with db/team
                      (sql/fields [:id :team]))
                    (sql/with db/pod
                      (sql/fields [:number :pod])
                      (sql/where {:pod_round round-id})))
        seatings (deck-construction-seatings tournament-id pod-seats)]
    (delete-seatings tournament-id)
    (sql/insert db/seating
      (sql/values seatings))))

(defn ^:private add-ranks [team->rank round]
  (for [match round]
    (assoc match :team1_rank (team->rank (:team1 match))
           :team2_rank (team->rank (:team2 match)))))

(defn ^:private add-empty-rounds [bracket]
  (loop [num (/ (count (last bracket)) 2)
         bracket (vec bracket)]
    (if (>= num 1)
      (recur (/ num 2) (conj bracket (repeat num {})))
      bracket)))

(defn bracket [tournament-id]
  (when-let [playoff-rounds (seq (sql/select db/round
                                   (sql/fields :id :num)
                                   (sql/where {:tournament tournament-id
                                               :playoff    true})
                                   (sql/order :num :ASC)))]
    (let [final-standings (standings tournament-id (dec (:num (first playoff-rounds))) false)
          team->rank (into {} (map (juxt :team :rank)) final-standings)
          duplicate-names (get-duplicate-names tournament-id)
          playoff-matches (map (comp (partial add-ranks team->rank)
                                     (partial results-of-round duplicate-names)
                                     :id)
                               playoff-rounds)]
      (add-empty-rounds playoff-matches))))

(defn ^:private count-full-pods [n]
  (- (int (Math/ceil (/ n 8))) (mod (- n) 8)))

(defn generate-draft-pods [sanction-id dropped-dcis]
  (let [tournament-id (sanctionid->id sanction-id)
        std (-> (sql-util/select-unique db/standings
                  (sql/where {:tournament tournament-id
                              :round      (sql/subselect db/standings
                                            (sql/aggregate (max :round) :round)
                                            (sql/where {:tournament tournament-id}))}))
                :standings
                edn/read-string)
        max-round (:round (sql-util/select-unique db/round
                            (sql/aggregate (max :num) :round)
                            (sql/where {:tournament tournament-id})))
        team->dci (into {} (map (juxt :id :player)) (sql/select db/team
                                                      (sql/join db/team-players {:team.id :team_players.team})
                                                      (sql/where {:tournament tournament-id})
                                                      (sql/fields :team.id :team_players.player)))
        playing (remove dropped-dcis (map (comp team->dci :team) std))
        full-pods (count-full-pods (count playing))
        pods (concat (take full-pods (partition 8 playing))
                     (partition 7 (drop (* 8 full-pods) playing)))]
    (get-or-add-round tournament-id (inc max-round) false)
    (add-pods sanction-id [{:round (inc max-round)
                            :pods  (for [[n pod] (util/indexed pods)]
                                     {:number (inc n)
                                      :seats  (for [[seat dci] (util/indexed (shuffle pod))]
                                                {:seat (inc seat)
                                                 :team [dci]})})}])))
