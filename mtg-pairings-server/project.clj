(defproject mtg-pairings-server "2.2.1"
  :license {:name "MIT License"
            :url  "http://www.opensource.org/licenses/mit-license.php"}
  :dependencies [[org.clojure/clojure "1.10.1"]
                 [org.clojure/core.memoize "1.0.236"]
                 [org.clojure/tools.logging "1.1.0"]
                 [ring/ring-core "1.8.1"]
                 [ring/ring-defaults "0.3.2"]
                 [hiccup "1.0.5"]
                 [yogthos/config "1.1.7"]
                 [org.clojure/core.cache "1.0.207"]
                 [aleph "0.4.6"]
                 [clj-time "0.15.2"]
                 [honeysql "1.0.444"]
                 [seancorfield/next.jdbc "1.1.588"]
                 [mount "0.1.16"]
                 [org.postgresql/postgresql "42.2.16"]
                 [com.zaxxer/HikariCP "3.4.5" :exclusions [org.slf4j/slf4j-api]]
                 [metosin/compojure-api "1.1.13" :exclusions [org.mozilla/rhino]]
                 [cheshire "5.10.0"]
                 [ring.middleware.jsonp "0.1.6"]
                 [com.taoensso/sente "1.16.0"]
                 [com.taoensso/timbre "5.0.0"]
                 [com.cognitect/transit-clj "1.0.324"]
                 [com.fzakaria/slf4j-timbre "0.3.19"]
                 [ragtime "0.8.0"]
                 [thheller/shadow-cljs "2.11.4"]]
  :plugins [[lein-ancient "0.6.15"]
            [lein-cljfmt "0.7.0"]
            [lein-kibit "0.1.8"]
            [jonase/eastwood "0.3.11"]]

  :uberjar-name "mtg-pairings.jar"

  :clean-targets ^{:protect false} ["target"
                                    "resources/public/js"
                                    "resources/manifest.edn"
                                    ".shadow-cljs"]

  :source-paths ["src/clj" "src/cljc" "src/cljs"]
  :resource-paths ["resources"]
  :test-paths []

  :aliases {"migrate"  ["run" "-m" "mtg-pairings-server.migrations/migrate"]
            "rollback" ["run" "-m" "mtg-pairings-server.migrations/rollback"]}

  :cljfmt {:indents {reg-sub          [[:inner 0]]
                     reg-fx           [[:inner 0]]
                     reg-event-fx     [[:inner 0]]
                     reg-event-db     [[:inner 0]]
                     validate-request [[:inner 0]]
                     cond->           [[:inner 0]]}}

  :eastwood {:namespaces   [:source-paths :test-paths]
             :config-files ["test-resources/eastwood.clj"]}

  :profiles {:dev      {:repl-options   {:init-ns          mtg-pairings-server.repl
                                         :nrepl-middleware [shadow.cljs.devtools.server.nrepl/middleware]}
                        :source-paths   ["dev" "env/dev/clj"]
                        :resource-paths ["dev-resources" "target"]
                        :test-paths     ["test/clj"]
                        :dependencies   [[org.clojure/tools.namespace "1.0.0"]
                                         [ring/ring-mock "0.4.0"]
                                         [ring/ring-devel "1.8.1"]
                                         [prone "2020-01-17"]]}
             :test     {:source-paths   ^:replace ["src/clj" "src/cljc" "src/cljs"]
                        :resource-paths ^:replace ["resources" "test-resources" "target"]}
             :provided {:dependencies [[reagent "0.10.0"]
                                       [clj-commons/secretary "1.2.4"]
                                       [venantius/accountant "0.2.5"]
                                       [com.cognitect/transit-cljs "0.8.264"]
                                       [com.cognitect/transit-js "0.8.867"]
                                       [com.andrewmcveigh/cljs-time "0.5.2"]
                                       [re-frame "1.1.1"]
                                       [arttuka/reagent-material-ui "4.11.0-3"]
                                       [re-frisk "1.3.4" :exclusions [org.clojure/clojurescript]]
                                       [binaryage/devtools "1.0.2"]]}
             :uberjar  {:main        mtg-pairings-server.main
                        :aot         :all
                        :omit-source true
                        :auto-clean  false}})
