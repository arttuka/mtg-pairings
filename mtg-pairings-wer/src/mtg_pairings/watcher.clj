(ns mtg-pairings.watcher
  (:require [watchtower.core :as watch]
            [mtg-pairings.db-reader :as reader]
            [clojure.tools.reader.edn :as edn]
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
                                     :pairings []
                                     :results []
                                     :tracking false}]))]
    (swap! state merge new-tournaments)
    (doseq [[id t] new-tournaments]
      (handler id))))

(defn ^:private round-done? [results]
  (not-any? neg? (map :team1_wins results)))

(defn ^:private changed-index [idx [old new]]
  (when (not= old new) (inc idx)))

(defn ^:private changed-round [old-values new-values]
  (if (< (count old-values) (count new-values))
    (count new-values)
    (let [changed-rounds (keep-indexed changed-index (zip old-values new-values))]
      (first changed-rounds))))

(defn ^:private tournament-checker [handlers]
  (letfn [(handler [type values tournament-id]
            (let [f (get handlers type)
                  old-values (get-in @state [tournament-id type])
                  round (changed-round old-values values)]
              (when (not= old-values values)
                (swap! state assoc-in [tournament-id type] values)
                (when f
                  (case type
                    :teams (f tournament-id)
                    :seatings (f tournament-id)
                    :pairings (f tournament-id round)
                    :results (f tournament-id round))))))]
    (fn [db tournament-id]
      (handler :teams (reader/teams db tournament-id) tournament-id)
      (handler :seatings (reader/seatings db tournament-id) tournament-id)
      (handler :pairings (reader/pairings db tournament-id) tournament-id)
      (handler :results (filter round-done? (reader/results db tournament-id)) tournament-id))))

(defn get-tournament [id]
  (let [tournament (get @state id)]
    (select-keys tournament [:name :rounds :day :sanctionid])))

(defn get-pairings [id round]
  (nth (get-in @state [id :pairings]) (dec round) nil))

(defn get-pairings-count [id]
  (count (get-in @state [id :pairings])))

(defn get-seatings [id]
  (seq (get-in @state [id :seatings])))

(defn get-results [id round]
  (nth (get-in @state [id :results]) (dec round) nil))

(defn get-results-count [id]
  (count (get-in @state [id :results])))

(defn get-teams [id]
  (seq (get-in @state [id :teams])))

(defn get-tournaments [] (for [[id tournament] @state] 
                           (-> tournament (select-keys [:name :rounds :day]) (assoc :id id))))

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

(defn save-state! [filename]
  (spit filename (pr-str @state)))

(defn load-state! [filename]
  (reset! state (edn/read-string (slurp filename))))