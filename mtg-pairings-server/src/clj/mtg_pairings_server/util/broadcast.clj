(ns mtg-pairings-server.util.broadcast
  (:require [korma.core :as sql]
            [mtg-pairings-server.service.player :refer [format-tournament]]
            [mtg-pairings-server.service.tournament :as tournament]
            [mtg-pairings-server.sql-db :as db]
            [mtg-pairings-server.util.util :refer [extract-list]]
            [mtg-pairings-server.websocket :as ws]))

(defonce dci->uid (atom {}))
(defonce uid->dci (atom {}))
(defonce id->uid (atom {}))
(defonce uid->id (atom {}))

(defn login [uid dci-number]
  (swap! dci->uid update dci-number (fnil conj #{}) uid)
  (swap! uid->dci assoc uid dci-number))

(defn logout [uid]
  (when-let [dci-number (get @uid->dci uid)]
    (swap! dci->uid update dci-number disj uid)
    (swap! uid->dci dissoc uid)))

(defn disconnect [uid]
  (logout [uid])
  (when-let [id (get @uid->id uid)]
    (swap! id->uid update id disj uid)
    (swap! uid->id dissoc uid)))

(defn watch [uid tournament-id]
  (when-let [old-id (get @uid->id uid)]
    (swap! id->uid update old-id disj uid))
  (swap! id->uid update tournament-id (fnil conj #{}) uid)
  (swap! uid->id assoc uid tournament-id))

(defn broadcast-tournament [sanctionid]
  (let [tournament (->> (sql/select db/tournament
                          (sql/join :inner db/team {:tournament.id :team.tournament})
                          (sql/join :inner db/team-players {:team.id :team_players.team})
                          (sql/fields :tournament.id :tournament.name :tournament.organizer
                                      :tournament.day :tournament.rounds :team_players.player)
                          (sql/where {:sanctionid sanctionid}))
                        (extract-list :player))]
    (doseq [p (:player tournament)
            :when (contains? @dci->uid p)
            :let [t (-> tournament
                        (dissoc :player)
                        (format-tournament p))]
            uid (@dci->uid p)]
      (ws/send! uid [:server/player-tournament t]))
    (let [organizer-tournament (tournament/tournament (:id tournament))]
      (doseq [uid (get @id->uid (:id tournament))]
        (ws/send! uid [:server/organizer-tournament organizer-tournament])))))
