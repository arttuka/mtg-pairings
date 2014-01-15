(ns mtg-pairings-server.in-memory-db
  (:require [mtg-pairings-server.mtg-util :refer [calculate-standings]]
            [mtg-pairings-server.db]
            [clojure.java.io :only [output-stream]]
            [clojure.tools.reader.edn :as edn]))

(def ^:private default-tournament
  {:rounds {}
   :teams {}
   :standings []})

(defn ^:private add-tournament-to-map [tournaments tournament id]
  (let [tournament (merge default-tournament (assoc tournament :id id))]
    (assoc tournaments id tournament)))

(defn ^:private mem-tournament [db id]
  (get-in @db [:tournaments id]))

(defn ^:private mem-player [db dci]
  (get-in @db [:players dci]))

(defn ^:private mem-add-tournament [db tournament-id-seq tournament]
  (let [id (swap! tournament-id-seq inc)] 
    (swap! db update-in [:tournaments] add-tournament-to-map tournament id)
    id))

(defn ^:private mem-add-teams [db tournament-id teams]
  (let [players (mapcat :players teams)
        players-map (zipmap (map :dci players) players)
        teams (for [team teams]
                {:name (:name team)
                 :dci (map :dci (:players team))})]
    (swap! db update-in [:players] merge players-map)
    (swap! db update-in [:tournaments tournament-id] assoc :teams teams)))

(defn ^:private add-points-to-pairings [round standings pairings]
  (let [points-of-team (if (= 1 round)
                         (constantly 0)
                         (into {} (for [team standings]
                                    {(:team team) (:points team)})))]
    (for [pairing pairings]
      {:team-1 (:team1 pairing)
       :team-2 (:team2 pairing)
       :team-1-points (points-of-team (:team1 pairing))
       :team-2-points (points-of-team (:team2 pairing))
       :wins (:team1_wins pairing)
       :losses (:team2_wins pairing)
       :draws (:draws pairing)})))

(defn ^:private mem-add-pairings [db tournament-id round pairings]
  (let [standings (get-in @db [:tournaments tournament-id :standings])
        pairings (add-points-to-pairings round standings pairings)] 
    (swap! db update-in [:tournaments tournament-id :rounds] assoc round pairings)))

(defn ^:private update-results-and-calculate-standings
  [tournament round results]
  (let [old-standings (:standings tournament) 
        results (add-points-to-pairings round old-standings results)
        tournament (assoc-in tournament [:rounds round] results)
        standings (calculate-standings (:rounds tournament) round)]
    (assoc tournament :standings standings)))

(defn ^:private mem-add-results [db tournament-id round results]
  (swap! db update-in [:tournaments tournament-id] update-results-and-calculate-standings round results))

(defrecord ^:private InMemoryDB [db tournament-id-seq]
  mtg-pairings-server.db/DB
  (tournament [this id]
    (mem-tournament db id))
  (player [this dci]
    (mem-player db dci))
  (add-tournament [this tournament]
    (mem-add-tournament db tournament-id-seq tournament))
  (add-teams [this tournament-id teams]
    (mem-add-teams db tournament-id teams))
  (add-pairings [this tournament-id round-num pairings]
    (mem-add-pairings db tournament-id round-num pairings))
  (add-results [this tournament-id round-num results]
    (mem-add-results db tournament-id round-num results)))

(defn write-to-file [db filename]
  (spit filename (pr-str @(:db db)))
  db)

(defn load-from-file [db filename]
  (let [data-str (slurp filename)
        data (edn/read-string data-str)
        max-id (apply max (keys (:tournaments data)))]
    (reset! (:tournament-id-seq db) max-id)
    (reset! (:db db) data)
    db))

(defn create-db []
  (->InMemoryDB 
    (atom {:tournaments {}
           :players {}})
    (atom 0)))
