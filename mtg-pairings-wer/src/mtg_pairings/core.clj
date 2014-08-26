(ns mtg-pairings.core
  (:gen-class)
  (:require [cheshire.generate :as json-gen]
            [seesaw.core :as ui]
            [seesaw.color :as color]
            [seesaw.table :as table]
            [seesaw.chooser :as chooser]
            [seesaw.mig :as mig]
            [mtg-pairings.watcher :as watcher]
            [mtg-pairings.uploader :as uploader]
            [clojure.java.io :refer [as-file]]
            [clojure.tools.reader.edn :as edn]
            [org.httpkit.client :as http])
  (:import (java.io File FileNotFoundException)
           java.awt.Desktop
           java.net.URI))


(ui/native!)

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

(declare update-tournaments! tabbed-pane)

(defn id->selector [id]
  (keyword (str "#" (name id))))

(defn tab-id [tournament-id]
  (keyword (str "tab" tournament-id)))

(defn pairings-round-id [tournament-id round]
  (keyword (str "pairings" tournament-id "-" round)))

(defn results-round-id [tournament-id round]
  (keyword (str "results" tournament-id "-" round)))

(defn tournament-button-id [tournament-id]
  (keyword (str "tournament" tournament-id)))

(defn teams-button-id [tournament-id]
  (keyword (str "teams" tournament-id)))

(defn seatings-button-id [tournament-id]
  (keyword (str "seatings" tournament-id)))

(defn reset-button-id [tournament-id]
  (keyword (str "reset" tournament-id)))

(defn pairings-button-id [tournament-id]
  (keyword (str "pairings" tournament-id)))

(defn results-button-id [tournament-id]
  (keyword (str "results" tournament-id)))

(defn publish-button-id [tournament-id]
  (keyword (str "publish" tournament-id)))

(defn check-button-id [tournament-id]
  (keyword (str "check" tournament-id)))

(defn pairings-combo-id [tournament-id]
  (keyword (str "pairingscombo" tournament-id)))

(defn results-combo-id [tournament-id]
  (keyword (str "resultscombo" tournament-id)))

(defn disable-buttons [tournament-id]
  (doseq [btn (ui/select (ui/to-root tabbed-pane) [(id->selector (tab-id tournament-id)) :JButton])]
    (ui/config! btn :enabled? false))
  (-> (ui/select (ui/to-root tabbed-pane) [(id->selector (reset-button-id tournament-id))])
    (ui/config! :enabled? true)))

(defn update-pairings-combo [tournament-id]
  (let [combo (ui/select (ui/to-root tabbed-pane) [(id->selector (pairings-combo-id tournament-id))])
        cnt (watcher/get-pairings-count tournament-id)]
    (ui/config! combo :model (range 1 (inc cnt)))
    (ui/selection! combo (long cnt))))

(defn update-results-combo [tournament-id]
  (let [combo (ui/select (ui/to-root tabbed-pane) [(id->selector (results-combo-id tournament-id))])
        cnt (watcher/get-results-count tournament-id)]
    (ui/config! combo :model (range 1 (inc cnt)))
    (ui/selection! combo (long cnt))))

(defn enable-buttons [tournament-id]
  (let [root (ui/to-root tabbed-pane)] 
    (when (watcher/get-tournament tournament-id)
      (-> (ui/select root [(id->selector (tournament-button-id tournament-id))])
        (ui/config! :enabled? true)))
    (when (watcher/get-teams tournament-id)
      (-> (ui/select root [(id->selector (teams-button-id tournament-id))])
        (ui/config! :enabled? true)))
    (when (watcher/get-seatings tournament-id)
      (-> (ui/select root [(id->selector (seatings-button-id tournament-id))])
        (ui/config! :enabled? true)))
    (when (pos? (watcher/get-pairings-count tournament-id))
      (-> (ui/select root [(id->selector (pairings-button-id tournament-id))])
        (ui/config! :enabled? true)))
    (when (pos? (watcher/get-results-count tournament-id))
      (-> (ui/select root [(id->selector (results-button-id tournament-id))])
        (ui/config! :enabled? true))
      (-> (ui/select root [(id->selector (publish-button-id tournament-id))])
        (ui/config! :enabled? true)))
    (-> (ui/select root [(id->selector (reset-button-id tournament-id))])
      (ui/config! :enabled? true))
    (-> (ui/select root [(id->selector (check-button-id tournament-id))])
      (ui/config! :enabled? true))))

(defmacro upload [action done tournament-id & params]
  `(fn [_#]
     (disable-buttons ~tournament-id)
     (let [sanctionid# (:sanctionid (watcher/get-tournament ~tournament-id))
           settings# (assoc (:properties @state) :sanction-id sanctionid#)] 
       (~action settings# ~tournament-id ~@params #(do 
                                                     ~done 
                                                     (enable-buttons ~tournament-id)
                                                     (save-state!))))))

(defn check-results-window [tournament-id]
  (fn [& _] 
    (-> 
      (ui/dialog
        :type :info
        :content (clojure.string/join \newline (cons "Seuraavien pöytien tulos puuttuu:" (watcher/get-missing-results tournament-id))))
      ui/pack!
      ui/show!)))

(defn create-tab [tournament-id]
  (let [pairings-combo (ui/combobox :model []
                                    :id (pairings-combo-id tournament-id))
        results-combo (ui/combobox :model []
                                   :id (results-combo-id tournament-id))
        info (ui/label "")
        info! (fn [& args] (ui/text! info (apply str args)))
        tab (mig/mig-panel :constraints ["fill, wrap 3" 
                                         "[100!][100!][200!]" 
                                         ""]
                           :id (tab-id tournament-id)
                           :items [[(ui/label "Turnaus")]
                                   [(ui/label "")]
                                   [(ui/button :text "Lähetä"
                                               :listen [:action (upload uploader/upload-tournament! (info! "Turnaus lähetetty") tournament-id)]
                                               :id (tournament-button-id tournament-id)
                                               :enabled? false)]
                                   
                                   [(ui/label "Tiimit")]
                                   [(ui/label "")]
                                   [(ui/button :text "Lähetä"
                                               :listen [:action (upload uploader/upload-teams! (info! "Tiimit lähetetty") tournament-id)]
                                               :id (teams-button-id tournament-id)
                                               :enabled? false)]
                                   
                                   [(ui/label "Seatingit")]
                                   [(ui/label "")]
                                   [(ui/button :text "Lähetä"
                                               :listen [:action (upload uploader/upload-seatings! (info! "Seatingit lähetetty") tournament-id)]
                                               :id (seatings-button-id tournament-id)
                                               :enabled? false)]
                                   
                                   [(ui/label "Pairingit")]
                                   [pairings-combo]
                                   [(ui/button :text "Lähetä"
                                               :listen [:action (upload uploader/upload-pairings! 
                                                                        (info! "Pairingit " (ui/selection pairings-combo) " lähetetty") 
                                                                        tournament-id 
                                                                        (ui/selection pairings-combo))]
                                               :id (pairings-button-id tournament-id)
                                               :enabled? false)]
                                   
                                   [(ui/label "Tulokset")]
                                   [results-combo]
                                   [(ui/vertical-panel :items [(ui/button :text "Lähetä"
                                                                          :listen [:action (upload uploader/upload-results! 
                                                                                                   (info! "Tulokset " (ui/selection pairings-combo) " lähetetty")
                                                                                                   tournament-id 
                                                                                                   (ui/selection results-combo))]
                                                                          :id (results-button-id tournament-id)
                                                                          :enabled? false)
                                                               (ui/button :text "Julkaise"
                                                                          :listen [:action (upload uploader/publish-results! 
                                                                                                   (info! "Tulokset " (ui/selection pairings-combo) " julkaistu")
                                                                                                   tournament-id 
                                                                                                   (ui/selection results-combo))]
                                                                          :id (publish-button-id tournament-id)
                                                                          :enabled? false)
                                                               (ui/button :text "Tarkista"
                                                                          :listen [:action (check-results-window tournament-id)]
                                                                          :id (check-button-id tournament-id)
                                                                          :enabled? false)])]
                                   
                                   [(ui/label "Resetoi")]
                                   [(ui/label "")]
                                   [(ui/button :text "Resetoi"
                                               :listen [:action (upload uploader/reset-tournament! (info! "Turnaus resetoitu") tournament-id)]
                                               :id (reset-button-id tournament-id)
                                               :enabled? false)]
                                   
                                   [info "span 3"]])]
    (disable-buttons tournament-id)
    (enable-buttons tournament-id)
    tab))

(defn track-tournament! [tournament-id track?]
  (watcher/set-tournament-tracking! tournament-id track?)
  (let [name (:name (watcher/get-tournament tournament-id))] 
    (if track?
      (.addTab tabbed-pane name (create-tab tournament-id))
      (let [tab (ui/select (ui/to-root tabbed-pane) [(id->selector (tab-id tournament-id))])]
        (.remove tabbed-pane tab)))))

(defn tournament-table-model [tournaments]
  (letfn [(column [value num]
            (case num
              0 (:name value)
              1 (:day value)
              2 (:tracking value)))] 
    (proxy
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
            (track-tournament! (:id tournament) value)
            (update-tournaments! (watcher/get-tournaments)))))
      (getColumnName [col]
        (column {:name "Nimi"
                 :day "Päivä"
                 :tracking "Seuranta"}
                col)))))

(defn set-column-width [table column width]
  (-> table .getColumnModel (.getColumn column) (.setPreferredWidth width)))

(def tournament-table (doto (ui/table :model (tournament-table-model (watcher/get-tournaments))
                                   :show-vertical-lines? false)
                        (set-column-width 0 230)
                        (set-column-width 1 100)
                        (set-column-width 2 70)))

(defn update-tournaments! [tournaments]
  (ui/config! tournament-table :model (tournament-table-model tournaments)))

(defn tournament-handler [id]
  (update-tournaments! (watcher/get-tournaments))
  (enable-buttons id)) 

(defn teams-handler [id]
  (enable-buttons id)) 

(defn pairings-handler [id round]
  (update-pairings-combo id)
  (enable-buttons id)) 

(defn results-handler [id round]
  (update-results-combo id)
  (enable-buttons id)) 

(defn seatings-handler [id]
  (enable-buttons id)) 

(def about-window
  (ui/frame
    :on-close :dispose
    :title "About MTG pairings"
    :size [100 :by 100]
    :content (ui/border-panel 
               :vgap 5
               :hgap 5
               :border ""
               :north "This is the WER database sniffer and result uploader"
               :south (ui/button :text "Close" :listen [:action (fn [event] (-> about-window ui/hide!))]))))
  
(defn about-handler [event]
  (-> about-window ui/pack! ui/show!))

(defn exit-handler [event]
  (System/exit 0))

(defn apikey-window []
  (ui/dialog
    :option-type :ok-cancel
    :title "API key"
    :size [300 :by 100]
    :content (ui/text :id :key
                      :text (property :api-key))
    :success-fn (fn [p] (let [value (ui/text (ui/select (ui/to-root p) [:#key]))]
                          (set-property! :api-key value)))))

(defn apikey-handler [& args]
  (-> (apikey-window) ui/pack! ui/show!))

(defn new-version-window []
  (ui/dialog
    :type :warning
    :content "Pairings-ohjelmasta on uusi versio. Lataa uusi versio painamalla OK."
    :success-fn (fn [p] 
                  (.browse (Desktop/getDesktop) (URI. "http://www.mtgsuomi.fi/mtg-pairings.zip"))
                  (System/exit 0))))

(defn new-version-handler []
  (-> (new-version-window) ui/pack! ui/show!))

(defn get-version-from-server []
  (let [url (str (-> @state :properties :url) "/version")]
    (-> @(http/get url) :body slurp edn/read-string :client)))

(def about-action (ui/menu-item :text "About" :listen [:action about-handler]))

(def apikey-action (ui/menu-item :text "API key..." :listen [:action apikey-handler]))

(def exit-action (ui/menu-item :text "Sulje" :listen [:action exit-handler]))

(def tabbed-pane (ui/tabbed-panel :tabs [{:title "Turnaukset"
                                          :content (ui/scrollable tournament-table)}]))

(def main-window
  (ui/frame
    :on-close :exit
    :title "MtgSuomi Pairings"
    :size [300 :by 300]
    :menubar (ui/menubar :items
               [(ui/menu :text "Asetukset" :items [apikey-action exit-action])]) 
    :content (mig/mig-panel
               :constraints ["fill, wrap 1" 
                             "[500!]" 
                             ""]
               :items [[tabbed-pane "grow"]])))

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
  (-> main-window ui/pack! ui/show!)
  (let [tournaments (watcher/get-tournaments)]
    (update-tournaments! tournaments)
    (doseq [tournament tournaments
            :let [id (:id tournament)]
            :when (:tracking tournament)]
      (.addTab tabbed-pane (:name tournament) (create-tab id))
      (enable-buttons id)
      (update-pairings-combo id)
      (update-results-combo id)))
  (when (clojure.string/blank? (property :api-key))
    (apikey-handler))
  (when (not= version (get-version-from-server))
    (new-version-handler))
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
