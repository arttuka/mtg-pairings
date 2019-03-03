(ns mtg-pairings-server.util.event-listener
  (:require [re-frame.core :refer [dispatch]]
            [mount.core :refer-macros [defstate]]
            [mtg-pairings-server.events :as events]
            [mtg-pairings-server.util.local-storage :as local-storage]))

(defn resize-listener [_]
  (dispatch [::events/window-resized]))

(def ^:private listeners [{:event    "resize"
                           :listener resize-listener}
                          {:event    "storage"
                           :listener local-storage/listener}])

(defstate event-listeners
  :start (doseq [{:keys [event listener]} listeners]
           (.addEventListener js/window event listener))
  :stop (doseq [{:keys [event listener]} listeners]
          (.removeEventListener js/window event listener)))
