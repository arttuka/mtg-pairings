(ns mtg-pairings.core
  (:gen-class)
  (:use seesaw.core)
  (:use seesaw.chooser))

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
                       "" "" ""
                       "Upload results:" upload-button])
               ))


(defn -main []
 (invoke-later
   (-> (main-window)
     pack!
     show!)))
