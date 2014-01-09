(defproject mtg-pairings-backend "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "MIT License"
            :url "http://www.opensource.org/licenses/mit-license.php"}
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [ring/ring-core "1.2.1"]
                 [ring/ring-json "0.2.0"]
                 [http-kit "2.1.16"]
                 [compojure "1.1.6"]
                 [org.clojure/tools.reader "0.8.3"]]
  :main mtg-pairings-backend.server
  :profiles {:dev {:source-paths ["dev"]
                   :dependencies [[org.clojure/tools.namespace "0.2.4"]]}})
