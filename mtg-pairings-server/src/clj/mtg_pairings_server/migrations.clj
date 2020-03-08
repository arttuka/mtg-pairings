(ns mtg-pairings-server.migrations
  (:require [config.core :refer [env]]
            [ragtime.jdbc :as jdbc]
            [ragtime.repl :as repl]))

(def config {:datastore (jdbc/sql-database {:dbtype "postgresql"
                                            :dbname (env :db-name)
                                            :host (env :db-host)
                                            :port (env :db-port)
                                            :user (env :db-user)
                                            :password (env :db-password)})
             :migrations (jdbc/load-resources "migrations")})

(defn migrate []
  (repl/migrate config))

(defn rollback [id]
  (repl/rollback config id))
