(ns mtg-pairings-server.prod
  (:require [mtg-pairings-server.core :as core]))

;;ignore println statements in prod
(set! *print-fn* (fn [& _]))

(core/init!)
