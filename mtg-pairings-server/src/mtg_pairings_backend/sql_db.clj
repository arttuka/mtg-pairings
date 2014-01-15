(ns mtg-pairings-backend.sql-db
  (:require [korma.core :as sql]
            korma.db
            [clojure.walk :refer [postwalk]]
            [clj-time.core :as time]
            [clj-time.coerce :as time-coerce]))

(defn ^:private create-korma-db
  [db-properties]
  (korma.db/default-connection
    (korma.db/create-db
      (korma.db/postgres db-properties))))

(defn ^:private convert-instances-of
  [cls f m]
  (postwalk (fn [x]
              (if (instance? cls x)
                (f x)
                x))
            m))

(defn ^:private to-local-date-default-tz
  [date]
  (let [dt (time-coerce/to-date-time date)]
    (time-coerce/to-local-date (time/to-time-zone dt (time/default-time-zone)))))

(def sql-date->joda-date
  (partial convert-instances-of java.sql.Date to-local-date-default-tz))

(def joda-date->sql-date
  (partial convert-instances-of org.joda.time.LocalDate time-coerce/to-sql-date))

(declare tournament player team round pairing result standings)

(sql/defentity tournament
  (sql/pk :id)
  (sql/has-many team
    {:fk :tournament})
  (sql/has-many round
    {:fk :tournament})
  (sql/has-one standings
    {:fk :tournament})
  (sql/prepare joda-date->sql-date)
  (sql/transform sql-date->joda-date))

(sql/defentity player
  (sql/pk :dci)
  (sql/many-to-many team
    :team_players
    {:lfk :player, :rfk :team}))

(sql/defentity team
  (sql/pk :id)
  (sql/belongs-to tournament
    {:fk :tournament})
  (sql/many-to-many player
    :team_players
    {:lfk :team, :rfk :player}))

(sql/defentity round
  (sql/pk :id)
  (sql/belongs-to tournament
    {:fk :tournament})
  (sql/has-many pairing
    {:fk :round}))

(sql/defentity team1
  (sql/pk :id)
  (sql/table :team))

(sql/defentity team2
  (sql/pk :id)
  (sql/table :team))

(sql/defentity pairing
  (sql/pk :id)
  (sql/belongs-to team1
    {:fk :team1})
  (sql/belongs-to team2
    {:fk :team2})
  (sql/belongs-to round
    {:fk :round})
  (sql/has-one result
    {:fk :pairing}))

(sql/defentity result
  (sql/pk :pairing)
  (sql/belongs-to pairing
    {:fk :pairing}))

(sql/defentity standings
  (sql/pk :tournament)
  (sql/belongs-to tournament
    {:fk :tournament}))

(defrecord ^:private SqlDb []
  mtg-pairings-backend.db/DB
  (tournament [this id]
    nil)
  (player [this dci]
    nil)
  (add-tournament [this tournament]
    nil)
  (add-teams [this tournament-id teams]
    nil)
  (add-pairings [this tournament-id round-num pairings]
    nil)
  (add-results [this tournament-id round-num results]
    nil))

(defn create-db [db-properties]
  (create-korma-db db-properties)
  (->SqlDb))
