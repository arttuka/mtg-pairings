(defproject mtg-pairings-server "2.2.1"
  :license {:name "MIT License"
            :url  "http://www.opensource.org/licenses/mit-license.php"}
  :dependencies [[org.clojure/clojure "1.10.0"]
                 [org.clojure/core.async "0.4.490"]
                 [org.clojure/tools.logging "0.4.1"]
                 [ring/ring-core "1.7.1"]
                 [ring/ring-defaults "0.3.2"]
                 [buddy/buddy-auth "2.1.0"]
                 [hiccup "1.0.5"]
                 [yogthos/config "1.1.1"]
                 [org.clojure/core.cache "0.7.2"]
                 [aleph "0.4.6"]
                 [clj-time "0.15.1"]
                 [korma "0.4.3" :exclusions [com.mchange/c3p0]]
                 [mount "0.1.16"]
                 [org.postgresql/postgresql "42.2.5"]
                 [hikari-cp "2.7.1"]
                 [metosin/compojure-api "1.1.12"]
                 [org.flatland/ordered "1.5.7"]
                 [cheshire "5.8.1"]
                 [ring.middleware.jsonp "0.1.6"]
                 [com.taoensso/sente "1.14.0-RC2"]
                 [com.taoensso/timbre "4.10.0"]
                 [com.cognitect/transit-clj "0.8.313"]
                 [com.fzakaria/slf4j-timbre "0.3.12"]]
  :plugins [[lein-asset-minifier "0.4.6"]
            [lein-cljfmt "0.6.4"]
            [lein-ancient "0.6.15"]
            [lein-garden "0.3.0" :exclusions [org.apache.commons/commons-compress]]]

  :uberjar-name "mtg-pairings.jar"

  :clean-targets ^{:protect false} ["target"]

  :source-paths ["src/clj" "src/cljc" "src/cljs"]
  :resource-paths ["resources"]
  :test-paths []

  :garden {:builds [{:id           "main"
                     :source-paths ["src/clj" "src/cljc"]
                     :stylesheet   mtg-pairings-server.styles.main/main
                     :compiler     {:output-to     "target/public/css/main.css"
                                    :pretty-print? true}}]}

  :minify-assets [[:css {:source ["target/public/css/main.css"
                                  "resources/public/css/slider.css"]
                         :target "target/public/css/main.min.css"}]]

  :aliases {"fig"     ["trampoline" "run" "-m" "figwheel.main"]
            "fig:min" ["run" "-m" "figwheel.main" "-bo" "prod"]}

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

  :profiles {:dev      {:repl-options   {:init-ns          mtg-pairings-server.repl
                                         :nrepl-middleware [cider.piggieback/wrap-cljs-repl]}
                        :source-paths   ["dev" "env/dev/clj" "env/dev/cljs"]
                        :resource-paths ["target"]
                        :test-paths     ["test/clj"]
                        :dependencies   [[org.clojure/tools.namespace "0.2.11"]
                                         [binaryage/devtools "0.9.10"]
                                         [com.bhauman/rebel-readline-cljs "0.1.4" :exclusions [org.clojure/clojurescript]]
                                         [ring/ring-mock "0.3.2"]
                                         [ring/ring-devel "1.7.1"]
                                         [prone "1.6.1"]
                                         [hawk "0.2.11"]
                                         [cider/piggieback "0.4.0" :exclusions [org.clojure/clojurescript]]
                                         [re-frisk "0.5.4.1" :exclusions [org.clojure/clojurescript]]]}
             :test     {:source-paths   ^:replace ["src/clj" "src/cljc" "src/cljs"]
                        :resource-paths ["test-resources"]}
             :prod     {:source-paths ["env/prod/cljs"]}
             :provided {:dependencies [[org.clojure/clojurescript "1.10.520"]
                                       [reagent "0.8.1"]
                                       [com.google.errorprone/error_prone_annotations "2.3.3"]
                                       [com.google.code.findbugs/jsr305 "3.0.2"]
                                       [com.bhauman/figwheel-main "0.2.0" :exclusions [org.clojure/clojurescript]]
                                       [clj-commons/secretary "1.2.4"]
                                       [venantius/accountant "0.2.4"]
                                       [com.cognitect/transit-cljs "0.8.256"]
                                       [com.andrewmcveigh/cljs-time "0.5.2"]
                                       [re-frame "0.10.6" :exclusions [cljsjs/react org.clojure/clojurescript]]
                                       [binaryage/oops "0.7.0"]
                                       [cljsjs/react "16.8.3-0"]
                                       [cljsjs/react-dom "16.8.3-0"]
                                       [cljsjs/react-dom-server "16.8.3-0"]
                                       [cljs-react-material-ui "0.2.50" :exclusions [org.clojure/clojurescript]]
                                       [cljsjs/prop-types "15.6.2-0"]
                                       [cljsjs/rc-slider "8.6.1-0"]
                                       [cljsjs/react-autosuggest "9.4.3-0"]
                                       [garden "1.3.6"]]}
             :uberjar  {:source-paths       ["env/prod/cljs"]
                        :main               mtg-pairings-server.main
                        :aot                :all
                        :uberjar-exclusions [#"public/css/slider.css"]
                        :omit-source        true}})
