(defproject mtg-pairings-server "2.0.0"
  :license {:name "MIT License"
            :url  "http://www.opensource.org/licenses/mit-license.php"}
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [org.clojure/core.async "0.3.443"]
                 [reagent "0.7.0" :exclusions [cljsjs/react]]
                 [reagent-utils "0.2.1"]
                 [ring "1.6.2"]
                 [ring/ring-defaults "0.3.1"]
                 [compojure "1.6.0"]
                 [hiccup "1.0.5"]
                 [yogthos/config "0.9"]
                 [org.clojure/clojurescript "1.9.908"
                  :scope "provided"]
                 [secretary "1.2.3"]
                 [venantius/accountant "0.2.0"
                  :exclusions [org.clojure/tools.reader]]
                 [org.clojure/core.cache "0.6.5"]
                 [http-kit "2.2.0"]
                 [org.clojure/tools.reader "1.0.5"]
                 [clj-time "0.14.0"]
                 [com.andrewmcveigh/cljs-time "0.5.0"]
                 [korma "0.4.3"]
                 [mount "0.1.11"]
                 [org.postgresql/postgresql "42.1.4"]
                 [metosin/compojure-api "1.1.11"]
                 [ring.middleware.jsonp "0.1.6"]
                 [com.taoensso/sente "1.11.0" :exclusions [com.taoensso/encore]]
                 [com.taoensso/timbre "4.10.0"]
                 [com.cognitect/transit-clj "0.8.300"]
                 [com.cognitect/transit-cljs "0.8.239"]
                 [re-frame "0.10.1" :exclusions [cljsjs/react]]
                 [cljsjs/react-with-addons "15.6.1-0"]]
  :plugins [[lein-environ "1.1.0" :exclusions [org.clojure/clojure]]
            [lein-cljsbuild "1.1.7"]
            [lein-asset-minifier "0.3.2"]]

  :uberjar-name "mtg-pairings.jar"

  :clean-targets ^{:protect false} [:target-path
                                    [:cljsbuild :builds :app :compiler :output-dir]
                                    [:cljsbuild :builds :app :compiler :output-to]]

  :source-paths ["src/clj" "src/cljc"]
  :resource-paths ["resources" "target/cljsbuild"]

  :minify-assets {:assets {"resources/public/css/main.min.css"
                           "resources/public/css/main.css"}}

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
             :figwheel     {:on-jsload "mtg-pairings-server.core/mount-root"}
             :compiler
                           {:main          "mtg-pairings-server.dev"
                            :asset-path    "/js/out"
                            :output-to     "target/cljsbuild/public/js/app.js"
                            :output-dir    "target/cljsbuild/public/js/out"
                            :source-map    true
                            :optimizations :none
                            :pretty-print  true}}}}

  :figwheel {:http-server-root "public"
             :server-port      3449
             :nrepl-port       7002
             :nrepl-middleware ["cemerick.piggieback/wrap-cljs-repl"
                                ]
             :css-dirs         ["resources/public/css"]
             :ring-handler     mtg-pairings-server.handler/app}

  :profiles {:dev     {:repl-options {:init-ns          mtg-pairings-server.repl
                                      :nrepl-middleware [cemerick.piggieback/wrap-cljs-repl]}
                       :source-paths ["dev" "env/dev/clj"]
                       :plugins [[lein-figwheel "0.5.13"]]

                       :injections [(require 'pjstadig.humane-test-output)
                                    (pjstadig.humane-test-output/activate!)]

                       :env {:dev true}
                       :dependencies [[org.clojure/tools.namespace "0.2.11"]
                                      [binaryage/devtools "0.9.4"]
                                      [ring/ring-mock "0.3.1"]
                                      [ring/ring-devel "1.6.2"]
                                      [prone "1.1.4"]
                                      [figwheel-sidecar "0.5.13"]
                                      [org.clojure/tools.nrepl "0.2.13"]
                                      [com.cemerick/piggieback "0.2.2"]
                                      [pjstadig/humane-test-output "0.8.2"]]}
             :uberjar {:main         mtg-pairings-server.main
                       :aot          :all
                       :hooks        [minify-assets.plugin/hooks]
                       :source-paths ["env/prod/clj"]
                       :prep-tasks   ["compile" ["cljsbuild" "once" "min"]]
                       :env          {:production true}
                       :omit-source  true}})
