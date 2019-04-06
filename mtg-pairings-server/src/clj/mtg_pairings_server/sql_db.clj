(ns mtg-pairings-server.sql-db
  (:require [clojure.walk :refer [postwalk]]
            [clj-time.core :as time]
            [clj-time.coerce :as time-coerce]
            [korma.core :as sql]
            [korma.db]
            [hikari-cp.core :as hikari]
            [mount.core :refer [defstate]]
            [config.core :refer [env]]
            [mtg-pairings-server.util :refer [some-value]])
  (:import (java.sql Date Timestamp)
           (org.joda.time LocalDate DateTime)))

(def datasource-options {:adapter       "postgresql"
                         :username      (env :db-user)
                         :password      (env :db-password)
                         :database-name (env :db-name)
                         :server-name   (env :db-host)
                         :port-number   (env :db-port)})

(defstate ^{:on-reload :noop} db
  :start (korma.db/default-connection (korma.db/create-db
                                       {:make-pool? false
                                        :datasource (hikari/make-datasource datasource-options)}))
  :stop (do
          (korma.db/default-connection nil)
          (hikari/close-datasource (get-in @db [:pool :datasource]))))

(defn ^:private convert [conversions x]
  (if-let [[_ converter] (some-value (fn [[cls _]]
                                       (instance? cls x))
                                     conversions)]
    (converter x)
    x))

(defn ^:private convert-instances-of
  [conversions m]
  (postwalk (partial convert conversions) m))

(defn ^:private to-local-date-default-tz
  [date]
  (let [dt (time-coerce/to-date-time date)]
    (time-coerce/to-local-date (time/to-time-zone dt (time/default-time-zone)))))

(def sql-date->joda-date
  (partial convert-instances-of {Date      to-local-date-default-tz
                                 Timestamp time-coerce/to-date-time}))

(def joda-date->sql-date
  (partial convert-instances-of {LocalDate time-coerce/to-sql-date
                                 DateTime  time-coerce/to-sql-time}))

(declare tournament player team round pairing result standings team-players seating user pod-round
         decklist decklist-card)

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

(sql/defentity card
  (sql/table :trader_card)
  (sql/pk :id))

(sql/defentity decklist-tournament
  (sql/table :decklist_tournament)
  (sql/pk :id)
  (sql/has-many decklist
    {:fk :tournament})
  (sql/prepare joda-date->sql-date)
  (sql/transform sql-date->joda-date)
  (sql/transform #(update % :format keyword)))

(sql/defentity decklist
  (sql/pk :id)
  (sql/belongs-to decklist-tournament
    {:fk :tournament})
  (sql/has-many decklist-card
    {:fk :decklist})
  (sql/prepare joda-date->sql-date)
  (sql/transform sql-date->joda-date))

(sql/defentity decklist-card
  (sql/table :decklist_card)
  (sql/belongs-to decklist
    {:fk :decklist})
  (sql/belongs-to card
    {:fk :card}))

(sql/defentity smf-user
  (sql/table :smf_members)
  (sql/pk :id_member))
