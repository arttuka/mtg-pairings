(defproject mtg-pairings-server "2.2.1"
  :license {:name "MIT License"
            :url  "http://www.opensource.org/licenses/mit-license.php"}
  :dependencies [[org.clojure/clojure "1.10.3"]
                 [org.clojure/core.memoize "1.0.236"]
                 [org.clojure/tools.logging "1.1.0"]
                 [ring/ring-core "1.9.1"]
                 [ring/ring-defaults "0.3.2"]
                 [hiccup "1.0.5"]
                 [yogthos/config "1.1.7"]
                 [org.clojure/core.cache "1.0.207"]
                 [aleph "0.4.6"]
                 [clj-time "0.15.2"]
                 [honeysql "1.0.461"]
                 [seancorfield/next.jdbc "1.1.613"]
                 [mount "0.1.16"]
                 [org.postgresql/postgresql "42.2.19"]
                 [hikari-cp "2.13.0"]
                 [metosin/compojure-api "1.1.13" :exclusions [org.mozilla/rhino]]
                 [cheshire "5.10.0"]
                 [ring.middleware.jsonp "0.1.6"]
                 [com.taoensso/sente "1.16.2"]
                 [com.taoensso/timbre "5.1.2"]
                 [com.cognitect/transit-clj "1.0.324"]
                 [com.fzakaria/slf4j-timbre "0.3.20"]
                 [ragtime "0.8.1"]]
  :plugins [[lein-ancient "0.7.0"]
            [lein-cljfmt "0.7.0"]
            [lein-kibit "0.1.8"]
            [jonase/eastwood "0.3.14"]]

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
                     cond->           [[:inner 0]]
                     react-component  [[:inner 0]]}}

  :eastwood {:namespaces   [:source-paths :test-paths]
             :config-files ["test-resources/eastwood.clj"]}

  :profiles {:dev      {:repl-options   {:init-ns          mtg-pairings-server.repl
                                         :nrepl-middleware [shadow.cljs.devtools.server.nrepl/middleware]}
                        :source-paths   ["dev" "env/dev/clj"]
                        :resource-paths ["dev-resources" "target"]
                        :test-paths     ["test/clj"]
                        :dependencies   [[org.clojure/tools.namespace "1.1.0"]
                                         [ring/ring-mock "0.4.0"]
                                         [ring/ring-devel "1.9.1"]
                                         [prone "2020-01-17"]
                                         [thheller/shadow-cljs "2.11.20"]]}
             :test     {:source-paths   ^:replace ["src/clj" "src/cljc" "src/cljs"]
                        :resource-paths ^:replace ["resources" "test-resources" "target"]}
             :provided {:dependencies [[reagent "1.0.0"]
                                       [clj-commons/secretary "1.2.4"]
                                       [venantius/accountant "0.2.5" :exclusions [org.clojure/clojurescript]]
                                       [com.cognitect/transit-cljs "0.8.264"]
                                       [com.cognitect/transit-js "0.8.867"]
                                       [com.andrewmcveigh/cljs-time "0.5.2"]
                                       [re-frame "1.2.0"]
                                       [arttuka/reagent-material-ui "5.0.0-alpha.27-0"]
                                       [re-frisk "1.3.10" :exclusions [org.clojure/clojurescript]]
                                       [binaryage/devtools "1.0.2"]]}
             :uberjar  {:main        mtg-pairings-server.main
                        :aot         :all
                        :omit-source true
                        :auto-clean  false}})
