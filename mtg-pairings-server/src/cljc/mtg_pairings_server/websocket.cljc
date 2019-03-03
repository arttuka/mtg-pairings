(ns mtg-pairings-server.websocket
  (:require [taoensso.sente :as sente]
            [taoensso.timbre :as log]
            #?(:clj  [mount.core :refer [defstate]]
               :cljs [mount.core :refer-macros [defstate]])
            [cognitect.transit :as transit]
            [taoensso.sente.packers.transit :as sente-transit]
            [mtg-pairings-server.util :refer [parse-iso-date format-iso-date]]
            #?(:clj [taoensso.sente.server-adapters.http-kit :refer [get-sch-adapter]])
            #?(:clj [compojure.core :refer [defroutes GET POST]])))

;; Transit communication

(def writers
  {#?(:clj org.joda.time.LocalDate, :cljs goog.date.Date)
   (transit/write-handler (constantly "Date") format-iso-date)
   #?@(:clj [clojure.lang.Ratio
             (transit/write-handler (constantly "d") double)])})

(def readers
  {"Date" (transit/read-handler #(parse-iso-date %))})

(def packer (sente-transit/->TransitPacker :json
                                           {:handlers writers}
                                           {:handlers readers}))

;; Websocket API

(def path "/chsk")

(defn- init-ws []
  #?(:clj
     (let [{:keys [ch-recv send-fn ajax-post-fn ajax-get-or-ws-handshake-fn connected-uids]}
           (sente/make-channel-socket! (get-sch-adapter) {:packer     packer
                                                          :user-id-fn (fn [ring-req] (:client-id ring-req))})]
       {:receive                     ch-recv
        :send!                       send-fn
        :connected-uids              connected-uids
        :ajax-post-fn                ajax-post-fn
        :ajax-get-or-ws-handshake-fn ajax-get-or-ws-handshake-fn})
     :cljs
     (let [{:keys [ch-recv send-fn chsk state]} (sente/make-channel-socket! path {:packer packer
                                                                                  :type   :auto})]
       {:receive ch-recv
        :send!   send-fn
        :state   state
        :chsk    chsk})))

;; Event handler

(defmulti event-handler :id)

#?(:clj
   (defmethod event-handler :default [{:keys [event ring-req]}]
     (log/debugf "Unhandled event %s from client %s" event (get-in ring-req [:params :client-id]))))

#?(:cljs
   (defmethod event-handler :default [{:keys [event]}]
     (log/debugf "Unhandled event %s" event)))

#?(:clj
   (defmethod event-handler :chsk/ws-ping [_]))

;; Event router

(defn start-router []
  (let [conn (init-ws)]
    (assoc conn :router (sente/start-chsk-router! (:receive conn) event-handler))))

(defn stop-router [r]
  #?(:cljs (sente/chsk-disconnect! (:chsk r)))
  ((:router r)))

(defstate router
  :start (start-router)
  :stop (stop-router @router))

#?(:clj
   (defn send! [uid event]
     ((:send! @router) uid event))
   :cljs
   (defn send! [event]
     ((:send! @router) event)))

#?(:clj (defroutes routes
          (GET path request ((:ajax-get-or-ws-handshake-fn @router) request))
          (POST path request ((:ajax-post-fn @router) request))))
