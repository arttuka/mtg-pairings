(defproject mtg-pairings-server "2.2.1"
  :license {:name "MIT License"
            :url  "http://www.opensource.org/licenses/mit-license.php"}
  :dependencies [[org.clojure/clojure "1.9.0"]
                 [org.clojure/core.async "0.4.490"]
                 [ring "1.7.1"]
                 [ring/ring-defaults "0.3.2"]
                 [compojure "1.6.1"]
                 [hiccup "1.0.5"]
                 [yogthos/config "1.1.1"]
                 [org.clojure/core.cache "0.7.1"]
                 [http-kit "2.3.0"]
                 [org.clojure/tools.reader "1.3.2"]
                 [clj-time "0.15.1"]
                 [korma "0.4.3"]
                 [mount "0.1.15"]
                 [org.postgresql/postgresql "42.2.5"]
                 [metosin/compojure-api "1.1.12"]
                 [ring.middleware.jsonp "0.1.6"]
                 [com.taoensso/sente "1.13.1" :exclusions [com.taoensso/encore]]
                 [com.taoensso/timbre "4.10.0"]
                 [com.cognitect/transit-clj "0.8.313"]
                 [com.fzakaria/slf4j-timbre "0.3.12"]]
  :plugins [[lein-environ "1.1.0" :exclusions [org.clojure/clojure]]
            [lein-cljsbuild "1.1.7"]
            [lein-asset-minifier "0.4.5"]
            [lein-ancient "0.6.15"]]

  :uberjar-name "mtg-pairings.jar"

  :clean-targets ^{:protect false} [:target-path
                                    [:cljsbuild :builds :app :compiler :output-dir]
                                    [:cljsbuild :builds :app :compiler :output-to]]

  :source-paths ["src/clj" "src/cljc"]
  :resource-paths ["resources" "target/cljsbuild"]

  :minify-assets [[:css {:source ["resources/public/css/main.css"]
                         :target "resources/public/css/main.min.css"}]]

  :cljsbuild
  {:builds {:min
            {:source-paths ["src/cljs" "src/cljc" "env/prod/cljs"]
             :compiler
                           {:output-to     "target/cljsbuild/public/js/app.js"
                            :output-dir    "target/cljsbuild/public/js"
                            :source-map    "target/cljsbuild/public/js/app.js.map"
                            :optimizations :advanced
                            :pretty-print  false}}
            :app
            {:source-paths ["src/cljs" "src/cljc" "env/dev/cljs"]
             :figwheel     {:on-jsload "mtg-pairings-server.core/figwheel-reload"}
             :compiler
                           {:main            "mtg-pairings-server.dev"
                            :asset-path      "/js/out"
                            :output-to       "target/cljsbuild/public/js/app.js"
                            :output-dir      "target/cljsbuild/public/js/out"
                            :source-map      true
                            :optimizations   :none
                            :pretty-print    true
                            :closure-defines {"re_frame.trace.trace_enabled_QMARK_" true}
                            :preloads        [devtools.preload
                                              re-frisk.preload]}}}}

  :figwheel {:http-server-root "public"
             :server-port      3449
             :nrepl-port       7002
             :nrepl-middleware ["cemerick.piggieback/wrap-cljs-repl"]
             :css-dirs         ["resources/public/css"]
             :ring-handler     mtg-pairings-server.handler/app}

  :profiles {:dev      {:repl-options {:init-ns          mtg-pairings-server.repl
                                       :nrepl-middleware [cemerick.piggieback/wrap-cljs-repl]}
                        :source-paths ["dev" "env/dev/clj"]
                        :plugins      [[lein-figwheel "0.5.16"]]

                        :injections   [(require 'pjstadig.humane-test-output)
                                       (pjstadig.humane-test-output/activate!)]

                        :env          {:dev true}
                        :dependencies [[org.clojure/tools.namespace "0.2.11"]
                                       [binaryage/devtools "0.9.10"]
                                       [ring/ring-mock "0.3.2"]
                                       [ring/ring-devel "1.7.1"]
                                       [prone "1.6.1"]
                                       [figwheel-sidecar "0.5.17"]
                                       [org.clojure/tools.nrepl "0.2.13"]
                                       [com.cemerick/piggieback "0.2.2"]
                                       [pjstadig/humane-test-output "0.9.0"]
                                       [re-frisk "0.5.4" :exclusions [args4j]]]}
             :provided {:dependencies [[reagent "0.8.1" :exclusions [cljsjs/react]]
                                       [reagent-utils "0.3.1"]
                                       [org.clojure/clojurescript "1.10.439"]
                                       [clj-commons/secretary "1.2.4"]
                                       [venantius/accountant "0.2.4"
                                        :exclusions [org.clojure/tools.reader]]
                                       [com.cognitect/transit-cljs "0.8.256"]
                                       [com.andrewmcveigh/cljs-time "0.5.2"]
                                       [re-frame "0.10.6" :exclusions [cljsjs/react]]
                                       [binaryage/oops "0.6.3"]
                                       [cljsjs/react "16.6.0-0"]
                                       [cljsjs/react-dom "16.6.0-0"]
                                       [cljs-react-material-ui "0.2.50"]
                                       [cljsjs/prop-types "15.6.2-0"]
                                       [cljsjs/rc-slider "8.6.1-0"]]}
             :uberjar  {:main         mtg-pairings-server.main
                        :aot          :all
                        :hooks        [minify-assets.plugin/hooks]
                        :prep-tasks   ["compile" ["cljsbuild" "once" "min"]]
                        :env          {:production true}
                        :omit-source  true}})
