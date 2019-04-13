(ns mtg-pairings-server.handler
  (:require [compojure.api.sweet :refer :all]
            [compojure.route :refer [not-found resources]]
            [clojure.string :as str]
            [config.core :refer [env]]
            [hiccup.page :refer [include-js include-css html5]]
            [schema.core :as s]
            [ring.middleware.anti-forgery :refer [*anti-forgery-token*]]
            [ring.middleware.jsonp :refer [wrap-json-with-padding]]
            [mtg-pairings-server.api.http :as http-api]
            [mtg-pairings-server.auth :refer [auth-routes]]
            [mtg-pairings-server.middleware :refer [wrap-site-middleware]]
            [mtg-pairings-server.middleware.cors :refer [wrap-allow-origin]]
            [mtg-pairings-server.middleware.log :refer [wrap-request-log]]
            [mtg-pairings-server.service.decklist :as decklist]
            [mtg-pairings-server.service.tournament :as tournament]
            [mtg-pairings-server.transit :as transit]
            [mtg-pairings-server.websocket :as ws]))

(defn escape-quotes [s]
  (str/escape s {\' "\\'"}))

(defn loading-page
  ([]
   (loading-page {}))
  ([initial-db]
   (let [html (html5
               {:lang :fi}
               [:head
                [:title "Pairings.fi"]
                [:meta {:charset "utf-8"}]
                [:meta {:name    "viewport"
                        :content "width=device-width, initial-scale=1"}]
                (include-css (if (env :dev)
                               "/css/main.css"
                               (env :main-css)))
                (when (env :dev)
                  (include-css "/css/slider.css"))]
               [:body {:class "body-container"}
                [:div#app]
                [:script (str "var csrf_token = '" *anti-forgery-token* "'; "
                              "var initial_db = '" (escape-quotes (transit/write initial-db)) "'; ")]
                (include-js (if (env :dev)
                              "/js/dev-main.js"
                              (env :main-js)))])]
     {:status  200
      :body    html
      :headers {"Content-Type"  "text/html"
                "Cache-Control" "no-cache"}})))

(def robots-txt
  {:status  200
   :body    "User-agent: *\nDisallow: /\n"
   :headers {"Content-Type" "text/plain"}})

(def forbidden {:status 403
                :body   "403 Forbidden"})

(defroutes site-routes
  (GET "/" [] (loading-page))
  (GET "/tournaments" []
    (loading-page {:page {:page :tournaments}}))
  (GET "/tournaments/:id" []
    :path-params [id :- s/Int]
    (loading-page {:page        {:page :tournament, :id id}
                   :tournaments {id (tournament/client-tournament id)}}))
  (GET "/tournaments/:id/pairings-:round" []
    :path-params [id :- s/Int
                  round :- s/Int]
    (loading-page {:page        {:page :pairings, :id id, :round round}
                   :tournaments {id (tournament/client-tournament id)}
                   :pairings    {id {round (tournament/get-round id round)}}}))
  (GET "/tournaments/:id/standings-:round" []
    :path-params [id :- s/Int
                  round :- s/Int]
    (loading-page {:page        {:page :standings, :id id, :round round}
                   :tournaments {id (tournament/client-tournament id)}
                   :standings   {id {round (tournament/standings id round false)}}}))
  (GET "/tournaments/:id/pods-:round" []
    :path-params [id :- s/Int
                  round :- s/Int]
    (loading-page {:page        {:page :pods, :id id, :round round}
                   :tournaments {id (tournament/client-tournament id)}
                   :pods        {id {round (tournament/pods id round)}}}))
  (GET "/tournaments/:id/seatings" []
    :path-params [id :- s/Int]
    (loading-page {:page        {:page :seatings, :id id}
                   :tournaments {id (tournament/client-tournament id)}
                   :seatings    {id (tournament/seatings id)}}))
  (GET "/tournaments/:id/bracket" []
    :path-params [id :- s/Int]
    (loading-page {:page        {:page :bracket, :id id}
                   :tournaments {id (tournament/client-tournament id)}
                   :bracket     {id (tournament/bracket id)}}))
  (GET "/tournaments/:id/organizer" [] (loading-page))
  (GET "/tournaments/:id/organizer/menu" [] (loading-page))
  (GET "/tournaments/:id/organizer/deck-construction" [] (loading-page))
  (GET "/decklist/tournament/:id" []
    :path-params [id :- s/Str]
    (loading-page {:page            {:page :decklist-submit}
                   :decklist-editor {:tournament (decklist/get-tournament id)}}))
  (GET "/decklist/organizer" request
    (loading-page {:page            {:page :decklist-organizer}
                   :decklist-editor {:organizer-tournaments (decklist/get-organizer-tournaments (get-in request [:session :identity :id]))}}))
  (GET "/decklist/organizer/new" []
    (loading-page {:page {:page :decklist-organizer-tournament}}))
  (GET "/decklist/organizer/view/:id" request
    :path-params [id :- s/Str]
    (let [decklist (decklist/get-decklist id)
          tournament (decklist/get-organizer-tournament (:tournament decklist))
          user-id (get-in request [:session :identity :id])]
      (cond
        (not user-id) (loading-page {:page {:page :decklist-organizer-view, :id id}})
        (= user-id (:user tournament)) (loading-page {:page            {:page :decklist-organizer-view, :id id}
                                                      :decklist-editor {:decklist             decklist
                                                                        :organizer-tournament tournament}})
        :else forbidden)))
  (GET "/decklist/organizer/:id" request
    :path-params [id :- s/Str]
    (let [tournament (decklist/get-organizer-tournament id)
          user-id (get-in request [:session :identity :id])]
      (cond
        (not user-id) (loading-page {:page {:page :decklist-organizer-tournament, :id id}})
        (= user-id (:user tournament)) (loading-page {:page            {:page :decklist-organizer-tournament, :id id}
                                                      :decklist-editor {:organizer-tournament tournament}})
        :else forbidden)))
  (GET "/decklist/:id" []
    :path-params [id :- s/Str]
    (let [decklist (decklist/get-decklist id)]
      (loading-page {:page            {:page :decklist-submit, :id id}
                     :decklist-editor {:tournament (decklist/get-tournament (:tournament decklist))
                                       :decklist   decklist}})))
  (GET "/robots.txt" [] robots-txt)
  auth-routes
  ws/routes)

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
             wrap-request-log
             wrap-allow-origin))
