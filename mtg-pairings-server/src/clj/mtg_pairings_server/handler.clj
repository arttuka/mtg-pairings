(ns mtg-pairings-server.handler
  (:require [compojure.core :refer [GET POST defroutes]]
            [compojure.route :refer [not-found resources]]
            [config.core :refer [env]]
            [hiccup.page :refer [include-js include-css html5]]
            [taoensso.timbre :as log]
            [mtg-pairings-server.api.http :as http-api]
            [mtg-pairings-server.middleware :refer [wrap-middleware]]
            [mtg-pairings-server.service.tournament :as tournament]
            [mtg-pairings-server.service.player :as player]
            [mtg-pairings-server.websocket :as ws]))

(def mount-target
  [:div#app
      [:h3 "ClojureScript has not been compiled!"]
      [:p "please run "
       [:b "lein figwheel"]
       " in order to start the compiler"]])

(defn head []
  [:head
   [:meta {:charset "utf-8"}]
   [:meta {:name "viewport"
           :content "width=device-width, initial-scale=1"}]
   (include-css "/css/main.css")
   (include-css "/css/bootstrap.css")
   (include-css "/css/bootstrap-theme.css")])

(defn loading-page []
  (html5
    (head)
    [:body {:class "body-container"}
     mount-target
     (include-js "/js/app.js")]))


(defroutes routes
  (GET "/" [] (loading-page))
  (GET "/tournaments" [] (loading-page))
  (GET "/tournaments/:id" [] (loading-page))
  (GET "/tournaments/:id/pairings-:round" [] (loading-page))
  (GET "/tournaments/:id/standings-:round" [] (loading-page))
  (GET "/tournaments/:id/pods-:round" [] (loading-page))
  (GET "/tournaments/:id/seatings" [] (loading-page))
  (GET "/chsk" request
    (ws/ajax-get-or-ws-handshake-fn request))
  (POST "/chsk" request
    (ws/ajax-post-fn request))

  http-api/app
  (resources "/")
  (not-found "Not Found"))

(def app (wrap-middleware #'routes))

(defmethod ws/event-handler :client/connect
  [{:keys [uid]}]
  (log/debugf "New connection from %s" uid)
  (ws/send! uid [:server/tournaments (tournament/tournaments)]))

(defmethod ws/event-handler :client/login
  [{uid :uid, dci-number :?data}]
  (ws/send! uid [:server/login (player/player dci-number)]))

(defmethod ws/event-handler :client/logout)

(defmethod ws/event-handler :client/pairings
  [{uid :uid, [id round] :?data}]
  (ws/send! uid [:server/pairings [id round (tournament/get-round id round)]]))

(defmethod ws/event-handler :client/standings
  [{uid :uid, [id round] :?data}]
  (ws/send! uid [:server/standings [id round (tournament/standings id round true)]]))

(defmethod ws/event-handler :client/pods
  [{uid :uid, [id round] :?data}]
  (ws/send! uid [:server/pods [id round (tournament/pods id round)]]))

(defmethod ws/event-handler :client/seatings
  [{uid :uid, id :?data}]
  (ws/send! uid [:server/seatings [id (tournament/seatings id)]]))
