(defproject mtg-pairings-server "2.2.1"
  :license {:name "MIT License"
            :url  "http://www.opensource.org/licenses/mit-license.php"}
  :dependencies [[org.clojure/clojure "1.10.0"]
                 [org.clojure/core.async "0.4.490"]
                 [org.clojure/tools.logging "0.4.1"]
                 [ring/ring-core "1.7.1"]
                 [ring/ring-defaults "0.3.2"]
                 [hiccup "1.0.5"]
                 [yogthos/config "1.1.1"]
                 [org.clojure/core.cache "0.7.2"]
                 [http-kit "2.3.0"]
                 [org.clojure/tools.reader "1.3.2"]
                 [clj-time "0.15.1"]
                 [korma "0.4.3"]
                 [mount "0.1.15"]
                 [org.postgresql/postgresql "42.2.5"]
                 [metosin/compojure-api "1.1.12"]
                 [cheshire "5.8.1"]
                 [ring.middleware.jsonp "0.1.6"]
                 [com.taoensso/sente "1.13.1" :exclusions [com.taoensso/encore]]
                 [com.taoensso/timbre "4.10.0"]
                 [com.cognitect/transit-clj "0.8.313" :exclusions [commons-codec]]
                 [com.fzakaria/slf4j-timbre "0.3.12"]]
  :plugins [[lein-asset-minifier "0.4.5"]
            [lein-ancient "0.6.15"]]

  :uberjar-name "mtg-pairings.jar"

  :clean-targets ^{:protect false} ["resources/public/js"
                                    "target"]

  :source-paths ["src/clj" "src/cljc" "src/cljs"]
  :resource-paths ["resources"]

  :minify-assets [[:css {:source ["resources/public/css/main.css"]
                         :target "resources/public/css/main.min.css"}]]

  :aliases {"fig"     ["trampoline" "run" "-m" "figwheel.main"]
            "fig:min" ["run" "-m" "figwheel.main" "-bo" "prod"]}

  :profiles {:dev      {:repl-options   {:init-ns          mtg-pairings-server.repl
                                         :nrepl-middleware [cider.piggieback/wrap-cljs-repl]}
                        :source-paths   ["dev" "env/dev/clj" "env/dev/cljs"]
                        :resource-paths ["target"]
                        :dependencies   [[org.clojure/tools.namespace "0.2.11"]
                                         [binaryage/devtools "0.9.10"]
                                         [com.bhauman/rebel-readline-cljs "0.1.4" :exclusions [org.clojure/clojurescript]]
                                         [ring/ring-mock "0.3.2" :exclusions [cheshire ring/ring-codec]]
                                         [ring/ring-devel "1.7.1"]
                                         [prone "1.6.1"]
                                         [cider/piggieback "0.3.10" :exclusions [org.clojure/clojurescript]]
                                         [re-frisk "0.5.4" :exclusions [args4j]]]}
             :prod     {:source-paths ["env/prod/cljs"]}
             :provided {:dependencies [[reagent "0.8.1" :exclusions [cljsjs/react]]
                                       [org.clojure/clojurescript "1.10.439"]
                                       [com.google.errorprone/error_prone_annotations "2.3.2"]
                                       [com.google.code.findbugs/jsr305 "3.0.2"]
                                       [com.bhauman/figwheel-main "0.2.0" :exclusions [org.clojure/clojurescript]]
                                       [clj-commons/secretary "1.2.4"]
                                       [venantius/accountant "0.2.4" :exclusions [org.clojure/tools.reader]]
                                       [com.cognitect/transit-cljs "0.8.256"]
                                       [com.andrewmcveigh/cljs-time "0.5.2"]
                                       [re-frame "0.10.6" :exclusions [cljsjs/react args4j]]
                                       [binaryage/oops "0.6.4"]
                                       [cljsjs/react "16.6.0-0"]
                                       [cljsjs/react-dom "16.6.0-0"]
                                       [cljs-react-material-ui "0.2.50" :exclusions [args4j]]
                                       [cljsjs/prop-types "15.6.2-0"]
                                       [cljsjs/rc-slider "8.6.1-0"]]}
             :uberjar  {:source-paths       ["env/prod/cljs"]
                        :main               mtg-pairings-server.main
                        :aot                :all
                        :prep-tasks         ["compile"
                                             "minify-assets"
                                             "fig:min"]
                        :uberjar-exclusions [#"public/js/compiled"
                                             #"public/css/main.css"]
                        :omit-source        true}})
