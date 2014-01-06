(ns user
  (:require [clojure.tools.namespace.repl :as nsr]
            [clojure.repl :refer :all]))

(defonce ^:private server (atom nil))

(defn ^:private start! []
  (require 'mtg-pairings-backend.server)
  (reset! server ((ns-resolve 'mtg-pairings-backend.server 'run!))))

(defn ^:private stop! []
  (@server)
  (reset! server nil))

(defn ^:private restart! []
  (when @server
    (stop!))
  (nsr/refresh :after 'user/start!))
