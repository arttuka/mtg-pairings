(ns mtg-pairings-server.handler
  (:require [compojure.core :refer [GET POST defroutes]]
            [compojure.route :refer [not-found resources]]
            [hiccup.page :refer [include-js include-css html5]]
            [mtg-pairings-server.middleware :refer [wrap-middleware]]
            [mtg-pairings-server.http-api :as http-api]
            [mtg-pairings-server.websocket :as ws]
            [config.core :refer [env]]))

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
   (include-css (if (env :dev) "/css/site.css" "/css/site.min.css"))])

(defn loading-page []
  (html5
    (head)
    [:body {:class "body-container"}
     mount-target
     (include-js "/js/app.js")]))


(defroutes routes
  (GET "/" [] (loading-page))
  (GET "/chsk" request
         (ws/ajax-get-or-ws-handshake-fn request))
  (POST "/chsk" request
          (ws/ajax-post-fn request))

  http-api/app
  (resources "/")
  (not-found "Not Found"))

(def app (wrap-middleware #'routes))
