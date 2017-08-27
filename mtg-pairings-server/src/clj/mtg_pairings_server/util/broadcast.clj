(ns mtg-pairings-server.util.broadcast
  (:require [korma.core :as sql]
            [mtg-pairings-server.service.player :refer [format-tournament]]
            [mtg-pairings-server.sql-db :as db]
            [mtg-pairings-server.util.util :refer [extract-list]]
            [mtg-pairings-server.websocket :as ws]))

(defonce dci->uid (atom {}))
(defonce uid->dci (atom {}))

(defn login [uid dci-number]
  (swap! dci->uid assoc dci-number uid)
  (swap! uid->dci assoc uid dci-number))

(defn logout [uid]
  (let [dci-number (get @uid->dci uid)]
    (swap! dci->uid dissoc dci-number)
    (swap! uid->dci dissoc uid)))

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
                        (format-tournament p))
                  uid (@dci->uid p)]]
      (ws/send! uid [:server/player-tournament t]))))
