(ns mtg-pairings-server.service.tournament
  (:require [clojure.set :refer [rename-keys union]]
            [clojure.edn :as edn]
            [clj-time.core :as time]
            [honeysql.core :as hsql]
            [honeysql.helpers :as sql]
            [mtg-pairings-server.db :as db]
            [mtg-pairings-server.util.honeysql :refer [returning using]]
            [mtg-pairings-server.util.mtg :as mtg-util]
            [mtg-pairings-server.util :as util])
  (:import (java.util UUID)))

(defn user-for-apikey [apikey]
  (try
    (-> (sql/select :id)
        (sql/from :trader_user)
        (sql/where [:= :uuid (UUID/fromString apikey)])
        (db/query-one-field-or-nil))
    (catch IllegalArgumentException _
      nil)))

(defn owner-of-tournament [sanction-id]
  (-> (sql/select :owner)
      (sql/from :tournament)
      (sql/where [:= :sanctionid sanction-id])
      (db/query-one-field-or-nil)))

(defn sanctionid->id [sanction-id]
  (-> (sql/select :id)
      (sql/from :tournament)
      (sql/where [:= :sanctionid sanction-id])
      (db/query-one-field)))

(defn id->sanctionid [id]
  (-> (sql/select :sanctionid)
      (sql/from :tournament)
      (sql/where [:= :id id])
      (db/query-one-field)))

(def ^:private select-tournaments
  (-> (sql/select :rounds :day :tournament.name :organizer :tournament.id :modified)
      (sql/from :tournament)
      (sql/merge-select [{:exists (-> (sql/select :*)
                                      (sql/from :seating)
                                      (sql/where [:= :tournament :tournament.id]))}
                         :seatings])
      (sql/merge-select [{:exists (-> (sql/select :*)
                                      (sql/from :round)
                                      (sql/where [:= :tournament :tournament.id]
                                                 :playoff))}
                         :playoff])
      (sql/order-by [:day :desc] [:name :asc] [:id :asc])))

(def ^:private select-tournaments-with-teams
  (sql/merge-select select-tournaments
                    [(-> (sql/select :%count.*)
                         (sql/from :team)
                         (sql/where [:= :tournament :tournament.id]))
                     :players]))

(def ^:private select-rounds
  (-> (sql/select :num :created [(hsql/raw "COUNT(pairing.id) FILTER (WHERE team1_wins IS NULL) = 0") :results])
      (sql/from :round)
      (sql/join :pairing [:= :round :round.id])
      (sql/where [:not :playoff])
      (sql/group :num :created)
      (sql/order-by [:num :asc])))

(def ^:private select-standings
  (-> (sql/select :round)
      (sql/from :standings)
      (sql/where [:not :hidden])
      (sql/order-by [:round :asc])))

(defn ^:private update-round-data [tournament rounds standings pods]
  (merge tournament
         {:pairings    (map :num rounds)
          :results     (map :num (filter :results rounds))
          :pods        (range 1 (inc pods))
          :round-times (into {} (map (juxt :num :created)) rounds)
          :standings   standings}))

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
  (let [tournament (-> select-tournaments-with-teams
                       (sql/where [:= :tournament.id id])
                       (db/query-one))
        rounds (-> select-rounds
                   (sql/merge-where [:= :tournament id])
                   (db/query))
        standings (-> select-standings
                      (sql/merge-where [:= :tournament id])
                      (db/query-field))
        pods (-> (sql/select :%count.*)
                 (sql/from :pod_round)
                 (sql/where [:= :tournament id])
                 (db/query-one-field-or-nil))]
    (update-round-data tournament rounds standings pods)))

(defn client-tournament [id]
  (format-client-tournament (tournament id)))

(defn tournaments [{:keys [active? modified]
                    :or   {active? false, modified nil}}]
  (when-let [tourns (-> select-tournaments-with-teams
                        (cond->
                          active? (sql/merge-where [:>= :day (hsql/raw "current_date - 1")])
                          modified (sql/merge-where [:> :modified modified]))
                        (db/query)
                        (seq))]
    (let [ids (when (or active? modified)
                (map :id tourns))
          add-tournament-where (fn [query]
                                 (cond-> query
                                   ids (sql/merge-where [:in :tournament ids])))
          rounds (util/group-kv :tournament
                                #(select-keys % [:num :results :created])
                                (-> select-rounds
                                    (sql/merge-select :tournament)
                                    (add-tournament-where)
                                    (sql/merge-group-by :tournament)
                                    (db/query)))
          standings (util/group-kv :tournament
                                   :round
                                   (-> select-standings
                                       (sql/merge-select :tournament)
                                       (add-tournament-where)
                                       (db/query)))
          pod-rounds (into {} (-> (sql/select :tournament :%count.*)
                                  (sql/from :pod_round)
                                  (add-tournament-where)
                                  (sql/group :tournament)
                                  (db/queryv)))]
      (for [t tourns
            :let [id (:id t)]]
        (update-round-data t
                           (get rounds id [])
                           (get standings id [])
                           (get pod-rounds id 0))))))

(defn client-tournaments
  ([]
   (client-tournaments {}))
  ([opts]
   (let [tourns (tournaments opts)]
     {:tournaments (map format-client-tournament tourns)
      :modified    (when (seq tourns)
                     (reduce util/max-date (map :modified tourns)))})))

(defn organizer-tournaments []
  (-> select-tournaments
      (sql/select :id :name :organizer)
      (sql/merge-where [:= :day (hsql/raw "CURRENT_DATE")])
      (db/query)))

(defn add-tournament [tourn]
  (if (zero? (-> (sql/select :%count.*)
                 (sql/from :tournament)
                 (sql/where [:= :sanctionid (:sanctionid tourn)])
                 (db/query-one-field)))
    (-> (sql/insert-into :tournament)
        (sql/values [(select-keys tourn [:name :organizer :day :sanctionid :rounds :owner])])
        (returning :*)
        (db/query-one))
    (-> (sql/update :tournament)
        (sql/sset (select-keys tourn [:name :organizer :day :rounds]))
        (sql/where [:= :sanctionid (:sanctionid tourn)]
                   [:= :owner (:owner tourn)])
        (returning :*)
        (db/query-one))))

(defn save-tournament [sanctionid tourn]
  (-> (sql/update :tournament)
      (sql/sset tourn)
      (sql/where [:= :sanctionid sanctionid])
      (returning :*)
      (db/query-one)))

(defn update-tournament-modified [id]
  (-> (sql/update :tournament)
      (sql/sset {:modified (hsql/call :date_trunc "milliseconds" :%now)})
      (sql/where [:= :id id])
      (returning :*)
      (db/query-one)))

(defn add-players [players]
  (let [old-players (-> (sql/select :dci)
                        (sql/from :player)
                        (db/query-field)
                        (set))
        new-players (remove (comp old-players :dci) players)]
    (when (seq new-players)
      (-> (sql/insert-into :player)
          (sql/values new-players)
          (db/query)))))

(defn ^:private fix-dci-number [player]
  (update player :dci mtg-util/add-check-digits))

(defn ^:private fix-dci-numbers [team]
  (update team :players #(map fix-dci-number %)))

(defn ^:private delete-pods [tournament-id]
  (-> (sql/delete-from :pod_seat)
      (using :pod)
      (sql/join :pod_round [:= :pod_round :pod_round.id])
      (sql/where [:= :tournament tournament-id]
                 [:= :pod :pod.id])
      (db/query))
  (-> (sql/delete-from :pod)
      (using :pod_round)
      (sql/where [:= :tournament tournament-id]
                 [:= :pod_round :pod_round.id])
      (db/query))
  (-> (sql/delete-from :pod_round)
      (sql/where [:= :tournament tournament-id])
      (db/query)))

(defn ^:private delete-teams [tournament-id]
  (-> (sql/delete-from :team_players)
      (sql/where [:exists (-> (sql/select :*)
                              (sql/from :team)
                              (sql/where [:= :team_players.team :team.id]
                                         [:= :team.tournament tournament-id]))])
      (db/query))
  (-> (sql/delete-from :team)
      (sql/where [:= :tournament tournament-id])
      (db/query)))

(defn ^:private delete-rounds [tournament-id]
  (-> (sql/delete-from :pairing)
      (sql/where [:exists (-> (sql/select :*)
                              (sql/from :round)
                              (sql/where [:= :pairing.round :round.id]
                                         [:= :round.tournament tournament-id]))])
      (db/query))
  (-> (sql/delete-from :standings)
      (sql/where [:= :tournament tournament-id])
      (db/query)))

(defn ^:private delete-seatings [tournament-id]
  (-> (sql/delete-from :seating)
      (sql/where [:= :tournament tournament-id])
      (db/query)))

(defn ^:private has-duplicate-names? [tournament-id]
  (-> (sql/select :duplicate_names)
      (sql/from :tournament)
      (sql/where [:= :id tournament-id])
      (db/query-one-field)))

(defn ^:private get-duplicate-names [tournament-id]
  (when (has-duplicate-names? tournament-id)
    (let [players (-> (sql/select :id :name [:team_players.player :dci])
                      (sql/from :team)
                      (sql/join :team_players [:= :team.id :team_players.team])
                      (sql/where [:exists (-> (sql/select :*)
                                              (sql/from [:team :team2])
                                              (sql/where [:= :team2.name :team.name]
                                                         [:not= :team2.id :team.id]
                                                         [:= :team2.tournament tournament-id]))]
                                 [:= :tournament tournament-id])
                      (db/query))]
      (into {} (for [{:keys [id name dci]} players]
                 [id (str name " ..." (subs dci 6))])))))

(defn ^:private update-duplicate-names [duplicate-names results & ks]
  (if duplicate-names
    (for [result results]
      (reduce (fn [m [id-key name-key]]
                (if-let [name (get duplicate-names (get m id-key))]
                  (assoc m name-key name)
                  m))
              result
              ks))
    results))

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
                                (boolean))
          team-ids (-> (sql/insert-into :team)
                       (sql/values (for [{:keys [name]} teams]
                                     {:name       name
                                      :tournament tournament-id}))
                       (returning :id)
                       (db/query-field))]
      (add-players (mapcat :players teams))
      (-> (sql/insert-into :team_players)
          (sql/values (for [[team team-id] (util/zip teams team-ids)
                            player (:players team)]
                        {:team   team-id
                         :player (:dci player)}))
          (db/query))
      (-> (sql/update :tournament)
          (sql/sset {:duplicate_names duplicate-names?})
          (sql/where [:= :id tournament-id])
          (db/query)))))

(defn seatings [tournament-id]
  (let [duplicate-names (get-duplicate-names tournament-id)
        results (-> (sql/select [:team.id :team] :table_number :name)
                    (sql/from :seating)
                    (sql/join :team [:= :team :team.id])
                    (sql/where [:= :seating.tournament tournament-id])
                    (sql/order-by [:name :asc])
                    (db/query))]
    (seq (update-duplicate-names duplicate-names results [:team :name]))))

(def ^:private select-pods
  (-> (sql/select :seat [:team.id :team] [:team.name :team_name] [:pod.number :pod])
      (sql/from :pod_seat)
      (sql/join :team [:= :team :team.id]
                :pod [:= :pod :pod.id])
      (sql/order-by [:pod.number :asc] [:seat :asc])))

(defn pods [tournament-id number]
  (let [pod-rounds (-> (sql/select :id)
                       (sql/from :pod_round)
                       (sql/where [:= :tournament tournament-id])
                       (sql/order-by [:id :asc])
                       (db/query-field))
        duplicate-names (get-duplicate-names tournament-id)]
    (when (<= number (count pod-rounds))
      (let [results (-> select-pods
                        (sql/where [:= :pod_round (nth pod-rounds (dec number))])
                        (db/query))]
        (update-duplicate-names duplicate-names results [:team :team_name])))))

(defn latest-pods [tournament-id]
  (-> select-pods
      (sql/where [:= :pod_round (-> (sql/select :id)
                                    (sql/from :pod_round)
                                    (sql/where [:= :tournament tournament-id])
                                    (sql/order-by [:id :desc])
                                    (sql/limit 1))])
      (db/query)))

(defn standings [tournament-id round-num hidden?]
  (-> (sql/select :standings)
      (sql/from :standings)
      (sql/where [:= :tournament tournament-id]
                 [:= :round round-num]
                 (when-not hidden?
                   [:not :hidden]))
      (db/query-one-field-or-nil)
      (edn/read-string)))

(defn standings-for-api [tournament-id round-num hidden?]
  (seq
   (map #(select-keys % [:rank :team_name :points :omw :pgw :ogw])
        (standings tournament-id round-num hidden?))))

(defn ^:private get-or-add-round [tournament-id round-num playoff?]
  (if-let [old-id (-> (sql/select :id)
                      (sql/from :round)
                      (sql/where [:= :tournament tournament-id]
                                 [:= :num round-num])
                      (db/query-one-field-or-nil))]
    old-id
    (-> (sql/insert-into :round)
        (sql/values [{:tournament tournament-id
                      :num        round-num
                      :playoff    playoff?
                      :created    (time/now)}])
        (returning :id)
        (db/query-one-field))))

(defn teams-by-dci [tournament-id]
  (let [team-players (-> (sql/select :team [:%array_agg.player :players])
                         (sql/from :team_players)
                         (sql/join :team [:= :team_players.team :team.id])
                         (sql/where [:= :tournament tournament-id])
                         (sql/group :team)
                         (db/query))
        dci->players (into {} (for [{:keys [team players]} team-players]
                                [(set players) team]))]
    (fn [dcis] (dci->players (set (map mtg-util/add-check-digits dcis))))))

(defn add-seatings [sanction-id seatings]
  (let [tournament-id (sanctionid->id sanction-id)]
    (-> (sql/delete-from :seating)
        (sql/where [:= :tournament tournament-id])
        (db/query))
    (when (seq seatings)
      (let [dci->id (teams-by-dci tournament-id)]
        (-> (sql/insert-into :seating)
            (sql/values (for [seating seatings]
                          {:team         (dci->id (:team seating))
                           :table_number (:table_number seating)
                           :tournament   tournament-id}))
            (db/query)))
      (update-tournament-modified tournament-id))))

(defn ^:private delete-results [round-id]
  (-> (sql/update :pairing)
      (sql/sset {:team1_wins nil
                 :team2_wins nil
                 :draws      nil})
      (sql/where [:= :round round-id])
      (db/query)))

(defn ^:private delete-pairings [round-id]
  (-> (sql/delete-from :pairing)
      (sql/where [:= :round round-id])
      (db/query)))

(defn ^:private delete-standings
  ([tournament-id]
   (delete-standings tournament-id nil))
  ([tournament-id round-num]
   (-> (sql/delete-from :standings)
       (sql/where [:= :tournament tournament-id])
       (cond-> round-num (sql/merge-where [:= :round round-num]))
       (db/query))))

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
      (-> (sql/insert-into :pairing)
          (sql/values (for [pairing pairings
                            :let [team1 (dci->id (:team1 pairing))
                                  team2 (dci->id (:team2 pairing))]]
                        {:round        round-id
                         :team1        team1
                         :team2        team2
                         :team1_points (team->points team1)
                         :team2_points (team->points team2 0)
                         :table_number (:table_number pairing)}))
          (db/query))
      (update-tournament-modified tournament-id))))

(defn ^:private results-of-round [duplicate-names round-id]
  (let [results (-> (sql/select :team1 :team2 :team1_points :team2_points
                                :table_number :team1_wins :team2_wins :draws
                                [:team1.name :team1_name] [:round.num :round_number]
                                [(hsql/call :coalesce :team2.name "***BYE***") :team2_name])
                    (sql/from :pairing)
                    (sql/join [:team :team1] [:= :team1 :team1.id]
                              :round [:= :round :round.id])
                    (sql/left-join [:team :team2] [:= :team2 :team2.id])
                    (sql/where [:= :round round-id])
                    (sql/order-by [:table_number :asc])
                    (db/query))]
    (update-duplicate-names duplicate-names results [:team1 :team1_name] [:team2 :team2_name])))

(defn ^:private calculate-standings [tournament-id round]
  (let [rounds (-> (sql/select :id :num)
                   (sql/from :round)
                   (sql/where [:= :tournament tournament-id]
                              [:<= :num round])
                   (sql/order-by [:num :desc])
                   (db/query))
        duplicate-names (get-duplicate-names tournament-id)
        rounds-results (into {} (for [r rounds]
                                  [(:num r) (results-of-round duplicate-names (:id r))]))
        round-num (:num (first rounds))]
    (let [std (mtg-util/calculate-standings rounds-results round-num)]
      (-> (sql/insert-into :standings)
          (sql/values [{:standings  (pr-str std)
                        :tournament tournament-id
                        :round      round-num
                        :hidden     (not-every? :team1_wins (rounds-results round-num))}])
          (db/query)))))

(defn ^:private add-team-where [query key dcis]
  (if (seq dcis)
    (reduce (fn [q dci]
              (sql/merge-where q [:exists
                                  (-> (sql/select :*)
                                      (sql/from :team_players)
                                      (sql/where [:= :team key]
                                                 [:= :player (mtg-util/add-check-digits dci)]))]))
            query
            dcis)
    (sql/merge-where query [:= :team2 nil])))

(defn add-results [sanction-id round-num results]
  (let [tournament-id (sanctionid->id sanction-id)
        {round-id :id, playoff? :playoff} (-> (sql/select :id :playoff)
                                              (sql/from :round)
                                              (sql/where [:= :tournament tournament-id]
                                                         [:= :num round-num])
                                              (db/query-one))]
    (when (seq results)
      (delete-results round-id)
      (delete-standings tournament-id round-num)
      (doseq [res results]
        (-> (sql/update :pairing)
            (sql/sset (select-keys res [:team1_wins :team2_wins :draws]))
            (sql/where [:= :round round-id])
            (add-team-where :team1 (:team1 res))
            (add-team-where :team2 (:team2 res))
            (db/query-one)))
      (when-not playoff?
        (calculate-standings tournament-id round-num))
      (update-tournament-modified tournament-id))))

(defn publish-results [sanction-id round-num]
  (let [tournament-id (sanctionid->id sanction-id)]
    (-> (sql/update :standings)
        (sql/sset {:hidden false})
        (sql/where [:= :tournament tournament-id]
                   [:= :round round-num])
        (db/query-one))
    (update-tournament-modified tournament-id)))

(defn get-round-id [tournament-id round-num]
  (-> (sql/select :id)
      (sql/from :round)
      (sql/where [:= :tournament tournament-id]
                 [:= :num round-num])
      (db/query-one-field)))

(defn get-round [tournament-id round-num]
  (let [duplicate-names (get-duplicate-names tournament-id)
        round-id (get-round-id tournament-id round-num)]
    (map #(dissoc % :team1 :team2) (results-of-round duplicate-names round-id))))

(defn delete-round [sanction-id round-num]
  (let [tournament-id (sanctionid->id sanction-id)]
    (-> (sql/delete-from :standings)
        (sql/where [:= :tournament tournament-id]
                   [:= :round round-num])
        (db/query))
    (-> (sql/delete-from :pairing)
        (using :round)
        (sql/where [:= :round.tournament tournament-id]
                   [:= :round.num round-num]
                   [:= :pairing.round :round.id])
        (db/query))))

(defn add-pods [sanction-id pods]
  (let [tournament-id (sanctionid->id sanction-id)
        dci->id (teams-by-dci tournament-id)
        _ (delete-pods tournament-id)
        seats (doall
               (for [pod-round pods
                     :let [round-id (get-or-add-round tournament-id (:round pod-round) false)
                           pod-round-id (-> (sql/insert-into :pod_round)
                                            (sql/values [{:tournament tournament-id
                                                          :round      round-id}])
                                            (returning :id)
                                            (db/query-one-field))]
                     pod (:pods pod-round)
                     :let [pod-id (-> (sql/insert-into :pod)
                                      (sql/values [{:pod_round pod-round-id
                                                    :number    (:number pod)}])
                                      (returning :id)
                                      (db/query-one-field))]
                     seat (:seats pod)]
                 {:pod  pod-id
                  :seat (:seat seat)
                  :team (dci->id (:team seat))}))]
    (-> (sql/insert-into :pod_seat)
        (sql/values seats)
        (db/query))
    (update-tournament-modified tournament-id)))

(defn ^:private delete-tournament-data [tournament-id]
  (delete-pods tournament-id)
  (-> (sql/delete-from :pairing)
      (using :round)
      (sql/where [:= :pairing.round :round.id]
                 [:= :round.tournament tournament-id])
      (db/query))
  (delete-seatings tournament-id)
  (delete-teams tournament-id)
  (delete-standings tournament-id))

(defn reset-tournament [sanction-id]
  (delete-tournament-data (sanctionid->id sanction-id)))

(defn delete-tournament [sanction-id]
  (let [tournament-id (sanctionid->id sanction-id)]
    (delete-tournament-data tournament-id)
    (-> (sql/delete-from :round)
        (sql/where [:= :tournament tournament-id])
        (db/query))
    (-> (sql/delete-from :tournament)
        (sql/where [:= :id tournament-id])
        (db/query-one))))

(defn ^:private get-matches-by-team [tournament-id]
  (let [results (-> (sql/select :team1 :team2 :team1_points :team2_points
                                :table_number :team1_wins :team2_wins :draws
                                [:team1.name :team1_name] [:team2.name :team2_name]
                                [:round.num :round_number])
                    (sql/from :pairing)
                    (sql/join [:team :team1] [:= :team1 :team1.id]
                              :round [:= :round :round.id])
                    (sql/left-join [:team :team2] [:= :team2 :team2.id])
                    (sql/where [:= :round.tournament tournament-id])
                    (db/query))
        all-results (sort-by :round_number >
                             (mapcat (fn [result]
                                       (let [result (if-not (:team2 result)
                                                      (merge result {:team2_name   "***BYE***"
                                                                     :team2_points 0})
                                                      result)]
                                         [result (mtg-util/reverse-match result)])) results))]
    (group-by :team1_name all-results)))

(defn coverage [tournament-id]
  (let [round-num (-> (sql/select :num)
                      (sql/from :round)
                      (sql/where [:= :tournament tournament-id])
                      (sql/order-by [:num :desc])
                      (sql/limit 1)
                      (db/query-one-field))
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
        pod-rounds (-> (sql/select :id)
                       (sql/from :pod_round)
                       (sql/where [:= :tournament tournament-id])
                       (sql/order-by [:id :asc])
                       (db/query-field))
        round-id (nth pod-rounds (dec pod-round))
        pod-seats (-> (sql/select :seat :team [:pod.number :pod])
                      (sql/from :pod_seat)
                      (sql/join :team [:= :team :team.id]
                                :pod [:= :pod :pod.id])
                      (sql/where [:= :pod.pod_round round-id])
                      (db/query))
        seatings (deck-construction-seatings tournament-id pod-seats)]
    (delete-seatings tournament-id)
    (-> (sql/insert-into :seating)
        (sql/values seatings)
        (db/query))))

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

(defn ^:private has-team? [team match]
  (or (= team (:team1 match))
      (= team (:team2 match))))

(defn ^:private organize-bracket [bracket]
  (when (seq bracket)
    (loop [[round & rounds] (rest (reverse bracket))
           acc [(last bracket)]]
      (if round
        (let [teams (mapcat (juxt :team1 :team2) (first acc))
              next-round (for [team teams]
                           (util/some-value (partial has-team? team) round))]
          (recur rounds (cons next-round acc)))
        acc))))

(defn bracket [tournament-id]
  (when-let [playoff-rounds (seq (-> (sql/select :id :num)
                                     (sql/from :round)
                                     (sql/where [:= :tournament tournament-id]
                                                :playoff)
                                     (sql/order-by [:num :asc])
                                     (db/query)))]
    (let [final-standings (standings tournament-id (dec (:num (first playoff-rounds))) false)
          team->rank (into {} (map (juxt :team :rank)) final-standings)
          duplicate-names (get-duplicate-names tournament-id)
          playoff-matches (map (comp (partial add-ranks team->rank)
                                     (partial results-of-round duplicate-names)
                                     :id)
                               playoff-rounds)]
      (add-empty-rounds (organize-bracket playoff-matches)))))

(defn ^:private count-full-pods [n]
  (- (int (Math/ceil (/ n 8))) (mod (- n) 8)))

(defn generate-draft-pods [sanction-id dropped-dcis]
  (let [tournament-id (sanctionid->id sanction-id)
        std (-> (sql/select :standings)
                (sql/from :standings)
                (sql/where [:= :tournament tournament-id]
                           [:= :round (-> (sql/select :%max.round)
                                          (sql/from :standings)
                                          (sql/where [:= :tournament tournament-id]))])
                (db/query-one-field)
                (edn/read-string))
        max-round (-> (sql/select :%max.num)
                      (sql/from :round)
                      (sql/where [:= :tournament tournament-id])
                      (db/query-one-field))
        team->dci (into {} (-> (sql/select :team.id :team_players.player)
                               (sql/from :team)
                               (sql/join :team_players [:= :team :team.id])
                               (sql/where [:= :tournament tournament-id])
                               (db/queryv)))
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
