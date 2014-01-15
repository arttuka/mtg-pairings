(ns mtg-pairings-server.db)

(defprotocol DB
  (tournament [this id]
    "Returns the tournament with given id")
  (player [this dci]
    "Returns the tournament with given DCI number")
  (add-tournament [this tournament]
    "Adds a tournament to the database. Returns ID of the tournament")
  (add-teams [this tournament-id teams]
    "Adds teams to given tournament")
  (add-pairings [this tournament-id round-num pairings]
    "Adds pairings of given round to given tournament")
  (add-results [this tournament-id round-num results]
    "Adds results of given round to given tournament"))
