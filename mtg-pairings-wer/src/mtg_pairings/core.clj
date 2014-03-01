(ns mtg-pairings.core
  (:gen-class)
  (:use seesaw.core)
  (:use seesaw.chooser)
  (:require [cheshire.core :refer :all]
            [cheshire.generate :as json-gen]
            [seesaw.table :as table]
            [seesaw.mig :as mig]
            [mtg-pairings.watcher :as watcher]
            [mtg-pairings.uploader :as uploader]
            [clojure.java.io :refer [as-file]]
            [clojure.tools.reader.edn :as edn])
  (:import (java.io File FileNotFoundException)))

(native!)

(defonce state (atom {:properties {}}))

(def default-properties {:db (str (System/getenv "APPDATA") "\\Wizards of the Coast\\Event Reporter\\TournamentData.dat")
                         :server "http://mtgsuomi.fi/pairings"
                         :api-key ""})

(let [prop-file (as-file "properties.edn")]
  (swap! state assoc :properties (if (.exists prop-file)
                                   (merge default-properties
                                          (edn/read-string (slurp prop-file)))
                                   default-properties)))

(defn property [key]
  (get-in @state [:properties key]))

(defn set-property! [key value]
  (swap! state assoc-in [:properties key] value))

(defn tournament-table-model [tournaments]
  (letfn [(column [value num]
            (case num
              0 (:name value)
              1 (:day value)
              2 (:tracking value)))] 
    (doto (proxy
            [javax.swing.table.AbstractTableModel] []
            (getRowCount []
              (count tournaments))
            (getColumnCount []
              3)
            (getValueAt [row col]
              (-> tournaments
                (nth row)
                (column col)))
            (getColumnClass [col]
              (if (= col 2)
                Boolean
                String))
            (isCellEditable [row col]
              (= col 2))
            (setValueAt [value row col]
              (let [tournament (nth tournaments row)] 
                (when (= col 2)
                  (watcher/set-tournament-tracking! (:id tournament) value)
                  (update-tournaments! (watcher/get-tournaments)))))
            (getColumnName [col]
              (column {:name "Nimi"
                       :day "Päivä"
                       :tracking "Seuranta"}
                      col))))))
  #_(table/table-model :rows tournaments
                      :columns [{:key :name, :text "Turnaus"}
                                {:key :day, :text "Päivä"}
                                {:key :tracking, :text "Seuranta", :class Boolean}])

(defn set-column-width [table column width]
  (-> table .getColumnModel (.getColumn column) (.setPreferredWidth width)))

(def tournament-table (doto (table :model (tournament-table-model (watcher/get-tournaments))
                                   :show-vertical-lines? false)
                        (set-column-width 0 230)
                        (set-column-width 1 100)
                        (set-column-width 2 70)))

(def updates-panel (vertical-panel :items []))

(defn update-tournaments! [tournaments]
  (config! tournament-table :model (tournament-table-model tournaments)))

(defn create-upload-panel [id type & [round]]
  (let [tournament (watcher/get-tournament id)
        text (case type
               :tournament (str "Uusi turnaus:\n" (:name tournament))
               :seatings (str "Uudet seatingit turnauksessa\n" (:name tournament))
               :teams (str "Uudet tiimit turnauksessa\n" (:name tournament))
               :pairings (str "Uudet pairingit turnauksessa\n" (:name tournament) ", kierros " round)
               :results (str "Uudet tulokset turnauksessa\n" (:name tournament) ", kierros " round))
        server (property :server)
        api-key (property :api-key)
        sanction-id (:sanctionid tournament)
        panel-id (keyword (str id "-" (name type) "-" round))
        callback (fn [response] 
                   (println (:status response))
                   (when (= 204 (:status response)) 
                     (remove! updates-panel 
                              (select updates-panel [(keyword (str "#" (name panel-id)))]))))
        action (fn [_] 
                 (case type
                   :tournament (uploader/upload-tournament! server api-key tournament callback)
                   :seatings (uploader/upload-seatings! server sanction-id api-key (watcher/get-seatings id) callback)
                   :teams (uploader/upload-teams! server sanction-id api-key (watcher/get-teams id) callback)
                   :pairings (uploader/upload-pairings! server sanction-id round api-key (watcher/get-pairings id round) callback)
                   :results (uploader/upload-results! server sanction-id round api-key (watcher/get-results id round) callback)))
        panel (horizontal-panel :items [text (button :text "Lähetä" :listen [:action action])]
                                :id panel-id)]
    (add! updates-panel panel)))

(defn tournament-handler [id]
  (update-tournaments! (watcher/get-tournaments))
  (create-upload-panel id :tournament)) 

(defn teams-handler [id]
  (create-upload-panel id :teams)) 

(defn pairings-handler [id round]
  (create-upload-panel id :pairings round)) 

(defn results-handler [id round]
  (create-upload-panel id :results round)) 

(defn seatings-handler [id]
  (create-upload-panel id :seatings)) 

(def about-window
  (frame
    :on-close :dispose
    :title "About MTG pairings"
    :size [100 :by 100]
    :content (border-panel 
               :vgap 5
               :hgap 5
               :border ""
               :north "This is the WER database sniffer and result uploader"
               :south (button :text "Close" :listen [:action (fn [event] (-> about-window hide!))]))))
  
(defn about-handler [event]
  (-> about-window pack! show!))

(defn exit-handler [event]
  (System/exit 0))

(defn apikey-window []
  (dialog
    :option-type :ok-cancel
    :title "API key"
    :size [300 :by 100]
    :content (text :id :key
                   :text (property :api-key))
    :success-fn (fn [p] (let [value (text (select (to-root p) [:#key]))]
                          (set-property! :api-key value)))))

(defn apikey-handler [& args]
  (-> (apikey-window) pack! show!))

(def about-action (menu-item :text "About" :listen [:action about-handler]))

(def apikey-action (menu-item :text "API key..." :listen [:action apikey-handler]))

(def exit-action (menu-item :text "Exit" :listen [:action exit-handler]))

(def main-window
  (frame
    :on-close :dispose
    :title "MtgSuomi Pairings"
    :size [300 :by 300]
    :menubar (menubar :items
               [(menu :text "Asetukset" :items [apikey-action exit-action])]) 
    :content (mig/mig-panel
               :constraints ["fill, wrap 3" 
                             "[100!][300!][300!]" 
                             ""]
               :items [[(scrollable tournament-table) "span 2, grow"] [updates-panel "grow"]])))

(defn -main []
  (json-gen/add-encoder org.joda.time.LocalDate
    (fn [c generator]
      (.writeString generator (str c))))
  (-> main-window pack! show!)
  (watcher/stop!)
  (when (clojure.string/blank? (property :api-key))
    (apikey-handler))
  (let [database-location (if (.exists (as-file (property :db)))
                            (property :db)
                            (choose-file :success-fn (fn [_ file] (.getAbsolutePath file))
                                         :cancel-fn (constantly nil)))]
    (watcher/start! database-location 
                    :tournament tournament-handler 
                    :teams teams-handler 
                    :pairings pairings-handler 
                    :results results-handler 
                    :seatings seatings-handler)))
