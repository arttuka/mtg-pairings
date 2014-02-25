(ns mtg-pairings.watcher
  (:require [watchtower.core :as watch]
            [mtg-pairings.db-reader :as reader]
            [clojure.tools.reader.edn :as edn]
            [mtg-pairings.util :refer [filename]])
  (:import (java.io File FileNotFoundException)))

(defonce watcher (atom nil))
(defonce state (atom {}))

(defn ^:private check-tournaments! [db handler]
  (let [tournaments (reader/tournaments db {:IsStarted true})
        new-tournaments (remove #((set (keys @state)) (:TournamentId %)) tournaments)
        new-tournaments (into {} (for [tournament new-tournaments]
                                   [(:TournamentId tournament)
                                    {:name (:Name tournament)
                                     :rounds (:NumberOfRounds tournament)
                                     :teams {:uploaded []
                                             :waiting []}
                                     :seatings {:uploaded []
                                                :waiting []}
                                     :pairings {:uploaded []
                                                :waiting []}
                                     :results {:uploaded []
                                               :waiting []}
                                     :tracking false}]))]
    (swap! state merge new-tournaments)
    (doseq [[id t] new-tournaments]
      (handler id))))

(defn ^:private round-done [results]
  (not-any? neg? (map :team1_wins results)))

(defn ^:private set-uploaded! [k]
  (fn [tournament]
    (let [data (get-in tournament [k :waiting])]
      (if (seq data)
        (-> tournament
          (assoc-in [k :waiting] [])
          (assoc-in [k :uploaded] data))
        tournament))))

(defn ^:private tournament-checker [handlers]
  (fn [db tournament-id]
    (println "Checking tournament" tournament-id)
    (letfn [(handler [type values]
              (let [f (get handlers type)]
                (when (and (not= (get-in @state [tournament-id type :uploaded]) values)
                           (not= (get-in @state [tournament-id type :waiting]) values))
                  (swap! state assoc-in [tournament-id type :waiting] values)
                  (when f
                    (f tournament-id)))))]
      (handler :teams (reader/teams db tournament-id))
      (handler :seatings (reader/seatings db tournament-id))
      (handler :pairings (reader/pairings db tournament-id))
      (handler :results (filter round-done (reader/results db tournament-id))))))

(defn start! [path & {:as handlers}]
  (let [file (File. path)
        fname (.getName file)
        checker (tournament-checker handlers)
        on-change (fn [files]
                    (doseq [f files
                            :when (= fname (.getName f))]
                      (with-open [db (reader/open (.getPath f))]
                        (check-tournaments! db (:tournament handlers))
                        (doseq [[id tourn] @state
                                :when (:tracking tourn)]
                          (checker db id)))))]
    (if-not (.exists file)
      (throw (FileNotFoundException. path))
      (reset! watcher (watch/watcher [(.getParent file)]
                        (watch/rate 100)
                        (watch/file-filter (filename fname))
                        (watch/on-change on-change))))))

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