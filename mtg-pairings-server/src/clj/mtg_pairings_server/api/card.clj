(ns mtg-pairings-server.api.card
  (:require [compojure.api.sweet :refer :all]
            [schema.core :as s]
            [mtg-pairings-server.service.card :refer :all]
            [mtg-pairings-server.util.schema :refer :all]
            [mtg-pairings-server.util :refer [response]]))

(defroutes card-routes
  (GET "/search" []
    :query-params [prefix :- s/Str
                   format :- (s/enum :standard :modern :legacy)]
    :return [s/Str]
    :summary "Hae kortteja nimen alkuosalla"
    (response (search prefix format))))
