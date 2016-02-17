(defproject mtg-pairings "0.2.0"
  :license {:name "MIT License"
            :url "http://www.opensource.org/licenses/mit-license.php"}
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [clojess "0.3"]
                 [seesaw "1.4.4"]
                 [watchtower "0.1.1"]
                 [http-kit "2.1.18"]
                 [org.clojure/tools.logging "0.3.1"]
                 [org.clojure/tools.reader "1.0.0-alpha3"]
                 [cheshire "5.5.0"]
                 [clj-time "0.11.0"]]
  :profiles {:uberjar {:main mtg-pairings.core
                       :aot [mtg-pairings.core]}})
