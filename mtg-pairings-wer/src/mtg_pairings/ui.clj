(ns mtg-pairings.ui
  (:require [seesaw.core :as swing]
            [seesaw.color :as color]
            [seesaw.table :as table]
            [seesaw.chooser :as chooser]
            [seesaw.mig :as mig])
  (:import (java.io File FileNotFoundException)
           java.awt.Desktop
           java.net.URI
           javax.swing.table.TableRowSorter
           org.joda.time.LocalDate))

(declare tabbed-pane)

(swing/native!)

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

(defn info-label-id [tournament-id]
  (keyword (str "infolabel" tournament-id)))

(defn disable-buttons [tournament-id]
  (doseq [btn (swing/select (swing/to-root tabbed-pane) [(id->selector (tab-id tournament-id)) :JButton])]
    (swing/config! btn :enabled? false))
  (-> (swing/select (swing/to-root tabbed-pane) [(id->selector (reset-button-id tournament-id))])
    (swing/config! :enabled? true)))

(defn update-pairings-combo [tournament-id pairings-count]
  (let [combo (swing/select (swing/to-root tabbed-pane) [(id->selector (pairings-combo-id tournament-id))])]
    (swing/config! combo :model (range 1 (inc pairings-count)))
    (swing/selection! combo (long pairings-count))))

(defn update-results-combo [tournament-id results-count]
  (let [combo (swing/select (swing/to-root tabbed-pane) [(id->selector (results-combo-id tournament-id))])]
    (swing/config! combo :model (range 1 (inc results-count)))
    (swing/selection! combo (long results-count))))

(defn enable-buttons [tournament-id {:keys [tournament teams seatings pairings results]}]
  (let [root (swing/to-root tabbed-pane)] 
    (when tournament
      (-> (swing/select root [(id->selector (tournament-button-id tournament-id))])
        (swing/config! :enabled? true)))
    (when teams
      (-> (swing/select root [(id->selector (teams-button-id tournament-id))])
        (swing/config! :enabled? true)))
    (when seatings
      (-> (swing/select root [(id->selector (seatings-button-id tournament-id))])
        (swing/config! :enabled? true)))
    (when pairings
      (-> (swing/select root [(id->selector (pairings-button-id tournament-id))])
        (swing/config! :enabled? true)))
    (when results
      (-> (swing/select root [(id->selector (results-button-id tournament-id))])
        (swing/config! :enabled? true))
      (-> (swing/select root [(id->selector (publish-button-id tournament-id))])
        (swing/config! :enabled? true)))
    (-> (swing/select root [(id->selector (reset-button-id tournament-id))])
      (swing/config! :enabled? true))
    (-> (swing/select root [(id->selector (check-button-id tournament-id))])
      (swing/config! :enabled? true))))

(defn create-tab [tournament-id actions]
  (let [pairings-combo (swing/combobox :model []
                                       :id (pairings-combo-id tournament-id))
        results-combo (swing/combobox :model []
                                      :id (results-combo-id tournament-id))
        info (swing/label :id (info-label-id tournament-id)
                          :text "")
        tab (mig/mig-panel :constraints ["fill, wrap 3" 
                                         "[100!][100!][200!]" 
                                         ""]
                           :id (tab-id tournament-id)
                           :items [[(swing/label "Turnaus")]
                                   [(swing/label "")]
                                   [(swing/button :text "Lähetä"
                                                  :listen [:action (:tournament actions)]
                                                  :id (tournament-button-id tournament-id)
                                                  :enabled? false)]
                                   
                                   [(swing/label "Tiimit")]
                                   [(swing/label "")]
                                   [(swing/button :text "Lähetä"
                                                  :listen [:action (:teams actions)]
                                                  :id (teams-button-id tournament-id)
                                                  :enabled? false)]
                                   
                                   [(swing/label "Seatingit")]
                                   [(swing/label "")]
                                   [(swing/button :text "Lähetä"
                                                  :listen [:action (:seatings actions)]
                                                  :id (seatings-button-id tournament-id)
                                                  :enabled? false)]
                                   
                                   [(swing/label "Pairingit")]
                                   [pairings-combo]
                                   [(swing/button :text "Lähetä"
                                                  :listen [:action (:pairings actions)]
                                                  :id (pairings-button-id tournament-id)
                                                  :enabled? false)]
                                   
                                   [(swing/label "Tulokset")]
                                   [results-combo]
                                   [(swing/vertical-panel :items [(swing/button :text "Lähetä"
                                                                                :listen [:action (:results actions)]
                                                                                :id (results-button-id tournament-id)
                                                                                :enabled? false)
                                                                  (swing/button :text "Julkaise"
                                                                                :listen [:action (:publish actions)]
                                                                                :id (publish-button-id tournament-id)
                                                                                :enabled? false)
                                                                  (swing/button :text "Tarkista"
                                                                                :listen [:action (:check actions)]
                                                                                :id (check-button-id tournament-id)
                                                                                :enabled? false)])]
                                   
                                   [(swing/label "Resetoi")]
                                   [(swing/label "")]
                                   [(swing/button :text "Resetoi"
                                                  :listen [:action (:reset actions)]
                                                  :id (reset-button-id tournament-id)
                                                  :enabled? false)]
                                   
                                   [info "span 3"]])]
    tab))

(defn selected-pairings [tournament-id]
  (-> 
    (swing/to-root tabbed-pane)
    (swing/select [(id->selector (pairings-combo-id tournament-id))])
    swing/selection))

(defn selected-results [tournament-id]
  (-> 
    (swing/to-root tabbed-pane)
    (swing/select [(id->selector (results-combo-id tournament-id))])
    swing/selection))

(defn tournament-table-model [tournaments track-fn]
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
            (track-fn (:id tournament) value))))
      (getColumnName [col]
        (column {:name "Nimi"
                 :day "Päivä"
                 :tracking "Seuranta"}
                col)))))

(defn set-column-width [table column width]
  (-> table .getColumnModel (.getColumn column) (.setPreferredWidth width)))

(def tournament-table 
  (swing/table :show-vertical-lines? false))

(def local-date-comparator (fn [^LocalDate this ^LocalDate that]
                             (.compareTo this that)))

(defn update-tournament-table-model! [table-model]
  (let [row-sorter (doto (TableRowSorter. table-model)
                     (.setComparator 1 local-date-comparator)
                     (.setSortable 2 false))]
    (doto tournament-table
      (.setModel table-model)
      (.setRowSorter row-sorter)
      (set-column-width 0 230)
      (set-column-width 1 100)
      (set-column-width 2 70))))

(def tabbed-pane (swing/tabbed-panel :tabs [{:title "Turnaukset"
                                          :content (swing/scrollable tournament-table)}]))
(defn add-tab [tournament-id name actions]
  (.addTab tabbed-pane name (create-tab tournament-id actions)))

(defn remove-tab [tournament-id]
  (let [tab (swing/select (swing/to-root tabbed-pane) [(id->selector (tab-id tournament-id))])]
    (.remove tabbed-pane tab)))

(defn display-info [tournament-id text]
  (->
    (swing/to-root tabbed-pane)
    (swing/select [(id->selector (info-label-id tournament-id))])
    (swing/text! text)))

(defn check-results-window [missing-results]
  (-> 
    (swing/dialog
      :type :info
      :content (clojure.string/join \newline (cons "Seuraavien pöytien tulos puuttuu:" missing-results)))
    swing/pack!
    swing/show!))

(defn new-version-window []
  (-> 
    (swing/dialog
      :type :warning
      :content "Pairings-ohjelmasta on uusi versio. Lataa uusi versio painamalla OK."
      :success-fn (fn [p] 
                    (.browse (Desktop/getDesktop) (URI. "http://www.mtgsuomi.fi/mtg-pairings.zip"))
                    (System/exit 0)))
    swing/pack!
    swing/show!))

(defn apikey-window [state]
  (->
    (swing/dialog
      :option-type :ok-cancel
      :title "API key"
      :size [300 :by 100]
      :content (swing/text :id :key
                           :text (get-in @state [:properties :api-key]))
      :success-fn (fn [p] (let [value (swing/text (swing/select (swing/to-root p) [:#key]))]
                            (swap! state assoc-in [:properties :api-key] value))))
    swing/pack!
    swing/show!))

(defn main-window [state]
  (let [apikey-action (swing/menu-item :text "API key..."
                                       :listen [:action (fn [_] (apikey-window state))])
        exit-action (swing/menu-item :text "Sulje"
                                     :listen [:action (fn [_] (System/exit 0))])] 
    (-> 
      (swing/frame
        :on-close :exit
        :title "MtgSuomi Pairings"
        :size [300 :by 300]
        :menubar (swing/menubar :items [(swing/menu :text "Asetukset" :items [apikey-action exit-action])]) 
        :content (mig/mig-panel
                   :constraints ["fill, wrap 1" 
                                 "[500!]" 
                                 ""]
                   :items [[tabbed-pane "grow"]]))
      swing/pack!
      swing/show!)))