(ns mtg-pairings-server.handler
  (:require [compojure.core :refer [GET POST defroutes] :as c]
            [compojure.route :refer [not-found resources]]
            [config.core :refer [env]]
            [hiccup.page :refer [include-js include-css html5]]
            [taoensso.timbre :as log]
            [mtg-pairings-server.api.http :as http-api]
            [mtg-pairings-server.middleware :refer [wrap-site-middleware wrap-api-middleware]]
            [mtg-pairings-server.service.tournament :as tournament]
            [mtg-pairings-server.service.player :as player]
            [mtg-pairings-server.util.broadcast :as broadcast]
            [mtg-pairings-server.websocket :as ws]))

(let [html (delay (html5
                    {:lang :fi}
                    [:head
                     [:title "Pairings.fi"]
                     [:meta {:charset "utf-8"}]
                     [:meta {:name    "viewport"
                             :content "width=device-width, initial-scale=1"}]
                     (include-css "https://fonts.googleapis.com/css?family=Lato:400,700"
                                  "https://fonts.googleapis.com/css?family=Roboto:300,400,500"
                                  (if (env :dev)
                                    "/css/main.css"
                                    "/css/main.min.css"))]
                    [:body {:class "body-container"}
                     [:div#app]
                     (include-js (if (env :dev)
                                   "/js/dev-main.js"
                                   "/js/prod-main.js"))]))]
  (defn loading-page [] @html))


(defroutes site-routes
  (GET "/" [] (loading-page))
  (GET "/tournaments" [] (loading-page))
  (GET "/tournaments/:id" [] (loading-page))
  (GET "/tournaments/:id/pairings-:round" [] (loading-page))
  (GET "/tournaments/:id/standings-:round" [] (loading-page))
  (GET "/tournaments/:id/pods-:round" [] (loading-page))
  (GET "/tournaments/:id/seatings" [] (loading-page))
  (GET "/tournaments/:id/bracket" [] (loading-page))
  (GET "/tournaments/:id/organizer" [] (loading-page))
  (GET "/tournaments/:id/organizer/menu" [] (loading-page))
  (GET "/tournaments/:id/organizer/deck-construction" [] (loading-page))
  ws/routes)

(defroutes app
  (c/routes
    (wrap-api-middleware #'http-api/app)
    (wrap-site-middleware #'site-routes)
    (wrap-site-middleware (resources "/"))
    (wrap-site-middleware (not-found "Not Found"))))

(defmethod ws/event-handler
  :chsk/uidport-open
  [_])

(defmethod ws/event-handler :chsk/uidport-close
  [{:keys [uid]}]
  (broadcast/disconnect uid))

(defmethod ws/event-handler :client/connect
  [{:keys [uid]}]
  (log/debugf "New connection from %s" uid)
  (ws/send! uid [:server/tournaments (tournament/tournaments)]))

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
  (ws/send! uid [:server/tournaments (tournament/tournaments)]))

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
