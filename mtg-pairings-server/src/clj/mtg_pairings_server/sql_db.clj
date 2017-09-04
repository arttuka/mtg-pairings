(ns mtg-pairings-server.sql-db
  (:require [clojure.walk :refer [postwalk]]
            [clj-time.core :as time]
            [clj-time.coerce :as time-coerce]
            [korma.core :as sql]
            [korma.db]
            [mount.core :as m]
            [mtg-pairings-server.properties :refer [properties]]))

(m/defstate db
  :start (korma.db/default-connection
           (korma.db/create-db
             (korma.db/postgres (:db properties)))))

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

(declare tournament player team round pairing result standings team-players seating user pod-round)

(sql/defentity team-players
  (sql/table :team_players)
  (sql/belongs-to team
    {:fk :team}))

(sql/defentity tournament
  (sql/pk :id)
  (sql/has-many team
    {:fk :tournament})
  (sql/has-many round
    {:fk :tournament})
  (sql/has-many standings
    {:fk :tournament})
  (sql/has-many seating
    {:fk :tournament})
  (sql/has-many pod-round
    {:fk :tournament})
  (sql/belongs-to user
    {:fk :owner})
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

(sql/defentity seating
  (sql/pk :id)
  (sql/belongs-to tournament
    {:fk :tournament})
  (sql/belongs-to team
    {:fk :team}))

(sql/defentity round
  (sql/pk :id)
  (sql/belongs-to tournament
    {:fk :tournament})
  (sql/has-many pairing
    {:fk :round}))

(sql/defentity team1
  (sql/pk :id)
  (sql/table :team :team1))

(sql/defentity team2
  (sql/pk :id)
  (sql/table :team :team2))

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

(sql/defentity user
  (sql/table :trader_user)
  (sql/pk :id)
  (sql/has-many tournament
    {:fk :owner}))

(sql/defentity pod-round
  (sql/table :pod_round)
  (sql/pk :id)
  (sql/belongs-to tournament
    {:fk :tournament})
  (sql/belongs-to round
    {:fk :round}))

(sql/defentity pod
  (sql/table :pod)
  (sql/pk :id)
  (sql/belongs-to pod-round
    {:fk :pod_round}))

(sql/defentity seat
  (sql/table :pod_seat)
  (sql/pk :id)
  (sql/belongs-to pod
    {:fk :pod})
  (sql/belongs-to team
    {:fk :team}))
