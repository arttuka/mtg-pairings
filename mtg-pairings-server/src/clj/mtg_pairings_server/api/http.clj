(ns mtg-pairings-server.api.http
  (:require [compojure.api.sweet :refer :all]
            [compojure.api.exception :as ex]
            [config.core :refer [env]]
            [mtg-pairings-server.api.decklist :refer [decklist-routes]]
            [mtg-pairings-server.api.player :refer [player-routes]]
            [mtg-pairings-server.api.tournament :refer [tournament-routes]]
            [mtg-pairings-server.db :as db]
            [mtg-pairings-server.middleware.error :refer [request-validation-error-handler sql-error-handler default-error-handler]]))

(defapi app
  {:exceptions {:handlers {::ex/request-validation request-validation-error-handler
                           ::db/assertion          sql-error-handler
                           ::ex/default            default-error-handler}}}
  (swagger-routes
   {:ui   "/api-docs"
    :spec "/swagger.json"
    :data {:info {:title "WER pairings backend API"}}})
  (context "/tournament" [] tournament-routes)
  (context "/player" [] player-routes)
  (context "/decklist" [] decklist-routes)
  (GET "/client-version" []
    :no-doc true
    {:status 200
     :body   {:version (env :client-version)}}))
