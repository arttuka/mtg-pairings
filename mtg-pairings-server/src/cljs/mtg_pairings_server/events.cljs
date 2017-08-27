(ns mtg-pairings-server.events
  (:require [re-frame.core :refer [dispatch reg-fx reg-event-db reg-event-fx]]
            [mtg-pairings-server.util.local-storage :refer [fetch store]]
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

(reg-fx :store
  (fn [[key obj]]
    (store key obj)))

(def initial-db {:tournaments    {}
                 :tournament-ids []
                 :pairings       {:sort-key :table_number}
                 :pods           {:sort-key :pod}
                 :seatings       {:sort-key :table_number}
                 :page           {:page :main}
                 :logged-in-user (fetch :user)})

(reg-event-db :initialize
  (fn [db _]
    (merge db initial-db)))

(reg-event-fx :login
  (fn [_ [_ dci-number]]
    {:ws-send [:client/login dci-number]}))

(reg-event-fx :server/login
  (fn [{:keys [db]} [_ user]]
    {:db    (assoc db :logged-in-user user)
     :store [:user user]}))

(reg-event-fx :logout
  (fn [{:keys [db]} _]
    {:dispatch [:client/logout]
     :db       (assoc db :logged-in-user nil)
     :store    [:user nil]}))

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

(reg-event-fx :load-pairings
  (fn [_ [_ id round]]
    {:ws-send [:client/pairings [id round]]}))

(reg-event-db :server/pairings
  (fn [db [_ [id round pairings]]]
    (assoc-in db [:pairings id round] pairings)))

(reg-event-db :sort-pairings
  (fn [db [_ sort-key]]
    (assoc-in db [:pairings :sort-key] sort-key)))

(reg-event-fx :load-standings
  (fn [_ [_ id round]]
    {:ws-send [:client/standings [id round]]}))

(reg-event-db :server/standings
  (fn [db [_ [id round standings]]]
    (assoc-in db [:standings id round] standings)))

(reg-event-fx :load-pods
  (fn [_ [_ id round]]
    {:ws-send [:client/pods [id round]]}))

(reg-event-db :server/pods
  (fn [db [_ [id round pods]]]
    (assoc-in db [:pods id round] pods)))

(reg-event-db :sort-pods
  (fn [db [_ sort-key]]
    (assoc-in db [:pods :sort-key] sort-key)))

(reg-event-fx :load-seatings
  (fn [_ [_ id]]
    {:ws-send [:client/seatings id]}))

(reg-event-db :server/seatings
  (fn [db [_ [id seatings]]]
    (assoc-in db [:seatings id] seatings)))

(reg-event-db :sort-seatings
  (fn [db [_ sort-key]]
    (assoc-in db [:seatings :sort-key] sort-key)))
