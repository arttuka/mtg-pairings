(ns mtg-pairings-server.util.broadcast
  (:require [korma.core :as sql]
            [mtg-pairings-server.service.player :refer [format-tournament]]
            [mtg-pairings-server.service.tournament :as tournament]
            [mtg-pairings-server.sql-db :as db]
            [mtg-pairings-server.util.mtg :refer [add-check-digits]]
            [mtg-pairings-server.util :refer [dissoc-in extract-list]]
            [mtg-pairings-server.websocket :as ws]))

(defonce mapping (atom {:dci->uid {}
                        :uid->dci {}
                        :id->uid  {}
                        :uid->id  {}}))

(defn ^:private remove-uid [key->uid key uid]
  (if (= #{uid} (get key->uid key))
    (dissoc key->uid key)
    (update key->uid key disj uid)))

(defn ^:private login* [m uid dci-number]
  (-> m
      (update-in [:dci->uid dci-number] (fnil conj #{}) uid)
      (assoc-in [:uid->dci uid] dci-number)))

(defn ^:private logout* [m uid]
  (if-let [dci-number (get-in m [:uid->dci uid])]
    (-> m
        (update :dci->uid remove-uid dci-number uid)
        (dissoc-in [:uid->dci uid]))
    m))

(defn ^:private disconnect* [m uid]
  (if-let [id (get-in m [:uid->id uid])]
    (-> m
        (logout* uid)
        (update :id->uid remove-uid id uid)
        (dissoc-in [:uid->id uid]))
    (logout* m uid)))

(defn ^:private watch* [m uid id]
  (let [m (if-let [old-id (get-in m [:uid->id uid])]
            (update m :id->uid remove-uid old-id uid)
            m)]
    (-> m
        (update-in [:id->uid id] (fnil conj #{}) uid)
        (assoc-in [:uid->id uid] id))))

(defn login [uid dci-number]
  (swap! mapping login* uid (add-check-digits dci-number)))

(defn logout [uid]
  (swap! mapping logout* uid))

(defn disconnect [uid]
  (swap! mapping disconnect* uid))

(defn watch [uid tournament-id]
  (swap! mapping watch* uid tournament-id))

(defn broadcast-tournament [sanctionid to-clients?]
  (let [tournament (->> (sql/select db/tournament
                          (sql/join :inner db/team {:tournament.id :team.tournament})
                          (sql/join :inner db/team-players {:team.id :team_players.team})
                          (sql/fields :tournament.id :tournament.name :tournament.organizer
                                      :tournament.day :tournament.rounds :team_players.player)
                          (sql/where {:sanctionid sanctionid}))
                        (extract-list :player)
                        first)
        {:keys [dci->uid id->uid]} @mapping]
    (when to-clients?
      (doseq [p (:player tournament)
              :when (contains? dci->uid p)
              :let [t (-> tournament
                          (dissoc :player)
                          (format-tournament p))]
              uid (dci->uid p)]
        (ws/send! uid [:server/player-tournament t])))
    (let [organizer-tournament (tournament/tournament (:id tournament))]
      (doseq [uid (get id->uid (:id tournament))]
        (ws/send! uid [:server/organizer-tournament organizer-tournament])))))
