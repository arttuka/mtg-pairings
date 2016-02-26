(defproject mtg-pairings-server "0.2.0"
  :license {:name "MIT License"
            :url "http://www.opensource.org/licenses/mit-license.php"}
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [org.clojure/core.cache "0.6.4"]
                 [ring "1.2.1"]
                 [ring/ring-core "1.4.0"]
                 [http-kit "2.1.18"]
                 [org.clojure/tools.reader "1.0.0-alpha3"]
                 [clj-time "0.11.0"]
                 [korma "0.4.2"]
                 [org.postgresql/postgresql "9.4.1208"]
                 [org.clojure/tools.logging "0.3.1"]
                 [ch.qos.logback/logback-classic "1.1.5"]
                 [metosin/compojure-api "1.0.0"]
                 [ring.middleware.jsonp "0.1.6"]]
  :profiles {:dev {:source-paths ["dev"]
                   :dependencies [[org.clojure/tools.namespace "0.2.11"]]}
             :uberjar {:main mtg-pairings-server.server
                       :aot [mtg-pairings-server.server]}})
