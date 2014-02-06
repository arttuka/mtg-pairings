(ns user
  (:require [clojure.tools.namespace.repl :as nsr]
            [clojure.repl :refer :all]))

(defonce ^:private server (atom nil))

(defn ^:private start! []
  (require 'mtg-pairings-server.server)
  (reset! server ((ns-resolve 'mtg-pairings-server.server 'run!))))

(defn ^:private stop! []
  ((:stop-fn @server))
  (reset! server nil))

(defn restart! []
  (when @server
    (stop!))
  (nsr/refresh :after 'user/start!))
