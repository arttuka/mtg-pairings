(ns mtg-pairings-server.events.common
  (:require [re-frame.core :refer [dispatch reg-fx reg-event-db reg-event-fx]]
            [accountant.core :as accountant]
            [mtg-pairings-server.util.local-storage :as local-storage]
            [mtg-pairings-server.util.styles :refer [mobile?]]
            [mtg-pairings-server.websocket :as ws]))

(defmethod ws/event-handler :chsk/recv
  [{:keys [?data]}]
  (let [[event data] ?data]
    (dispatch [event data])))

(reg-fx :ws-send
  (fn ws-send
    [data]
    (let [[event timeout callback] (if (keyword? (first data))
                                     [data nil nil]
                                     data)]
      (ws/send! event timeout callback))))

(reg-event-db ::window-resized
  (fn [db _]
    (assoc db :mobile? (mobile?))))

(reg-fx :navigate
  (fn [path]
    (accountant/navigate! path)))

(reg-fx :store
  (fn [[key obj]]
    (local-storage/store key obj)))

(reg-event-db ::page
  (fn [db [_ data]]
    (assoc db :page data)))

(reg-event-fx ::set-language
  (fn [{:keys [db]} [_ language]]
    {:db    (assoc db :language language)
     :store [:stored-language language]}))
