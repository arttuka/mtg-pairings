(ns mtg-pairings-server.handler
  (:require [compojure.api.sweet :refer :all]
            [compojure.route :refer [not-found resources]]
            [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.string :as str]
            [config.core :refer [env]]
            [hiccup.page :refer [include-js include-css html5]]
            [schema.core :as s]
            [ring.middleware.anti-forgery :refer [*anti-forgery-token*]]
            [ring.middleware.jsonp :refer [wrap-json-with-padding]]
            [ring.util.response :refer [redirect]]
            [mtg-pairings-server.api.http :as http-api]
            [mtg-pairings-server.auth :as auth :refer [auth-routes]]
            [mtg-pairings-server.middleware :refer [wrap-site-middleware]]
            [mtg-pairings-server.middleware.cors :refer [wrap-allow-origin]]
            [mtg-pairings-server.middleware.db :refer [wrap-db-transaction]]
            [mtg-pairings-server.service.decklist :as decklist]
            [mtg-pairings-server.service.player :as player]
            [mtg-pairings-server.service.tournament :as tournament]
            [mtg-pairings-server.transit :as transit]
            [mtg-pairings-server.util :refer [map-by]]
            [mtg-pairings-server.util.decklist :refer [add-id-to-card add-id-to-cards]]
            [mtg-pairings-server.websocket :as ws]))

(defn escape-quotes [s]
  (str/escape s {\' "\\'"}))

(def asset-manifest (delay (let [manifest (edn/read-string (slurp (io/resource "manifest.edn")))]
                             (into {} (map (juxt :name :output-name)) manifest))))
(defn module-js [module]
  (get @asset-manifest module))

(defn index
  ([module]
   (index module {}))
  ([module initial-db]
   (let [html (html5
               {:lang :fi}
               [:head
                [:title "Pairings.fi"]
                [:meta {:charset "utf-8"}]
                [:meta {:name    "viewport"
                        :content "width=device-width, initial-scale=1"}]
                (include-css "https://fonts.googleapis.com/css?family=Roboto:300,400,500,700&display=swap")
                (include-css "https://fonts.googleapis.com/css?family=Lato:700&display=swap")
                [:style "@media print { @page { size: A4; } }"]]
               [:body {:class "body-container"}
                [:div#app]
                [:script (str "var csrfToken = '" *anti-forgery-token* "'; "
                              "var initialDb = '" (escape-quotes (transit/write initial-db)) "'; ")]
                (when-not (env :dev)
                  (include-js (str "/js/" (module-js :common))))
                (include-js (if (env :dev)
                              "/js/dev-main.js"
                              (str "/js/" (module-js module))))])]
     {:status  200
      :body    html
      :headers {"Content-Type"  "text/html"
                "Cache-Control" "no-cache"}})))

(defn pairings-index
  ([]
   (pairings-index nil {}))
  ([req]
   (pairings-index req {}))
  ([req initial-db]
   (let [user (some-> (get-in req [:session :dci])
                      (player/player))
         player-tournaments (when user
                              (player/tournaments (:dci user)))]
     (index :pairings (merge initial-db
                             {:logged-in-user     user
                              :player-tournaments (vec player-tournaments)})))))

(defroutes pairings-routes
  (GET "/" req
    (let [tournaments (:tournaments (tournament/client-tournaments {:active? true}))]
      (pairings-index req {:active-tournament-ids (map :id tournaments)
                           :tournaments           (map-by :id tournaments)})))
  (GET "/tournaments" req
    (pairings-index req {:page {:page :mtg-pairings-server.pages.pairings/tournaments}}))
  (GET "/tournaments/organizer" req
    (pairings-index req))
  (GET "/tournaments/organizer/menu" req
    (pairings-index req))
  (GET "/tournaments/:id" req
    :path-params [id :- s/Int]
    (pairings-index req
                    {:page        {:page :mtg-pairings-server.pages.pairings/tournament
                                   :id   id}
                     :tournaments {id (tournament/client-tournament id)}}))
  (GET "/tournaments/:id/pairings-:round" req
    :path-params [id :- s/Int
                  round :- s/Int]
    (pairings-index req
                    {:page        {:page  :mtg-pairings-server.pages.pairings/pairings
                                   :id    id
                                   :round round}
                     :tournaments {id (tournament/client-tournament id)}
                     :pairings    {id {round (tournament/get-round id round)}}}))
  (GET "/tournaments/:id/standings-:round" req
    :path-params [id :- s/Int
                  round :- s/Int]
    (pairings-index req
                    {:page        {:page  :mtg-pairings-server.pages.pairings/standings
                                   :id    id
                                   :round round}
                     :tournaments {id (tournament/client-tournament id)}
                     :standings   {id {round (tournament/standings id round false)}}}))
  (GET "/tournaments/:id/pods-:round" req
    :path-params [id :- s/Int
                  round :- s/Int]
    (pairings-index req
                    {:page        {:page  :mtg-pairings-server.pages.pairings/pods
                                   :id    id
                                   :round round}
                     :tournaments {id (tournament/client-tournament id)}
                     :pods        {id {round (tournament/pods id round)}}}))
  (GET "/tournaments/:id/seatings" req
    :path-params [id :- s/Int]
    (pairings-index req
                    {:page        {:page :mtg-pairings-server.pages.pairings/seatings
                                   :id   id}
                     :tournaments {id (tournament/client-tournament id)}
                     :seatings    {id (tournament/seatings id)}}))
  (GET "/tournaments/:id/bracket" req
    :path-params [id :- s/Int]
    (pairings-index req
                    {:page        {:page :mtg-pairings-server.pages.pairings/bracket
                                   :id   id}
                     :tournaments {id (tournament/client-tournament id)}
                     :bracket     {id (tournament/bracket id)}}))
  (GET "/tournaments/:id/organizer" [] (pairings-index))
  (GET "/tournaments/:id/organizer/menu" [] (pairings-index))
  (GET "/tournaments/:id/organizer/deck-construction" [] (pairings-index)))

(defmacro validate-request [user-id tournament & body]
  `(if (and ~user-id (not= ~user-id (:user ~tournament)))
     {:status 403
      :body   "403 Forbidden"}
     (do ~@body)))

(let [decklist-index (partial index :decklist)]
  (defroutes decklist-routes
    (GET "/decklist" []
      (redirect (auth/organizer-path)))
    (GET "/decklist/tournament/:id" []
      :path-params [id :- s/Str]
      (decklist-index {:page            {:page :mtg-pairings-server.pages.decklist/submit}
                       :decklist-editor {:tournament (decklist/get-tournament id)}}))
    (GET "/decklist/organizer" request
      (decklist-index {:page            {:page :mtg-pairings-server.pages.decklist/organizer}
                       :decklist-editor {:organizer-tournaments (decklist/get-organizer-tournaments (get-in request [:session :identity :id]))}}))
    (GET "/decklist/organizer/new" []
      (decklist-index {:page {:page :mtg-pairings-server.pages.decklist/organizer-tournament}}))
    (GET "/decklist/organizer/view/:id" request
      :path-params [id :- s/Str]
      (let [decklist (decklist/get-decklist id)
            tournament (decklist/get-organizer-tournament (:tournament decklist))
            user-id (get-in request [:session :identity :id])]
        (validate-request user-id tournament
          (decklist-index {:page            {:page :mtg-pairings-server.pages.decklist/organizer-view
                                             :id   id}
                           :decklist-editor (when user-id
                                              {:decklist             decklist
                                               :organizer-tournament tournament})}))))
    (GET "/decklist/organizer/print" []
      (redirect (auth/organizer-path)))
    (GET "/decklist/organizer/:id" request
      :path-params [id :- s/Str]
      (let [tournament (decklist/get-organizer-tournament id)
            user-id (get-in request [:session :identity :id])]
        (validate-request user-id tournament
          (decklist-index {:page            {:page :mtg-pairings-server.pages.decklist/organizer-tournament
                                             :id   id}
                           :decklist-editor (when user-id
                                              {:organizer-tournament tournament})}))))
    (GET "/decklist/:id" []
      :path-params [id :- s/Str]
      (let [decklist (add-id-to-cards "server-card__" (decklist/get-decklist id))]
        (decklist-index {:page            {:page :mtg-pairings-server.pages.decklist/submit
                                           :id   id}
                         :decklist-editor {:tournament (decklist/get-tournament (:tournament decklist))
                                           :decklist   decklist}})))))

(def robots-txt
  {:status  200
   :body    "User-agent: *\nDisallow: /\n"
   :headers {"Content-Type" "text/plain"}})

(defroutes site-routes
  decklist-routes
  pairings-routes
  auth-routes
  ws/routes
  (GET "/robots.txt" [] robots-txt))

(defroutes app-routes
  (context "/api" []
    http-api/app)
  (wrap-site-middleware
   (routes
    site-routes
    (resources "/")
    (not-found "Not Found"))))

(def app (-> app-routes
             wrap-json-with-padding
             wrap-allow-origin
             wrap-db-transaction))
