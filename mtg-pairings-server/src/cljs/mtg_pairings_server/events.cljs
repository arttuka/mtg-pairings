(ns mtg-pairings-server.events
  (:require [re-frame.core :refer [dispatch reg-fx reg-event-db reg-event-fx]]
            [cljs.core.async :as async :refer [<! >! timeout]]
            [cljs-time.core :as time]
            [mtg-pairings-server.util.local-storage :refer [fetch store]]
            [mtg-pairings-server.util.util :refer [map-by format-time assoc-in-many round-up]]
            [mtg-pairings-server.websocket :as ws])
  (:require-macros [cljs.core.async.macros :refer [go go-loop]]))

(defmethod ws/event-handler :chsk/recv
  [{:keys [?data]}]
  (let [[event data] ?data]
    (dispatch [event data])))


(def channel-open? (atom false))

(reg-fx :ws-send
  (fn [event]
    (if @channel-open?
      (ws/send! event)
      (let [k (gensym)]
        (add-watch channel-open? k (fn [_ _ _ new-val]
                                     (when new-val
                                       (remove-watch channel-open? k)
                                       (ws/send! event))))))))

(reg-fx :store
  (fn [[key obj]]
    (store key obj)))

(def initial-db {:tournaments            {}
                 :tournament-count       0
                 :tournaments-page       0
                 :tournament-ids         []
                 :tournament-filter      {:organizer ""
                                          :date-from nil
                                          :date-to   nil
                                          :players   [0 100]}
                 :max-players            100
                 :player-tournaments     []
                 :pairings               {:sort-key :table_number}
                 :pods                   {:sort-key :pod}
                 :seatings               {:sort-key :table_number}
                 :page                   {:page :main}
                 :logged-in-user         (fetch :user)
                 :mobile-menu-collapsed? true})

(reg-event-db ::initialize
  (fn [db _]
    (merge db initial-db)))

(defmethod ws/event-handler :chsk/state
  [{:keys [?data]}]
  (let [[_ new-state] ?data]
    (when (:first-open? new-state)
      (reset! channel-open? true)
      (ws/send! [:client/connect])
      (when-let [user (fetch :user)]
        (ws/send! [:client/login (:dci user)])))))

(reg-event-fx ::login
  (fn [_ [_ dci-number]]
    {:ws-send [:client/login dci-number]}))

(reg-event-fx :server/login
  (fn [{:keys [db]} [_ user]]
    {:db    (assoc db :logged-in-user user)
     :store [:user user]}))

(reg-event-fx ::logout
  (fn [{:keys [db]} _]
    {:ws-send [:client/logout]
     :db      (assoc db :logged-in-user nil)
     :store   [:user nil]}))

(reg-event-db ::page
  (fn [db [_ data]]
    (assoc db :page data)))

(reg-event-db ::tournaments-page
  (fn [db [_ page]]
    (assoc db :tournaments-page page)))

(reg-event-db ::tournament-filter
  (fn [db [_ [key value]]]
    (-> db
        (assoc-in [:tournament-filter key] value)
        (assoc :tournaments-page 0))))

(defn format-tournament [tournament]
  (let [rounds (sort > (into (set (:pairings tournament)) (:standings tournament)))]
    (-> tournament
        (update :pairings set)
        (update :standings set)
        (assoc :round-nums rounds))))

(reg-event-db :server/tournaments
  (fn [db [_ tournaments]]
    (let [max-players (round-up (transduce (map :players) max 0 tournaments) 10)]
      (-> db
          (assoc :tournaments (map-by :id (map format-tournament tournaments))
                 :tournament-ids (map :id tournaments)
                 :max-players max-players)
          (assoc-in [:tournament-filter :players 1] max-players)))))

(reg-event-fx ::load-pairings
  (fn [_ [_ id round]]
    {:ws-send [:client/pairings [id round]]}))

(reg-event-db :server/pairings
  (fn [db [_ [id round pairings]]]
    (assoc-in db [:pairings id round] pairings)))

(reg-event-db ::sort-pairings
  (fn [db [_ sort-key]]
    (assoc-in db [:pairings :sort-key] sort-key)))

(reg-event-fx ::load-standings
  (fn [_ [_ id round]]
    {:ws-send [:client/standings [id round]]}))

(reg-event-db :server/standings
  (fn [db [_ [id round standings]]]
    (assoc-in db [:standings id round] standings)))

(reg-event-fx ::load-pods
  (fn [_ [_ id round]]
    {:ws-send [:client/pods [id round]]}))

(reg-event-db :server/pods
  (fn [db [_ [id round pods]]]
    (assoc-in db [:pods id round] pods)))

(reg-event-db ::sort-pods
  (fn [db [_ sort-key]]
    (assoc-in db [:pods :sort-key] sort-key)))

(reg-event-fx ::load-seatings
  (fn [_ [_ id]]
    {:ws-send [:client/seatings id]}))

(reg-event-db :server/seatings
  (fn [db [_ [id seatings]]]
    (assoc-in db [:seatings id] seatings)))

(reg-event-db ::sort-seatings
  (fn [db [_ sort-key]]
    (assoc-in db [:seatings :sort-key] sort-key)))

(reg-event-fx ::load-bracket
  (fn [_ [_ id]]
    {:ws-send [:client/bracket id]}))

(reg-event-db :server/bracket
  (fn [db [_ [id bracket]]]
    (assoc-in db [:bracket id] bracket)))

(reg-event-db :server/player-tournaments
  (fn [db [_ tournaments]]
    (assoc db :player-tournaments (vec tournaments))))

(reg-event-db :server/player-tournament
  (fn [db [_ tournament]]
    (assoc-in db [:player-tournaments 0] tournament)))

(reg-event-fx ::load-organizer-tournament
  (fn [_ [_ id]]
    {:ws-send [:client/organizer-tournament id]}))

(reg-event-db :server/organizer-tournament
  (fn [db [_ tournament]]
    (cond-> db
      (not= (:pairings tournament) (get-in db [:organizer :tournament :pairings]))
      (assoc-in-many [:organizer :new-pairings] true
                     [:organizer :selected-pairings] (str (last (:pairings tournament))))

      (not= (:standings tournament) (get-in db [:organizer :tournament :standings]))
      (assoc-in-many [:organizer :new-standings] true
                     [:organizer :selected-standings] (str (last (:standings tournament))))

      (not= (:pods tournament) (get-in db [:organizer :tournament :pods]))
      (assoc-in-many [:organizer :new-pods] true
                     [:organizer :selected-pods] (str (last (:pods tournament))))

      :always
      (assoc-in [:organizer :tournament] tournament))))

(reg-event-db :server/organizer-pairings
  (fn [db [_ pairings]]
    (assoc-in db [:organizer :pairings] pairings)))

(reg-event-db :server/organizer-standings
  (fn [db [_ standings]]
    (assoc-in db [:organizer :standings] standings)))

(reg-event-db :server/organizer-pods
  (fn [db [_ pods]]
    (assoc-in db [:organizer :pods] pods)))

(reg-event-db :server/organizer-seatings
  (fn [db [_ seatings]]
    (assoc-in db [:organizer :seatings] seatings)))

(reg-event-db ::update-clock
  (fn [db _]
    (if-let [start (get-in db [:organizer :clock :start])]
      (let [now (time/now)
            diff (/ (time/in-millis (time/interval start now)) 1000)
            total (- (get-in db [:organizer :clock :time]) diff)]
        (update-in db [:organizer :clock] into {:text    (format-time total)
                                                :timeout (neg? total)
                                                :time    total
                                                :start   now}))
      db)))

(defonce ^:private running (atom false))

(reg-fx :clock
  (fn [action]
    (case action
      :start (do
               (reset! running true)
               (go-loop []
                 (<! (timeout 200))
                 (dispatch [::update-clock])
                 (when @running (recur))))
      :stop (reset! running false))))

(defn resolve-organizer-action [db id action value]
  (case action
    :pairings {:ws-send [:client/organizer-pairings [id value]]
               :db      (assoc-in-many db [:organizer :mode] :pairings
                                          [:organizer :pairings-round] value
                                          [:organizer :new-pairings] false)}
    :standings {:ws-send [:client/organizer-standings [id value]]
                :db      (assoc-in-many db [:organizer :mode] :standings
                                           [:organizer :standings-round] value
                                           [:organizer :new-standings] false)}
    :seatings {:ws-send [:client/organizer-seatings id]
               :db      (assoc-in db [:organizer :mode] :seatings)}
    :pods {:ws-send [:client/organizer-pods [id value]]
           :db      (assoc-in-many db [:organizer :mode] :pods
                                      [:organizer :new-pods] false)}
    :clock {:db (assoc-in db [:organizer :mode] :clock)}
    :set-clock {:db (update-in db [:organizer :clock] (fnil into {}) {:time    (* value 60)
                                                                      :text    (format-time (* value 60))
                                                                      :timeout false})}
    :start-clock {:clock :start
                  :db    (update-in db [:organizer :clock] (fnil into {}) {:start   (time/now)
                                                                           :running true})}
    :stop-clock {:clock :stop
                 :db    (assoc-in db [:organizer :clock :running] false)}
    :select-pairings {:db (assoc-in db [:organizer :selected-pairings] value)}
    :select-standings {:db (assoc-in db [:organizer :selected-standings] value)}
    :select-pods {:db (assoc-in db [:organizer :selected-pods] value)}))

(defn send-organizer-action [db id action value]
  (store ["organizer" id] {:action action, :value value})
  (case action
    :start-clock {:db (assoc-in db [:organizer :clock :running] true)}
    :stop-clock {:db (assoc-in db [:organizer :clock :running] false)}
    {}))

(reg-event-fx ::organizer-mode
  (fn [{:keys [db]} [_ action value]]
    (let [{:keys [id page]} (:page db)]
      (case page
        :organizer (resolve-organizer-action db id action value)
        :organizer-menu (send-organizer-action db id action value)))))

(reg-fx :popup
  (fn [id]
    (.open js/window (str "/tournaments/" id "/organizer/menu") (str "menu" id))))

(reg-event-fx ::popup-organizer-menu
  (fn [{:keys [db]} _]
    {:db    (assoc-in db [:organizer :menu] true)
     :popup (get-in db [:page :id])}))

(reg-event-fx ::local-storage-updated
  (fn [{:keys [db]} [_ k v]]
    (when (and (= (get-in db [:page :page]) :organizer)
               (= k ["organizer" (get-in db [:page :id])]))
      (resolve-organizer-action db
                                (get-in db [:page :id])
                                (keyword (:action v))
                                (:value v)))))

(reg-event-db ::collapse-mobile-menu
  (fn [db _]
    (update db :mobile-menu-collapsed? not)))

(reg-event-fx ::load-deck-construction
  (fn [_ [_ id]]
    {:ws-send [:client/deck-construction id]}))
