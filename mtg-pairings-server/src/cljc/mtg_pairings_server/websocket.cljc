(ns mtg-pairings-server.websocket
  (:require [taoensso.sente :as sente]
            [taoensso.timbre :as log]
            #?(:clj  [mount.core :refer [defstate]]
               :cljs [mount.core :refer-macros [defstate]])
            [taoensso.sente.packers.transit :as sente-transit]
            [mtg-pairings-server.transit :refer [writers readers]]
            [mtg-pairings-server.util :refer [parse-iso-date format-iso-date]]
            #?(:clj [taoensso.sente.server-adapters.aleph :refer [get-sch-adapter]])
            #?(:clj [compojure.core :refer [defroutes GET POST]])
            #?(:cljs [oops.core :refer [oget]])))

;; Transit communication

(def packer (sente-transit/->TransitPacker :json
                                           {:handlers writers}
                                           {:handlers readers}))

;; Websocket API

(def path "/chsk")

(defn- init-ws []
  #?(:clj
     (let [{:keys [ch-recv send-fn ajax-post-fn ajax-get-or-ws-handshake-fn connected-uids]}
           (sente/make-channel-socket-server! (get-sch-adapter) {:packer     packer
                                                                 :user-id-fn (fn [ring-req] (:client-id ring-req))})]
       {:receive                     ch-recv
        :send!                       send-fn
        :connected-uids              connected-uids
        :ajax-post-fn                ajax-post-fn
        :ajax-get-or-ws-handshake-fn ajax-get-or-ws-handshake-fn})
     :cljs
     (let [{:keys [ch-recv send-fn chsk state]}
           (sente/make-channel-socket-client! path (oget js/window "csrf_token") {:packer packer
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
   (defn send!
     ([event]
      (send! event nil nil))
     ([event timeout callback]
      ((:send! @router) event timeout callback))))

#?(:clj (defroutes routes
          (GET path request ((:ajax-get-or-ws-handshake-fn @router) request))
          (POST path request ((:ajax-post-fn @router) request))))
