(ns mtg-pairings.core
  (:gen-class)
  (:use seesaw.core)
  (:use seesaw.chooser))

(native!)


(def database-location (text "Insert database location here."))

(def server-address  (text "Insert server address here."))

(defn select-file []
  (text! database-location 
         (choose-file :success-fn (fn [fc file] (.getAbsolutePath file)))))

(def database-button (button :text "..."
                       :listen [:action #(select-file)]))
  
 

(defn main-window []
  (frame
    :on-close :dispose
    :title "MTG pairings database sniffer"
    :size [300 :by 300]
    :content (grid-panel 
               :columns 3
               :border "Settings"
               :vgap 5
               :hgap 5
               :items ["Database file" database-location database-button
                       "Server address" server-address])
               ))


(defn -main []
 (invoke-later
   (-> (main-window)
     pack!
     show!)))