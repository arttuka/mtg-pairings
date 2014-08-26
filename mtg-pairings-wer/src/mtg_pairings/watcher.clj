(ns mtg-pairings.watcher
  (:require [watchtower.core :as watch]
            [mtg-pairings.db-reader :as reader]
            [clojure.tools.reader.edn :as edn]
            [clj-time.core :refer [today local-date]]
            [clojure.java.io :refer [as-file]]
            [mtg-pairings.util :refer :all])
  (:import (java.io File FileNotFoundException)))

(defonce watcher (atom nil))
(defonce state (atom {}))

(defn ^:private check-tournaments! [db handler]
  (let [tournaments (reader/tournaments db {:IsStarted true})
        new-tournaments (remove #((set (keys @state)) (:TournamentId %)) tournaments)
        new-tournaments (into {} (for [tournament new-tournaments]
                                   [(:TournamentId tournament)
                                    {:name (:Title tournament)
                                     :rounds (:NumberOfRounds tournament)
                                     :day (:StartDate tournament)
                                     :sanctionid (sanction-id tournament)
                                     :teams []
                                     :seatings []
                                     :pairings {}
                                     :results {}
                                     :uploaded {:tournament false
                                                :teams false
                                                :seatings false
                                                :pairings {}
                                                :results {}}
                                     :tracking false}]))]
    (swap! state merge new-tournaments)
    (doseq [[id t] new-tournaments]
      (handler id))))

(defn set-uploaded! [tournament-id type & [round]]
  (swap! state assoc-in (if round
                          [tournament-id :uploaded type round]
                          [tournament-id :uploaded type]) 
                        true))

(defn uploaded? [tournament-id type & [round]]
  (get-in @state (if round
                   [tournament-id :uploaded type round]
                   [tournament-id :uploaded type])))

(defn ^:private round-done? [results]
  (not-any? neg? (map :team1_wins results)))

(defn ^:private done-rounds [all-results]
  (into {} (filter (comp round-done? val) all-results)))

(defn ^:private incomplete-round [all-results]
  (some-> 
    (remove (comp round-done? val) all-results)
    first
    val))

(defn ^:private missing-results [all-results]
  (->>
    (incomplete-round all-results)
    (filter #(neg? (:team1_wins %)))
    (map :table_number)
    sort))

(defn ^:private changed-index [idx [old new]]
  (when (not= old new) (inc idx)))

(defn ^:private changed-rounds [old-values new-values]
  (keep-indexed changed-index (zip old-values new-values)))

(defn ^:private tournament-checker [handlers]
  (letfn [(handler [type values tournament-id]
            (let [f (get handlers type)
                  old-values (get-in @state [tournament-id type])
                  rounds (changed-rounds old-values values)]
              (when (not= old-values values)
                (swap! state assoc-in [tournament-id type] values)
                (when f
                  (case type
                    :teams (f tournament-id)
                    :seatings (f tournament-id)
                    :pairings (doseq [round rounds]
                                (f tournament-id round))
                    :results (doseq [round rounds] 
                               (f tournament-id round)))))))]
    (fn [db tournament-id]
      (handler :teams (reader/teams db tournament-id) tournament-id)
      (handler :seatings (reader/seatings db tournament-id) tournament-id)
      (handler :pairings (reader/pairings db tournament-id) tournament-id)
      (handler :results (done-rounds (reader/results db tournament-id)) tournament-id)
      (handler :missing-results (missing-results (reader/results db tournament-id)) tournament-id))))

(defn get-tournament [id]
  (let [tournament (get @state id)]
    (select-keys tournament [:name :rounds :day :sanctionid :tracking])))

(defn get-pairings [id round]
  (get-in @state [id :pairings round]))

(defn get-pairings-count [id]
  (count (get-in @state [id :pairings])))

(defn get-seatings [id]
  (seq (get-in @state [id :seatings])))

(defn get-results [id round]
  (get-in @state [id :results round]))

(defn get-missing-results [id]
  (get-in @state [id :missing-results]))

(defn get-results-count [id]
  (count (get-in @state [id :results])))

(defn get-teams [id]
  (seq (get-in @state [id :teams])))

(defn get-tournaments [] (for [[id tournament] @state] 
                           (-> tournament (select-keys [:name :rounds :day :tracking]) (assoc :id id))))

(defn start! [path & {:as handlers}]
  (let [file (File. path)
        fname (.getName file)
        checker! (tournament-checker handlers)
        on-change! (fn [files]
                     (doseq [f files
                             :when (= fname (.getName f))]
                       (with-open [db (reader/open (.getPath f))]
                         (check-tournaments! db (:tournament handlers))
                         (doseq [[id tourn] @state
                                 :when (:tracking tourn)]
                           (checker! db id)))))]
    (if-not (.exists file)
      (throw (FileNotFoundException. path))
      (reset! watcher (watch/watcher [(.getParent file)]
                        (watch/rate 100)
                        (watch/file-filter (filename fname))
                        (watch/on-change on-change!))))))

(defn stop! []
  (when @watcher
    (future-cancel @watcher)
    (reset! watcher nil)))

(defn set-tournament-tracking! [tournament-id tracking]
  (swap! state assoc-in [tournament-id :tracking] tracking))

(defn reset-tournament! [tournament-id]
  (swap! state assoc-in [tournament-id :uploaded :teams] false)
  (swap! state assoc-in [tournament-id :uploaded :seatings] false)
  (swap! state assoc-in [tournament-id :uploaded :pairings] {})
  (swap! state assoc-in [tournament-id :uploaded :results] {}))

(defn save-state! [filename]
  (let [s (joda-date->java-date @state)] 
    (spit filename (pr-str s))))

(defn load-state! [filename]
  (let [data (if (.exists (as-file filename))
               (->> (slurp filename)
                 edn/read-string
                 java-date->joda-date
                 (map-keys int))
               {})] 
    (reset! state data)))