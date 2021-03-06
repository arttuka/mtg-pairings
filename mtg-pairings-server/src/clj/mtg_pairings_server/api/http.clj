(ns mtg-pairings-server.api.http
  (:require [compojure.api.sweet :refer :all]
            [compojure.api.exception :as ex]
            [config.core :refer [env]]
            [mtg-pairings-server.api.player :refer [player-routes]]
            [mtg-pairings-server.api.tournament :refer [tournament-routes]]
            [mtg-pairings-server.middleware.error :refer [request-validation-error-handler sql-error-handler]]
            [mtg-pairings-server.util.sql :as sql-util]))

(defapi app
  {:exceptions {:handlers {::ex/request-validation request-validation-error-handler
                           ::sql-util/assertion    sql-error-handler}}}
  (swagger-routes
   {:ui   "/api-docs"
    :spec "/swagger.json"
    :data {:info {:title "WER pairings backend API"}}})
  (context "/tournament" [] tournament-routes)
  (context "/player" [] player-routes)
  (GET "/client-version" []
    :no-doc true
    {:status 200
     :body   {:version (env :client-version)}}))
