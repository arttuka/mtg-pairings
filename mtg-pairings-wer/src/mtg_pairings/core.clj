(ns mtg-pairings.core
  (:gen-class)
  (:require [cheshire.generate :as json-gen]
            [seesaw.core :as swing]
            [seesaw.color :as color]
            [seesaw.table :as table]
            [seesaw.chooser :as chooser]
            [seesaw.mig :as mig]
            [mtg-pairings.watcher :as watcher]
            [mtg-pairings.uploader :as uploader]
            [mtg-pairings.ui :as ui]
            [clojure.java.io :refer [as-file]]
            [clojure.tools.reader.edn :as edn]
            [org.httpkit.client :as http])
  (:import (java.io File FileNotFoundException)
           java.awt.Desktop
           java.net.URI
           javax.swing.table.TableRowSorter
           org.joda.time.LocalDate))

(def version "0.2.0")

(defonce state (atom {:properties {}}))

(defn save-state! []
  (spit "properties.edn" (pr-str (:properties @state)))
  (watcher/save-state! "wer.edn"))

(def default-properties {:db (str (System/getenv "APPDATA") "\\Wizards of the Coast\\Event Reporter\\TournamentData.dat")
                         :url "http://mtgsuomi.fi/pairings"
                         :api-key ""})

(defn property [key]
  (get-in @state [:properties key]))

(defn set-property! [key value]
  (swap! state assoc-in [:properties key] value))

(defn enabled-buttons [tournament-id]
  {:tournament (watcher/get-tournament tournament-id)
   :teams (watcher/get-teams tournament-id)
   :seatings (watcher/get-seatings tournament-id)
   :pairings (pos? (watcher/get-pairings-count tournament-id))
   :results (pos? (watcher/get-results-count tournament-id))})

(defmacro action [action info tournament-id & params]
  `(fn [_#]
     (ui/disable-buttons ~tournament-id)
     (let [sanctionid# (:sanctionid (watcher/get-tournament ~tournament-id))
           settings# (assoc (:properties @state) :sanction-id sanctionid#)] 
       (~action settings# ~tournament-id ~@params #(do 
                                                     (ui/display-info ~tournament-id ~info) 
                                                     (ui/enable-buttons ~tournament-id (enabled-buttons ~tournament-id))
                                                     (save-state!))))))

(defn create-actions [tournament-id]
  {:tournament (action uploader/upload-tournament! "Turnaus lähetetty" tournament-id)
   :teams (action uploader/upload-teams! "Tiimit lähetetty" tournament-id)
   :seatings (action uploader/upload-seatings! "Seatingit lähetetty" tournament-id)
   :pairings (action uploader/upload-pairings! "Pairingit lähetetty" tournament-id (ui/selected-pairings tournament-id))
   :results (action uploader/upload-results! "Tulokset lähetetty" tournament-id (ui/selected-results tournament-id))
   :publish (action uploader/publish-results! "Tulokset julkaistu" tournament-id (ui/selected-results tournament-id))
   :check (fn [_] (ui/check-results-window (watcher/get-missing-results tournament-id)))
   :reset (action uploader/reset-tournament! "Turnaus resetoitu" tournament-id)})

(defn track-tournament! [tournament-id track?]
  (watcher/set-tournament-tracking! tournament-id track?)
  (if track?
    (let [name (:name (watcher/get-tournament tournament-id))
          actions (create-actions tournament-id)]
      (ui/add-tab tournament-id name actions)
      (ui/disable-buttons tournament-id)
      (ui/enable-buttons tournament-id (enabled-buttons tournament-id)))
    (ui/remove-tab tournament-id)))

(declare track-tournament-fn)

(defn update-tournaments! []
  (let [tournaments (watcher/get-tournaments) 
        table-model (ui/tournament-table-model tournaments track-tournament-fn)]
    (ui/update-tournament-table-model! table-model)))

(defn track-tournament-fn [tournament-id track?]
  (track-tournament! tournament-id track?)
  (update-tournaments!))

(defn tournament-handler [id]
  (update-tournaments!)
  (ui/enable-buttons id (enabled-buttons id))) 

(defn teams-handler [id]
  (ui/enable-buttons id (enabled-buttons id))) 

(defn pairings-handler [id round]
  (ui/update-pairings-combo id (watcher/get-pairings-count id))
  (ui/enable-buttons id (enabled-buttons id))) 

(defn results-handler [id round]
  (ui/update-results-combo id (watcher/get-results-count id))
  (ui/enable-buttons id (enabled-buttons id))) 

(defn seatings-handler [id]
  (ui/enable-buttons id (enabled-buttons id))) 
 
(defn exit-handler [event]
  (System/exit 0))

(defn get-version-from-server []
  (let [url (str (get-in @state [:properties :url]) "/version")]
    (-> @(http/get url) :body slurp edn/read-string :client)))

(defn -main []
  (json-gen/add-encoder org.joda.time.LocalDate
    (fn [c generator]
      (.writeString generator (str c))))
  (let [prop-file (as-file "properties.edn")]
    (swap! state assoc :properties (if (.exists prop-file)
                                     (merge default-properties
                                            (edn/read-string (slurp prop-file)))
                                     default-properties)))
  (watcher/stop!)
  (watcher/load-state! "wer.edn")
  (ui/main-window state)
  (let [tournaments (watcher/get-tournaments)]
    (update-tournaments!)
    (doseq [tournament tournaments
            :let [id (:id tournament)]
            :when (:tracking tournament)]
      (ui/add-tab id (:name tournament) (create-actions id))
      (ui/enable-buttons id (enabled-buttons id))
      (ui/update-pairings-combo id (watcher/get-pairings-count id))
      (ui/update-results-combo id (watcher/get-results-count id))))
  (when (clojure.string/blank? (property :api-key))
    (ui/apikey-window state))
  (when (not= version (get-version-from-server))
    (ui/new-version-window))
  (let [database-location (if (.exists (as-file (property :db)))
                            (property :db)
                            (chooser/choose-file :success-fn (fn [_ file] (.getAbsolutePath file))
                                         :cancel-fn (constantly nil)))]
    (watcher/start! database-location 
                    :tournament tournament-handler 
                    :teams teams-handler 
                    :pairings pairings-handler 
                    :results results-handler 
                    :seatings seatings-handler)))

