(ns mtg-pairings-server.events.decklist
  (:require [re-frame.core :refer [dispatch reg-fx reg-event-db reg-event-fx]]
            [oops.core :refer [oget]]
            [mtg-pairings-server.routes.decklist :as routes]
            [mtg-pairings-server.styles.common :refer [mobile?]]
            [mtg-pairings-server.transit :as transit]
            [mtg-pairings-server.util :as util]
            [mtg-pairings-server.util.decklist :refer [add-id-to-card add-id-to-cards]]
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
                                      :decklist              empty-decklist
                                      :sort                  {:key       :submitted
                                                              :ascending true}}}
                   (transit/read (oget js/window "initial_db"))))

(reg-event-db ::initialize
  (fn [db _]
    (util/deep-merge db (initial-db))))

(defn connect! []
  (ws/send! [:client/connect-decklist]))

(reg-event-db :server/decklist-error
  (fn [db _]
    (util/assoc-in-many db
                        [:decklist-editor :saving] false
                        [:decklist-editor :saved] false
                        [:decklist-editor :error :save-decklist] true)))

(reg-event-db :server/decklist-tournament-error
  (fn [db _]
    (util/assoc-in-many db
                        [:decklist-editor :saving] false
                        [:decklist-editor :saved] false
                        [:decklist-editor :error :save-tournament] true)))

(reg-event-fx ::save-decklist
  (fn [{:keys [db]} [_ tournament]]
    {:db      (assoc-in db [:decklist-editor :saving] true)
     :ws-send [:client/save-decklist [tournament (get-in db [:decklist-editor :decklist])]]}))

(reg-event-fx :server/decklist-saved
  (fn [{:keys [db]} [_ id]]
    {:db       (util/assoc-in-many db
                                   [:decklist-editor :saved] true
                                   [:decklist-editor :saving] false
                                   [:decklist-editor :error :save-decklist] false)
     :navigate (routes/old-decklist-path {:id id})}))

(reg-event-fx ::load-organizer-tournament
  (fn [{:keys [db]} [_ id]]
    (when (not= (get-in db [:decklist-editor :organizer-tournament :id]) id)
      {:ws-send [:client/decklist-organizer-tournament id]})))

(reg-event-db :server/decklist-organizer-tournament
  (fn [db [_ tournament]]
    (assoc-in db [:decklist-editor :organizer-tournament] tournament)))

(reg-event-fx ::load-organizer-tournaments
  (fn [{:keys [db]} _]
    (when (empty? (get-in db [:decklist-editor :organizer-tournaments]))
      {:ws-send [:client/decklist-organizer-tournaments]})))

(reg-event-db :server/decklist-organizer-tournaments
  (fn [db [_ tournaments]]
    (assoc-in db [:decklist-editor :organizer-tournaments] tournaments)))

(reg-event-fx ::save-tournament
  (fn [{:keys [db]} [_ tournament]]
    {:db      (-> db
                  (assoc-in [:decklist-editor :saving] true)
                  (update-in [:decklist-editor :organizer-tournament] merge tournament))
     :ws-send [:client/save-decklist-organizer-tournament tournament]}))

(reg-event-fx :server/organizer-tournament-saved
  (fn [{:keys [db]} [_ id]]
    {:db       (util/assoc-in-many db
                                   [:decklist-editor :organizer-tournament :id] id
                                   [:decklist-editor :saved] true
                                   [:decklist-editor :saving] false
                                   [:decklist-editor :error :save-tournament] false)
     :navigate (routes/organizer-tournament-path {:id id})}))

(reg-event-db ::sort-decklists
  (fn [db [_ sort-key]]
    (let [previous-key (get-in db [:decklist-editor :sort :key])]
      (if (= previous-key sort-key)
        (update-in db [:decklist-editor :sort :ascending] not)
        (assoc-in db [:decklist-editor :sort] {:key       sort-key
                                               :ascending true})))))

(reg-event-fx ::load-decklist
  (fn [{:keys [db]} [_ id]]
    {:db      (assoc-in db [:decklist-editor :decklist] empty-decklist)
     :ws-send [:client/load-decklist id]}))

(reg-event-fx ::load-decklists
  (fn [_ [_ id]]
    {:ws-send [:client/load-decklists id]}))

(reg-event-db :server/decklist
  (fn [db [_ decklist]]
    (util/assoc-in-many db
                        [:decklist-editor :decklist] (add-id-to-cards (merge empty-decklist decklist))
                        [:decklist-editor :loaded] true)))

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
                                       :error                {:save-decklist   false
                                                              :save-tournament false
                                                              :import-address  nil}})))

(reg-event-fx ::import-address
  (fn [{:keys [db]} [_ code]]
    {:db      (util/assoc-in-many db
                                  [:decklist-editor :error :import-address] nil
                                  [:decklist-editor :loaded] false)
     :ws-send [:client/load-decklist-with-id code]}))

(reg-event-db :server/decklist-load-error
  (fn [db [_ error]]
    (assoc-in db [:decklist-editor :error :import-address] error)))

(defn ^:private add-card [{:keys [board] :as decklist} card]
  (if (some #(= (:name card) (:name %)) (get decklist board))
    decklist
    (let [new-card (-> card
                       (update :types set)
                       (assoc :quantity 1)
                       (add-id-to-card))]
      (-> decklist
          (update board conj new-card)
          (update-in [:count board] inc)))))

(reg-event-db ::add-card
  (fn [db [_ card]]
    (update-in db [:decklist-editor :decklist] add-card card)))

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
