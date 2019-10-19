(defproject mtg-pairings-server "2.2.1"
  :license {:name "MIT License"
            :url  "http://www.opensource.org/licenses/mit-license.php"}
  :dependencies [[org.clojure/clojure "1.10.1"]
                 [org.clojure/core.async "0.4.500" :exclusions [org.clojure/core.memoize]]
                 [org.clojure/core.memoize "0.7.2"]
                 [org.clojure/tools.logging "0.5.0"]
                 [ring/ring-core "1.7.1"]
                 [ring/ring-defaults "0.3.2"]
                 [hiccup "1.0.5"]
                 [yogthos/config "1.1.6"]
                 [org.clojure/core.cache "0.8.2"]
                 [aleph "0.4.6"]
                 [clj-time "0.15.2"]
                 [korma "0.4.3" :exclusions [com.mchange/c3p0]]
                 [mount "0.1.16"]
                 [org.postgresql/postgresql "42.2.8"]
                 [hikari-cp "2.9.0"]
                 [metosin/compojure-api "1.1.12"]
                 [org.flatland/ordered "1.5.7"]
                 [cheshire "5.9.0"]
                 [ring.middleware.jsonp "0.1.6"]
                 [com.taoensso/sente "1.14.0"]
                 [com.taoensso/timbre "4.10.0"]
                 [com.cognitect/transit-clj "0.8.319"]
                 [com.fzakaria/slf4j-timbre "0.3.14"]
                 [garden "1.3.9"]]
  :plugins [[lein-ancient "0.6.15"]
            [lein-cljfmt "0.6.4"]
            [lein-garden "0.3.0" :exclusions [org.apache.commons/commons-compress]]
            [lein-kibit "0.1.7"]
            [jonase/eastwood "0.3.6"]
            [no.terjedahl/lein-buster "0.2.0"]]

  :uberjar-name "mtg-pairings.jar"

  :clean-targets ^{:protect false} ["target"
                                    "resources/public/js"
                                    "resources/public/css"
                                    "resources/manifest.json"]

  :source-paths ["src/clj" "src/cljc" "src/cljs"]
  :resource-paths ["resources"]
  :test-paths []

  :garden {:builds [{:id           "main"
                     :source-paths ["src/clj" "src/cljc"]
                     :stylesheet   mtg-pairings-server.styles.main/main
                     :compiler     {:output-to     "target/public/css/main.css"
                                    :pretty-print? true}}]}

  :buster {:files       ["target/public/js/pairings-main.js"
                         "target/public/js/decklist-main.js"
                         "target/public/css/main.min.css"]
           :files-base  "target/public"
           :output-base "resources/public"
           :manifest    "resources/manifest.json"}

  :aliases {"fig"     ["trampoline" "run" "-m" "figwheel.main"]
            "fig:min" ["run" "-m" "figwheel.main" "-bo"]}

  :cljfmt {:indents {reg-sub                       [[:inner 0]]
                     reg-fx                        [[:inner 0]]
                     reg-event-fx                  [[:inner 0]]
                     reg-event-db                  [[:inner 0]]
                     sql/select                    [[:inner 0]]
                     sql/sqlfn                     [[:inner 0]]
                     sql/subselect                 [[:inner 0]]
                     sql/with                      [[:inner 0]]
                     sql/insert                    [[:inner 0]]
                     sql/delete                    [[:inner 0]]
                     sql/belongs-to                [[:inner 0]]
                     sql/has-many                  [[:inner 0]]
                     sql/has-one                   [[:inner 0]]
                     sql/many-to-many              [[:inner 0]]
                     sql-util/select-unique-or-nil [[:inner 0]]
                     sql-util/select-unique        [[:inner 0]]
                     sql-util/delete-unique        [[:inner 0]]
                     sql-util/update-unique        [[:inner 0]]
                     validate-request              [[:inner 0]]}}

  :eastwood {:namespaces   [:source-paths :test-paths]
             :config-files ["test-resources/eastwood.clj"]}

  :profiles {:dev      {:repl-options   {:init-ns          mtg-pairings-server.repl
                                         :nrepl-middleware [cider.piggieback/wrap-cljs-repl]}
                        :source-paths   ["dev" "env/dev/clj" "env/dev/cljs"]
                        :resource-paths ["dev-resources" "target"]
                        :test-paths     ["test/clj"]
                        :dependencies   [[org.clojure/tools.namespace "0.3.1"]
                                         [binaryage/devtools "0.9.10"]
                                         [com.bhauman/rebel-readline-cljs "0.1.4" :exclusions [org.clojure/clojurescript]]
                                         [ring/ring-mock "0.4.0"]
                                         [ring/ring-devel "1.7.1"]
                                         [prone "2019-07-08"]
                                         [hawk "0.2.11"]
                                         [cider/piggieback "0.4.2" :exclusions [org.clojure/clojurescript]]
                                         [re-frisk "0.5.4.1" :exclusions [org.clojure/clojurescript]]]}
             :test     {:source-paths   ^:replace ["src/clj" "src/cljc" "src/cljs"]
                        :resource-paths ["test-resources"]}
             :prod     {:source-paths ["env/prod/cljs"]}
             :provided {:dependencies [[org.clojure/clojurescript "1.10.520"]
                                       [reagent "0.9.0-rc1"]
                                       [com.google.errorprone/error_prone_annotations "2.3.3"]
                                       [com.google.code.findbugs/jsr305 "3.0.2"]
                                       [com.bhauman/figwheel-main "0.2.3" :exclusions [org.clojure/clojurescript]]
                                       [clj-commons/secretary "1.2.4"]
                                       [venantius/accountant "0.2.5"]
                                       [com.cognitect/transit-cljs "0.8.256"]
                                       [com.andrewmcveigh/cljs-time "0.5.2"]
                                       [re-frame "0.10.9" :exclusions [cljsjs/react org.clojure/clojurescript]]
                                       [binaryage/oops "0.7.0"]
                                       [cljsjs/react "16.9.0-0"]
                                       [cljsjs/react-dom "16.9.0-0"]
                                       [cljsjs/react-dom-server "16.9.0-0"]
                                       [cljsjs/react-transition-group "4.3.0-0"]
                                       [arttuka/reagent-material-ui "4.5.1-0"]
                                       [cljsjs/react-autosuggest "9.4.3-0"]]}
             :uberjar  {:source-paths       ["env/prod/cljs"]
                        :main               mtg-pairings-server.main
                        :aot                :all
                        :omit-source        true
                        :auto-clean         false}})
