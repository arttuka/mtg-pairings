(ns mtg-pairings.core
  (:gen-class)
  (:use seesaw.core)
  (:use seesaw.chooser)
  (:use mtg-pairings.db-reader)
  (:require [cheshire.core :refer :all]))

(native!)


(def database-location (text ""))

(def server-address  (text ""))


(defn select-file [event]
  (text! database-location 
         (choose-file :success-fn (fn [fc file] (.getAbsolutePath file)))))


(def database-button (button :text "..."
                       :listen [:action select-file]))

(defn upload-results []
  ())

(def upload-button (button :text "Upload"
                     :listen [:action upload-results]))

(def tournament-label (text ""))

(def tournament-selector "")

(defn select-tournament [event]
  ())

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



(defn main-window []
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
                       "Server address:" server-address ""
                       "API key:" api-key ""
                       "Tournament:" tournament-label tournament-button
                       "" "" ""
                       "Upload results:" upload-button pairings-checkbox
                       "" "" results-checkbox])
               ))


(defn -main []
 (invoke-later
   (-> (main-window)
     pack!
     show!)))
