(ns mtg-pairings-server.handler
  (:require [compojure.api.sweet :refer :all]
            [compojure.route :refer [not-found resources]]
            [clojure.string :as str]
            [config.core :refer [env]]
            [hiccup.page :refer [include-js include-css html5]]
            [schema.core :as s]
            [taoensso.timbre :as log]
            [ring.middleware.anti-forgery :refer [*anti-forgery-token*]]
            [ring.middleware.jsonp :refer [wrap-json-with-padding]]
            [mtg-pairings-server.api.http :as http-api]
            [mtg-pairings-server.auth :refer [auth-routes]]
            [mtg-pairings-server.middleware :refer [wrap-site-middleware]]
            [mtg-pairings-server.middleware.cors :refer [wrap-allow-origin]]
            [mtg-pairings-server.middleware.log :refer [wrap-request-log]]
            [mtg-pairings-server.service.decklist :as decklist]
            [mtg-pairings-server.service.tournament :as tournament]
            [mtg-pairings-server.service.player :as player]
            [mtg-pairings-server.transit :as transit]
            [mtg-pairings-server.util.broadcast :as broadcast]
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

(defmethod ws/event-handler
  :chsk/uidport-open
  [_])

(defmethod ws/event-handler :chsk/uidport-close
  [{:keys [uid]}]
  (broadcast/disconnect uid))

(defmethod ws/event-handler :client/connect
  [{:keys [uid ring-req]}]
  (log/debugf "New connection from %s" uid)
  (ws/send! uid [:server/tournaments (tournament/client-tournaments)])
  (ws/send! uid [:server/organizer-login (get-in ring-req [:session :identity :username] false)]))

(defmethod ws/event-handler :client/login
  [{uid :uid, dci-number :?data}]
  (try
    (if-let [player (player/player dci-number)]
      (do
        (ws/send! uid [:server/login player])
        (ws/send! uid [:server/player-tournaments (player/tournaments dci-number)])
        (broadcast/login uid dci-number))
      (ws/send! uid [:server/login nil]))
    (catch NumberFormatException _
      (ws/send! uid [:server/login nil]))))

(defmethod ws/event-handler :client/logout
  [{:keys [uid]}]
  (broadcast/logout uid))

(defmethod ws/event-handler :client/tournaments
  [{:keys [uid]}]
  (ws/send! uid [:server/tournaments (tournament/client-tournaments)]))

(defmethod ws/event-handler :client/tournament
  [{uid :uid, id :?data}]
  (ws/send! uid [:server/tournament (tournament/client-tournament id)]))

(defmethod ws/event-handler :client/pairings
  [{uid :uid, [id round] :?data}]
  (ws/send! uid [:server/pairings [id round (tournament/get-round id round)]]))

(defmethod ws/event-handler :client/standings
  [{uid :uid, [id round] :?data}]
  (ws/send! uid [:server/standings [id round (tournament/standings id round false)]]))

(defmethod ws/event-handler :client/pods
  [{uid :uid, [id round] :?data}]
  (ws/send! uid [:server/pods [id round (tournament/pods id round)]]))

(defmethod ws/event-handler :client/seatings
  [{uid :uid, id :?data}]
  (ws/send! uid [:server/seatings [id (tournament/seatings id)]]))

(defmethod ws/event-handler :client/bracket
  [{uid :uid, id :?data}]
  (ws/send! uid [:server/bracket [id (tournament/bracket id)]]))

(defmethod ws/event-handler :client/organizer-tournament
  [{uid :uid, id :?data}]
  (broadcast/watch uid id)
  (ws/send! uid [:server/organizer-tournament (tournament/tournament id)]))

(defmethod ws/event-handler :client/organizer-pairings
  [{uid :uid, [id round] :?data}]
  (ws/send! uid [:server/organizer-pairings (tournament/get-round id round)]))

(defmethod ws/event-handler :client/organizer-standings
  [{uid :uid, [id round] :?data}]
  (ws/send! uid [:server/organizer-standings (tournament/standings id round false)]))

(defmethod ws/event-handler :client/organizer-pods
  [{uid :uid, [id number] :?data}]
  (when-not (Double/isNaN number)
    (ws/send! uid [:server/organizer-pods (tournament/pods id number)])))

(defmethod ws/event-handler :client/organizer-seatings
  [{uid :uid, id :?data}]
  (ws/send! uid [:server/organizer-seatings (tournament/seatings id)]))

(defmethod ws/event-handler :client/deck-construction
  [{uid :uid, id :?data}]
  (ws/send! uid [:server/organizer-seatings (tournament/seatings id)])
  (ws/send! uid [:server/organizer-pods (tournament/latest-pods id)]))

(defmethod ws/event-handler :client/save-decklist
  [{uid :uid, [tournament decklist] :?data}]
  (let [id (decklist/save-decklist tournament decklist)]
    (ws/send! uid [:server/decklist-saved id])))

(defmethod ws/event-handler :client/decklist-organizer-tournament
  [{uid :uid, id :?data, ring-req :ring-req}]
  (let [tournament (decklist/get-organizer-tournament id)]
    (when (= (get-in ring-req [:session :identity :id]) (:user tournament))
      (ws/send! uid [:server/decklist-organizer-tournament tournament]))))

(defmethod ws/event-handler :client/save-decklist-organizer-tournament
  [{uid :uid, tournament :?data, ring-req :ring-req}]
  (let [user-id (get-in ring-req [:session :identity :id])
        id (decklist/save-organizer-tournament user-id tournament)]
    (when id
      (ws/send! uid [:server/organizer-tournament-saved id]))))

(defmethod ws/event-handler :client/load-organizer-tournament-decklist
  [{uid :uid, id :?data}]
  (ws/send! uid [:server/organizer-tournament-decklist (decklist/get-decklist id)]))

(defmethod ws/event-handler :client/load-organizer-tournament-decklists
  [{uid :uid, ids :?data}]
  (ws/send! uid [:server/organizer-tournament-decklists (->> (map decklist/get-decklist ids)
                                                             (sort-by (fn [d]
                                                                        [(get-in d [:player :last-name])
                                                                         (get-in d [:player :first-name])])))]))

(defmethod ws/event-handler :client/load-decklist-with-id
  [{uid :uid, id :?data}]
  (if-let [decklist (decklist/get-decklist id)]
    (ws/send! uid [:server/organizer-tournament-decklist (dissoc decklist :id :player)])
    (ws/send! uid [:server/decklist-load-error "Pakkalistaa ei löytynyt"])))

(defmethod ws/event-handler :client/decklist-card-suggestions
  [{[prefix format] :?data, reply-fn :?reply-fn}]
  (reply-fn (decklist/search-cards prefix format)))

(defmethod ws/event-handler :client/load-text-decklist
  [{uid :uid, [text-decklist format] :?data}]
  (ws/send! uid [:server/organizer-tournament-decklist (decklist/load-text-decklist text-decklist format)]))
