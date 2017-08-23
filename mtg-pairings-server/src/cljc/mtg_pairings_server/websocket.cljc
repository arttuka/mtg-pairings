(ns mtg-pairings-server.websocket
  (:require [taoensso.sente :as sente]
            [taoensso.timbre :as log]
            [mount.core :as m]
            [cognitect.transit :as transit]
            [taoensso.sente.packers.transit :as sente-transit]
    #?(:clj
            [taoensso.sente.server-adapters.http-kit :refer [get-sch-adapter]])))

;; Transit communication

(def packer (sente-transit/->TransitPacker :json
                                           {:handlers {}}
                                           {:handlers {}}))

;; Websocket API

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
     (let [{:keys [ch-recv send-fn chsk state]} (sente/make-channel-socket! "/chsk" {:packer packer
                                                                                     :type   :auto})]
       {:receive ch-recv
        :send!   send-fn
        :state   state
        :chsk    chsk})))


;; Event handler

(defmulti event-handler :id)

#?(:clj
   (defmethod event-handler :default [{:keys [event id ring-req ?data]}]
     (log/debugf "Unhandled event %s from client %s" event (get-in ring-req [:params :client-id]))))

#?(:cljs
   (defmethod event-handler :default [{:keys [event]}]
     (log/debugf "Unhandled event %s" event)))

;; Event router

(def ^:private ws-state
  (atom {:router         nil
         :connected-uids #{}
         :handler-fns    {:receive nil
                          :send!   nil
                          :state   nil
                          :chsk    nil}}))

(defn- call-ws-state-fns [fn-key & args]
  (if-let [f (fn-key (:handler-fns @ws-state))]
    (apply f args)
    (log/warn (str "websocket connection not initialized for fn-key " fn-key))))

; both ends have these
(def receive (partial call-ws-state-fns :ch-recv))
(def send! (partial call-ws-state-fns :send!))

#?(:clj (def ajax-get-or-ws-handshake-fn (partial call-ws-state-fns :ajax-get-or-ws-handshake-fn)))
#?(:clj (def ajax-post-fn (partial call-ws-state-fns :ajax-post-fn)))

#?(:cljs (def chsk (partial call-ws-state-fns :chsk)))

#?(:clj (defn send-all! [event]
          (doseq [uid (:connected-uids)]
            (send! uid event))))


(defn stop-router!
  []
  (when-let [stop! (:router @ws-state)]
    #?(:cljs (sente/chsk-disconnect! (:chsk (:handler-fns @ws-state))))
    (log/info "Stopping websocket router")
    (stop!)
    (reset! ws-state {})))

(defn start-router!
  []
  (stop-router!)
  (log/info "Starting websocket router")
  (swap! ws-state
         (fn [& _]
           (let [handler-fns (init-ws)]
             {:handler-fns    (dissoc handler-fns :connected-uids)
              :connected-uids (:connected-uids handler-fns)
              :router         (sente/start-chsk-router! (:receive handler-fns) event-handler)}))))

(m/defstate router
  :start (start-router!)
  :stop (stop-router!))
