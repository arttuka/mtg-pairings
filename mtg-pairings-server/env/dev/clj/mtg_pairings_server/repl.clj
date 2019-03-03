(ns mtg-pairings-server.repl
  (:require [mount.core :as m]
            [figwheel.main.api :as figwheel]
            [taoensso.timbre :as timbre]
            [clojure.tools.namespace.repl :as repl]
            [clojure.java.io :as io]
            [clojure.string :as str]
            [hawk.core :as hawk]
            [mtg-pairings-server.styles.main :as main-css]
            mtg-pairings-server.server))

(m/in-cljc-mode)
(timbre/swap-config! (fn [config] (assoc config :ns-whitelist ["mtg-pairings-server.*"])))

(defn compile-garden-css! []
  (require 'mtg-pairings-server.styles.main :reload-all)
  (timbre/debug "Compiling CSS")
  (let [f (io/file "./target/public/css/main.css")]
    (io/make-parents f)
    (spit f main-css/css)))

(m/defstate garden-watcher
  :start (do
           (compile-garden-css!)
           (hawk/watch! [{:paths   ["src/clj/mtg_pairings_server/styles"]
                          :handler (fn [ctx e]
                                     (when (and (= :modify (:kind e))
                                                (str/ends-with? (.getAbsolutePath (:file e)) ".clj"))
                                       (compile-garden-css!))
                                     ctx)}]))
  :stop (hawk/stop! @garden-watcher))

(m/defstate figwheel
  :start (figwheel/start {:mode :serve} "dev")
  :stop (figwheel/stop "dev"))

(defn cljs-repl []
  (figwheel/cljs-repl "dev"))

(defn restart []
  (m/stop)
  (repl/refresh :after `m/start))
