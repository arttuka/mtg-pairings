(ns mtg-pairings-server.prod
  (:require [mtg-pairings-server.core]
            [mount.core :as m]))

;;ignore println statements in prod
(set! *print-fn* (fn [& _]))

(m/start)
