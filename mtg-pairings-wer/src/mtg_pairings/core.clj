(ns mtg-pairings.core
  (:gen-class)
  (:use seesaw.core)
  (:use seesaw.chooser)
  (:use mtg-pairings.watcher)
  (:require [cheshire.core :refer :all])
  (:import (java.io File FileNotFoundException)))

(native!)


(def database-location (text :editable? false :text "%AppData%\\Wizards of the Coast\\Event Reporter\\TournamentData.dat"))

(def server-address  (text :editable? false :text "http://mtgsuomi.fi/pairings/api"))

(def address-checkbox (checkbox :text "Edit address" :selected? false 
                        :listen [:action (fn [event] (config! server-address :editable? (config address-checkbox :selected?)))]))


(defn select-file [event]
  (if @watcher (stop!))
  (text! database-location 
         (choose-file :success-fn (fn [fc file] (.getAbsolutePath file))
                    :cancel-fn (fn [fc] (text database-location) )))
  (if (.exists (File. (text database-location)))
   (start! (text database-location) )
   (-> (dialog :content "Error opening file") pack! show!)))


(def database-button (button :text "..."
                       :listen [:action select-file]))

(defn upload-results []
  ())

(def upload-button (button :text "Upload"
                     :listen [:action upload-results]))

(def tournament-label (text :text "" :editable? false))

(def tournament-listbox (listbox :model "none"))
  
(def tournament-selector
  (frame
    :on-close :dispose
    :title "Select tournament"
    :content (border-panel
               :vgap 5
               :hgap 5
               :border ""
               :north tournament-listbox
               :south (button :text "OK" :listen [:action (fn [event] (-> tournament-selector hide!)
                                                            (text! tournament-label (selection tournament-listbox)  ) )]))))

(defn select-tournament [event]
  (if @watcher
    (do (config! tournament-listbox :model (for [tournaments (get-tournaments)] 
                                             (str (:name tournaments) ", id: " (:id tournaments)) ))
          (-> tournament-selector pack! show! ))
    (-> (dialog :content "No database opened") pack! show!)))

(def tournament-button (button :text "Select"
                         :listen [:action select-tournament]))

(def api-key (text ""))
 
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


(def about-action (menu-item :text "About" :listen [:action about-handler]))

(def exit-action (menu-item :text "Exit" :listen [:action exit-handler]))

(def pairings-checkbox (checkbox :text "New pairings" :selected? false))

(def results-checkbox (checkbox :text "Standings" :selected? false))



(def main-window
  (frame
    :on-close :dispose
    :title "MTG pairings database sniffer"
    :size [300 :by 300]
    :menubar (menubar :items
               [(menu :text "File" :items [about-action exit-action])]) 
    :content (grid-panel 
               :columns 3
               :border "Settings"
               :vgap 5
               :hgap 5
               :items ["Database file:" database-location database-button
                       "Server address:" server-address address-checkbox
                       "API key:" api-key ""
                       "Tournament:" tournament-label tournament-button
                       "" "" ""
                       "Upload results:" upload-button pairings-checkbox
                       "" "" results-checkbox])
               ))


(defn -main []
 (-> main-window pack! show!)
 (if @watcher (stop!))
 (if (.exists (File. (text database-location))) 
   (start! (text database-location) )
   (-> (dialog :content "Database not found at default location, please set database location.") pack! show!)))
