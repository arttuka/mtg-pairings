(ns mtg-pairings-server.events.pairings
  (:require [re-frame.core :refer [dispatch reg-fx reg-event-db reg-event-fx]]
            [cljs-time.core :as time]
            [mtg-pairings-server.i18n.common :as i18n]
            [mtg-pairings-server.transit :as transit]
            [mtg-pairings-server.util :refer [map-by format-time deep-merge round-up dissoc-index]]
            [mtg-pairings-server.util.styles :refer [mobile?]]))

(defn initial-db []
  (deep-merge {:tournaments           {}
               :tournament-count      0
               :tournaments-page      0
               :tournament-ids        []
               :active-tournament-ids []
               :tournaments-modified  nil
               :tournament-filter     {:organizer "all-organizers"
                                       :date-from nil
                                       :date-to   nil
                                       :players   [0 100]}
               :filters-active        false
               :max-players           100
               :player-tournaments    []
               :pairings              {:sort-key :table_number}
               :pods                  {:sort-key :pod}
               :seatings              {:sort-key :table_number}
               :page                  {:page  nil
                                       :id    nil
                                       :round nil}
               :logged-in-user        nil
               :notification          nil
               :language              (i18n/language :fi)
               :mobile?               (mobile?)
               :window-size           [(.-innerWidth js/window)
                                       (.-innerHeight js/window)]
               :organizer             {:clock [{:time    (* 50 60)
                                                :text    (format-time (* 50 60))
                                                :timeout false
                                                :id      (gensym "clock")}]}}
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

(reg-event-fx ::connect
  (fn [{:keys [db]} _]
    {:ws-send [:client/connect-pairings (get-in db [:logged-in-user :dci])]}))

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
  (fn [db [_ {:keys [modified tournaments]}]]
    (let [old-ids (set (:tournament-ids db))
          new-ids (filterv (complement old-ids) (map :id tournaments))
          max-players (round-up (transduce (map :players) max 0 tournaments) 10)
          new-max-players (max (:max-players db 0) max-players)]
      (-> db
          (update :tournaments into (map-by :id) tournaments)
          (cond->
            (seq new-ids) (update :tournament-ids (partial into new-ids))
            modified (assoc :tournaments-modified modified))
          (assoc :max-players new-max-players)
          (assoc-in [:tournament-filter :players 1] new-max-players)))))

(reg-event-db :server/active-tournaments
  (fn [db [_ tournaments]]
    (-> db
        (update :tournaments into (map-by :id) tournaments)
        (assoc :active-tournament-ids (map :id tournaments)))))

(reg-event-db :server/tournament
  (fn [db [_ tournament]]
    (assoc-in db [:tournaments (:id tournament)] tournament)))

(reg-event-fx ::load-tournaments
  (fn [{:keys [db]} _]
    {:ws-send [:client/tournaments (:tournaments-modified db)]}))

(reg-event-db :server/pairings
  (fn [db [_ [id round pairings]]]
    (assoc-in db [:pairings id round] pairings)))

(reg-event-db ::sort-pairings
  (fn [db [_ sort-key]]
    (assoc-in db [:pairings :sort-key] sort-key)))

(reg-event-db :server/standings
  (fn [db [_ [id round standings]]]
    (assoc-in db [:standings id round] standings)))

(reg-event-db :server/pods
  (fn [db [_ [id round pods]]]
    (assoc-in db [:pods id round] pods)))

(reg-event-db ::sort-pods
  (fn [db [_ sort-key]]
    (assoc-in db [:pods :sort-key] sort-key)))

(reg-event-db :server/seatings
  (fn [db [_ [id seatings]]]
    (assoc-in db [:seatings id] seatings)))

(reg-event-db ::sort-seatings
  (fn [db [_ sort-key]]
    (assoc-in db [:seatings :sort-key] sort-key)))

(reg-event-db :server/bracket
  (fn [db [_ [id bracket]]]
    (assoc-in db [:bracket id] bracket)))

(reg-event-db :server/player-tournaments
  (fn [db [_ tournaments]]
    (assoc db :player-tournaments (vec tournaments))))

(reg-event-db :server/player-tournament
  (fn [db [_ tournament]]
    (assoc-in db [:player-tournaments 0] tournament)))

(reg-event-db ::notification
  (fn [db [_ notification]]
    (assoc db :notification notification)))

(reg-event-db :server/organizer-tournament
  (fn [db [_ tournament]]
    (cond-> db
      (not= (seq (:pairings tournament)) (get-in db [:organizer :tournament :pairings]))
      (update :organizer merge {:new-pairings      (boolean (seq (:pairings tournament)))
                                :selected-pairings (str (last (:pairings tournament)))})

      (not= (seq (:standings tournament)) (get-in db [:organizer :tournament :standings]))
      (update :organizer merge {:new-standings      (boolean (seq (:standings tournament)))
                                :selected-standings (str (last (:standings tournament)))})

      (not= (seq (:pods tournament)) (get-in db [:organizer :tournament :pods]))
      (update :organizer merge {:new-pods      (boolean (seq (:pods tournament)))
                                :selected-pods (str (last (:pods tournament)))})

      (not= (:seatings tournament) (boolean (get-in db [:organizer :tournament :seatings])))
      (assoc-in [:organizer :new-seatings] (:seatings tournament))

      :always
      (assoc-in [:organizer :tournament] tournament))))

(reg-event-db :server/organizer-tournaments
  (fn [db [_ tournaments]]
    (assoc-in db [:organizer :tournaments] tournaments)))

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

(defn update-clock [clock]
  (if-not (:running clock)
    clock
    (let [now (time/now)
          diff (/ (time/in-millis (time/interval (:start clock) now)) 1000)
          total (- (:time clock) diff)]
      (merge clock {:text    (format-time total)
                    :timeout (neg? total)
                    :time    total
                    :start   now}))))

(defn update-clocks [clocks]
  (mapv update-clock clocks))

(reg-event-db ::update-clocks
  (fn [db _]
    (update-in db [:organizer :clock] update-clocks)))

(defn resolve-organizer-action [db id action value]
  (let [selected-clock (or (get-in db [:organizer :selected-clock])
                           (when (= 1 (count (get-in db [:organizer :clock])))
                             0))]
    (case action
      :clear {:db (update db :organizer merge {:mode           nil
                                               :selected-clock nil})}
      :load-tournament {:ws-send [:client/organizer-tournament id]}
      :pairings {:ws-send [:client/organizer-pairings [id value]]
                 :db      (update db :organizer merge {:mode           :pairings
                                                       :pairings       nil
                                                       :pairings-round value
                                                       :new-pairings   false
                                                       :selected-clock nil})}
      :standings {:ws-send [:client/organizer-standings [id value]]
                  :db      (update db :organizer merge {:mode            :standings
                                                        :standings       nil
                                                        :standings-round value
                                                        :new-standings   false
                                                        :selected-clock  nil})}
      :seatings {:ws-send [:client/organizer-seatings id]
                 :db      (update db :organizer merge {:mode           :seatings
                                                       :seatings       nil
                                                       :new-seatings   false
                                                       :selected-clock nil})}
      :pods {:ws-send [:client/organizer-pods [id value]]
             :db      (update db :organizer merge {:mode           :pods
                                                   :pods           nil
                                                   :new-pods       false
                                                   :selected-clock nil})}
      :clock {:db (update db :organizer #(-> %
                                             (assoc :mode :clock)
                                             (update :clock update-clocks)))}
      :set-clock {:db (update-in db [:organizer :clock selected-clock] merge {:time    (* value 60)
                                                                              :text    (format-time (* value 60))
                                                                              :timeout false})}
      :start-clock {:db (update-in db [:organizer :clock selected-clock] merge {:start   (time/now)
                                                                                :running true})}
      :stop-clock {:db (assoc-in db [:organizer :clock selected-clock :running] false)}
      :add-clock {:db (update db :organizer #(-> %
                                                 (update :clock conj {:time    (* value 60)
                                                                      :text    (format-time (* value 60))
                                                                      :timeout false
                                                                      :id      (gensym "clock")})
                                                 (assoc :mode :clock)))}
      :rename-clock {:db (assoc-in db [:organizer :clock selected-clock :name] value)}
      :remove-clock {:db (update db :organizer #(-> %
                                                    (update :clock dissoc-index selected-clock)
                                                    (assoc :selected-clock nil)))}
      :select-clock {:db (assoc-in db [:organizer :selected-clock] value)}
      :select-pairings {:db (assoc-in db [:organizer :selected-pairings] value)}
      :select-standings {:db (assoc-in db [:organizer :selected-standings] value)}
      :select-pods {:db (assoc-in db [:organizer :selected-pods] value)}
      :close-popup {:db (assoc-in db [:organizer :menu] false)})))

(defn send-organizer-action [db id action value]
  (assoc
   (if (contains? #{:start-clock :stop-clock :set-clock :add-clock
                    :rename-clock :remove-clock :select-clock
                    :select-pairings :select-standings :select-pods}
                  action)
     (resolve-organizer-action db id action value)
     {})
   :store [["organizer"] {:action action, :value value, :id id, :key (gensym)}]))

(reg-event-fx ::load-organizer-tournament
  (fn [{:keys [db]} [_ id]]
    (let [{:keys [page]} (:page db)]
      (cond-> {:ws-send [:client/organizer-tournament id]}
        (= page :mtg-pairings-server.pages.organizer/menu)
        (assoc :store [["organizer"] {:action :load-tournament, :id id}])))))

(reg-event-fx ::organizer-mode
  (fn [{:keys [db]} [_ action value]]
    (let [{:keys [id page]} (:page db)
          id (or id (get-in db [:organizer :tournament :id]))]
      (case page
        :mtg-pairings-server.pages.organizer/main (resolve-organizer-action db id action value)
        :mtg-pairings-server.pages.organizer/menu (send-organizer-action db id action value)))))

(reg-fx :popup
  (fn [id]
    (.open js/window (str "/tournaments" (when id (str "/" id)) "/organizer/menu") (str "menu" id))))

(reg-fx :close-popup
  (fn []
    (js/setTimeout #(.close js/window) 100)))

(reg-event-fx ::popup-organizer-menu
  (fn [{:keys [db]} _]
    (let [{:keys [id page]} (:page db)]
      (case page
        :mtg-pairings-server.pages.organizer/main {:db    (assoc-in db [:organizer :menu] true)
                                                   :popup id}
        :mtg-pairings-server.pages.organizer/menu {:store       [["organizer"] {:action :close-popup}]
                                                   :close-popup nil}))))

(reg-event-fx ::local-storage-updated
  (fn [{:keys [db]} [_ k v]]
    (when (and (= (get-in db [:page :page]) :mtg-pairings-server.pages.organizer/main)
               (= k ["organizer"]))
      (resolve-organizer-action db
                                (:id v)
                                (keyword (:action v))
                                (:value v)))))
