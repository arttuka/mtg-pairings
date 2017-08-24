(ns mtg-pairings-server.events
  (:require [re-frame.core :refer [dispatch reg-fx reg-event-db]]
            [mtg-pairings-server.util.util :refer [map-by]]
            [mtg-pairings-server.websocket :as ws]))

(defmethod ws/event-handler :chsk/recv
  [{:keys [?data]}]
  (let [[event data] ?data]
    (dispatch [event data])))

(reg-fx :ws-send
  (fn [event]
    (if @ws/channel-open?
      (ws/send! event)
      (let [k (gensym)]
        (add-watch ws/channel-open? k (fn [_ _ _ new-val]
                                        (when new-val
                                          (remove-watch ws/channel-open? k)
                                          (ws/send! event))))))))

(def initial-db {:tournaments {}
                 :tournament-ids []
                 :page {:page :main}})

(reg-event-db :initialize
  (fn [db _]
    (merge db initial-db)))

(reg-event-db :page
  (fn [db [_ data]]
    (assoc db :page data)))

(defn format-tournament [tournament]
  (let [rounds (sort > (into (set (:pairings tournament)) (:standings tournament)))]
    (-> tournament
        (update :pairings set)
        (update :standings set)
        (assoc :round-nums rounds))))

(reg-event-db :server/tournaments
  (fn [db [_ tournaments]]
    (assoc db :tournaments (map-by :id (map format-tournament tournaments))
              :tournament-ids (map :id tournaments))))
