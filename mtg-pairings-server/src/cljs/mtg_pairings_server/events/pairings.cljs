(ns mtg-pairings-server.events.pairings
  (:require [re-frame.core :refer [dispatch reg-fx reg-event-db reg-event-fx]]
            [cljs-time.core :as time]
            [mtg-pairings-server.i18n.common :as i18n]
            [mtg-pairings-server.transit :as transit]
            [mtg-pairings-server.util :refer [map-by format-time assoc-in-many deep-merge round-up dissoc-in
                                              index-where dissoc-index]]
            [mtg-pairings-server.util.local-storage :as local-storage]
            [mtg-pairings-server.util.styles :refer [mobile?]]
            [mtg-pairings-server.websocket :as ws]))

(defn initial-db []
  (deep-merge {:tournaments        {}
               :tournament-count   0
               :tournaments-page   0
               :tournament-ids     []
               :tournament-filter  {:organizer "all-organizers"
                                    :date-from nil
                                    :date-to   nil
                                    :players   [0 100]}
               :filters-active     false
               :max-players        100
               :player-tournaments []
               :pairings           {:sort-key :table_number}
               :pods               {:sort-key :pod}
               :seatings           {:sort-key :table_number}
               :page               {:page  nil
                                    :id    nil
                                    :round nil}
               :logged-in-user     (local-storage/fetch :user)
               :notification       nil
               :language           (i18n/language :fi)
               :mobile?            (mobile?)
               :window-size        [(.-innerWidth js/window)
                                    (.-innerHeight js/window)]}
              (transit/read js/initialDb)))

(defn update-filters-active [db]
  (assoc db :filters-active (not= {:organizer "all-organizers"
                                   :date-from nil
                                   :date-to   nil
                                   :players   [0 (:max-players db)]}
                                  (:tournament-filter db))))

(reg-event-db ::initialize
  (fn [db _]
    (deep-merge db (initial-db))))

(defn connect! []
  (ws/send! [:client/connect-pairings])
  (when-let [user (local-storage/fetch :user)]
    (ws/send! [:client/login (:dci user)])))

(reg-event-fx ::login
  (fn [_ [_ dci-number]]
    {:ws-send [:client/login dci-number]}))

(reg-event-fx :server/login
  (fn [{:keys [db]} [_ user]]
    {:db    (if user
              (assoc db :logged-in-user user)
              (assoc db :notification "DCI-numeroa ei löydy"
                     :logged-in-user nil))
     :store [:user user]}))

(reg-event-fx ::logout
  (fn [{:keys [db]} _]
    {:ws-send [:client/logout]
     :db      (assoc db :logged-in-user nil)
     :store   [:user nil]}))

(reg-event-db ::tournaments-page
  (fn [db [_ page]]
    (assoc db :tournaments-page page)))

(reg-event-db ::tournament-filter
  (fn [db [_ [key value]]]
    (-> db
        (assoc-in [:tournament-filter key] value)
        (assoc :tournaments-page 0)
        (update-filters-active))))

(reg-event-db ::reset-tournament-filter
  (fn [db _]
    (assoc db
           :tournaments-page 0
           :tournament-filter {:organizer "all-organizers"
                               :date-from nil
                               :date-to   nil
                               :players   [0 (:max-players db)]}
           :filters-active false)))

(reg-event-db :server/tournaments
  (fn [db [_ tournaments]]
    (let [max-players (round-up (transduce (map :players) max 0 tournaments) 10)]
      (-> db
          (assoc :tournaments (map-by :id tournaments)
                 :tournament-ids (map :id tournaments)
                 :max-players max-players)
          (assoc-in [:tournament-filter :players 1] max-players)))))

(reg-event-db :server/tournament
  (fn [db [_ tournament]]
    (assoc-in db [:tournaments (:id tournament)] tournament)))

(reg-event-fx ::load-tournament
  (fn [_ [_ id]]
    {:ws-send [:client/tournament id]}))

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

(reg-event-db ::notification
  (fn [db [_ notification]]
    (assoc db :notification notification)))

(reg-event-db :server/organizer-tournament
  (fn [db [_ tournament]]
    (cond-> db
      (not= (seq (:pairings tournament)) (get-in db [:organizer :tournament :pairings]))
      (assoc-in-many [:organizer :new-pairings] true
                     [:organizer :selected-pairings] (str (last (:pairings tournament))))

      (not= (seq (:standings tournament)) (get-in db [:organizer :tournament :standings]))
      (assoc-in-many [:organizer :new-standings] true
                     [:organizer :selected-standings] (str (last (:standings tournament))))

      (not= (seq (:pods tournament)) (get-in db [:organizer :tournament :pods]))
      (assoc-in-many [:organizer :new-pods] true
                     [:organizer :selected-pods] (str (last (:pods tournament))))

      (not= (:seatings tournament) (boolean (get-in db [:organizer :tournament :seatings])))
      (assoc-in [:organizer :new-seatings] true)

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

(defonce ^:private clock-interval (atom nil))

(reg-fx :clock
  (fn [action]
    (case action
      :start (reset! clock-interval (js/setInterval #(dispatch [::update-clock]) 200))
      :stop (swap! clock-interval #(js/clearInterval %)))))

(defn resolve-organizer-action [db id action value]
  (case action
    :pairings {:ws-send [:client/organizer-pairings [id value]]
               :db      (assoc-in-many db
                                       [:organizer :mode] :pairings
                                       [:organizer :pairings-round] value
                                       [:organizer :new-pairings] false)}
    :standings {:ws-send [:client/organizer-standings [id value]]
                :db      (assoc-in-many db
                                        [:organizer :mode] :standings
                                        [:organizer :standings-round] value
                                        [:organizer :new-standings] false)}
    :seatings {:ws-send [:client/organizer-seatings id]
               :db      (assoc-in-many db
                                       [:organizer :mode] :seatings
                                       [:organizer :new-seatings] false)}
    :pods {:ws-send [:client/organizer-pods [id value]]
           :db      (assoc-in-many db
                                   [:organizer :mode] :pods
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
    :select-pods {:db (assoc-in db [:organizer :selected-pods] value)}
    :close-popup {:db (assoc-in db [:organizer :menu] false)}))

(defn send-organizer-action [db id action value]
  (assoc
   (case action
     :start-clock {:db (assoc-in db [:organizer :clock :running] true)}
     :stop-clock {:db (assoc-in db [:organizer :clock :running] false)}
     :select-pairings (resolve-organizer-action db id action value)
     :select-standings (resolve-organizer-action db id action value)
     :select-pods (resolve-organizer-action db id action value)
     {})
   :store [["organizer" id] {:action action, :value value}]))

(reg-event-fx ::organizer-mode
  (fn [{:keys [db]} [_ action value]]
    (let [{:keys [id page]} (:page db)]
      (case page
        :mtg-pairings-server.pages.organizer/main (resolve-organizer-action db id action value)
        :mtg-pairings-server.pages.organizer/menu (send-organizer-action db id action value)))))

(reg-fx :popup
  (fn [id]
    (.open js/window (str "/tournaments/" id "/organizer/menu") (str "menu" id))))

(reg-fx :close-popup
  (fn []
    (js/setTimeout #(.close js/window) 100)))

(reg-event-fx ::popup-organizer-menu
  (fn [{:keys [db]} _]
    (let [{:keys [id page]} (:page db)]
      (case page
        :mtg-pairings-server.pages.organizer/main {:db    (assoc-in db [:organizer :menu] true)
                                                   :popup (get-in db [:page :id])}
        :mtg-pairings-server.pages.organizer/menu {:store       [["organizer" id] {:action :close-popup}]
                                                   :close-popup nil}))))

(reg-event-fx ::local-storage-updated
  (fn [{:keys [db]} [_ k v]]
    (when (and (= (get-in db [:page :page]) :mtg-pairings-server.pages.organizer/main)
               (= k ["organizer" (get-in db [:page :id])]))
      (resolve-organizer-action db
                                (get-in db [:page :id])
                                (keyword (:action v))
                                (:value v)))))

(reg-event-fx ::load-deck-construction
  (fn [_ [_ id]]
    {:ws-send [:client/deck-construction id]}))
