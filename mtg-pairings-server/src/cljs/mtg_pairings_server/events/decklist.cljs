(ns mtg-pairings-server.events.decklist
  (:require [re-frame.core :refer [dispatch reg-fx reg-event-db reg-event-fx]]
            [oops.core :refer [oget]]
            [mtg-pairings-server.routes.decklist :as routes]
            [mtg-pairings-server.transit :as transit]
            [mtg-pairings-server.util :as util]
            [mtg-pairings-server.util.decklist :refer [add-id-to-card add-id-to-cards]]
            [mtg-pairings-server.util.mobile :refer [mobile?]]
            [mtg-pairings-server.websocket :as ws]))

(def empty-decklist {:main   []
                     :side   []
                     :count  {:main 0
                              :side 0}
                     :board  :main
                     :player {:dci        ""
                              :first-name ""
                              :last-name  ""
                              :deck-name  ""
                              :email      ""}})

(defn initial-db []
  (util/deep-merge {:decklist-editor {:organizer-tournaments []
                                      :decklist              empty-decklist}}
                   (transit/read (oget js/window "initial_db"))))

(reg-event-db ::initialize
  (fn [db _]
    (merge db (initial-db))))

(defn connect! []
  (ws/send! [:client/connect-decklist]))

(reg-event-db :server/decklist-error
  (fn [db _]
    (update db :decklist-editor merge {:error  true
                                       :saving false
                                       :saved  false})))

(reg-event-fx ::save-decklist
  (fn [{:keys [db]} [_ tournament decklist]]
    {:db      (update db :decklist-editor merge {:decklist decklist
                                                 :saving   true})
     :ws-send [:client/save-decklist [tournament decklist]]}))

(reg-event-fx :server/decklist-saved
  (fn [{:keys [db]} [_ id]]
    {:db       (update db :decklist-editor merge {:saved  true
                                                  :saving false
                                                  :error  false})
     :navigate (routes/old-decklist-path {:id id})}))

(reg-event-fx ::load-organizer-tournament
  (fn [_ [_ id]]
    {:ws-send [:client/decklist-organizer-tournament id]}))

(reg-event-db :server/decklist-organizer-tournament
  (fn [db [_ tournament]]
    (assoc-in db [:decklist-editor :organizer-tournament] tournament)))

(reg-event-fx ::save-tournament
  (fn [{:keys [db]} [_ tournament]]
    {:db      (update db :decklist-editor merge {:saving               true
                                                 :organizer-tournament tournament})
     :ws-send [:client/save-decklist-organizer-tournament tournament]}))

(reg-event-fx :server/organizer-tournament-saved
  (fn [{:keys [db]} [_ id]]
    {:db       (util/assoc-in-many db
                                   [:decklist-editor :organizer-tournament :id] id
                                   [:decklist-editor :saved] true
                                   [:decklist-editor :saving] false
                                   [:decklist-editor :error] false)
     :navigate (routes/organizer-tournament-path {:id id})}))

(reg-event-fx ::load-decklist
  (fn [_ [_ id]]
    {:ws-send [:client/load-decklist id]}))

(reg-event-fx ::load-decklists
  (fn [_ [_ id]]
    {:ws-send [:client/load-decklists id]}))

(reg-event-db :server/decklist
  (fn [db [_ decklist]]
    (assoc-in db [:decklist-editor :decklist] (add-id-to-cards (merge empty-decklist decklist)))))

(reg-event-db :server/decklists
  (fn [db [_ decklists]]
    (assoc-in db [:decklist-editor :decklists] decklists)))

(reg-event-db :server/organizer-login
  (fn [db [_ username]]
    (assoc-in db [:decklist-editor :user] username)))

(reg-event-db ::clear-tournament
  (fn [db _]
    (update db :decklist-editor merge {:organizer-tournament nil
                                       :saving               false
                                       :saved                false
                                       :error                false})))

(reg-event-db ::clear-status
  (fn [db [_ key]]
    (assoc-in db [:decklist-editor key] false)))

(reg-event-fx ::import-address
  (fn [_ [_ address]]
    (when-let [[_ code] (re-find #"/decklist/([A-z0-9_-]{22})$" address)]
      {:ws-send [:client/load-decklist-with-id code]})))

(defn ^:private add-card [{:keys [board] :as decklist} name]
  (if (some #(= name (:name %)) (get decklist board))
    decklist
    (-> decklist
        (update board conj (add-id-to-card {:name name, :quantity 1}))
        (update-in [:count board] inc))))

(reg-event-db ::add-card
  (fn [db [_ name]]
    (update-in db [:decklist-editor :decklist] add-card name)))

(defn ^:private set-quantity [decklist board id quantity]
  (let [index (util/index-where #(= id (:id %)) (get decklist board))
        orig-quantity (get-in decklist [board index :quantity])]
    (-> decklist
        (assoc-in [board index :quantity] quantity)
        (update-in [:count board] + (- quantity orig-quantity)))))

(reg-event-db ::set-quantity
  (fn [db [_ board id quantity]]
    (update-in db [:decklist-editor :decklist] set-quantity board id quantity)))

(defn ^:private remove-card [decklist board id]
  (let [cards (get decklist board)
        index (util/index-where #(= id (:id %)) cards)
        n (get-in decklist [board index :quantity])]
    (-> decklist
        (assoc board (util/dissoc-index cards index))
        (update-in [:count board] - n))))

(reg-event-db ::remove-card
  (fn [db [_ board id]]
    (update-in db [:decklist-editor :decklist] remove-card board id)))

(reg-event-db ::select-board
  (fn [db [_ board]]
    (assoc-in db [:decklist-editor :decklist :board] board)))

(reg-event-db ::update-player-info
  (fn [db [_ key value]]
    (assoc-in db [:decklist-editor :decklist :player key] value)))

(reg-event-fx ::card-suggestions
  (fn [_ [_ prefix format callback]]
    {:ws-send [[:client/decklist-card-suggestions [prefix format]] 1000 callback]}))

(reg-event-fx ::import-text
  (fn [{:keys [db]} [_ text-decklist]]
    {:ws-send [:client/load-text-decklist [text-decklist (get-in db [:decklist-editor :tournament :format])]]}))
