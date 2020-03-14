(ns mtg-pairings-server.websocket
  (:require [taoensso.sente :as sente]
            [taoensso.timbre :as log]
            #?(:clj  [mount.core :refer [defstate]]
               :cljs [mount.core :refer-macros [defstate]])
            [taoensso.sente.packers.transit :as sente-transit]
            [mtg-pairings-server.transit :refer [writers readers]]
            [mtg-pairings-server.util :refer [parse-iso-date format-iso-date]]
            #?(:clj [mtg-pairings-server.db :as db])
            #?(:clj [taoensso.sente.server-adapters.aleph :refer [get-sch-adapter]])
            #?(:clj [compojure.core :refer [defroutes GET POST]])))

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
           (sente/make-channel-socket-client! path js/csrfToken {:packer packer
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

(defn event-handler* [event]
  (log/debugf "handling event %s" (first (:event event)))
  #?(:clj  (db/with-transaction
             (event-handler event))
     :cljs (event-handler event)))

;; Event router

(declare router)

(defn ws-state []
  (some-> router deref :state deref))

#?(:cljs
   (defonce stored-events (atom [])))

#?(:cljs
   (defmethod event-handler :chsk/state
     [{:keys [?data]}]
     (let [[_ new-state] ?data]
       (when (:open? new-state)
         (let [[events _] (swap-vals! stored-events (constantly []))]
           (doseq [[event timeout callback] events]
             ((:send! @router) event timeout callback)))))))

(defn start-router []
  (let [conn (init-ws)]
    (assoc conn :router (sente/start-chsk-router! (:receive conn) event-handler*))))

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
      (if (:open? (ws-state))
        ((:send! @router) event timeout callback)
        (swap! stored-events conj [event timeout callback])))))

#?(:clj (defroutes routes
          (GET path request ((:ajax-get-or-ws-handshake-fn @router) request))
          (POST path request ((:ajax-post-fn @router) request))))
