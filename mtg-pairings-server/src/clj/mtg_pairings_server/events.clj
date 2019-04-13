(ns mtg-pairings-server.events
  (:require [taoensso.timbre :as log]
            [mtg-pairings-server.service.decklist :as decklist]
            [mtg-pairings-server.service.email :as email]
            [mtg-pairings-server.service.player :as player]
            [mtg-pairings-server.service.tournament :as tournament]
            [mtg-pairings-server.util.broadcast :as broadcast]
            [mtg-pairings-server.websocket :as ws]))

(defmethod ws/event-handler
  :chsk/uidport-open
  [_])

(defmethod ws/event-handler :chsk/uidport-close
  [{:keys [uid]}]
  (broadcast/disconnect uid))

(defmethod ws/event-handler :client/connect-pairings
  [{:keys [uid]}]
  (log/debugf "New connection from %s" uid)
  (ws/send! uid [:server/tournaments (tournament/client-tournaments)]))

(defmethod ws/event-handler :client/connect-decklist
  [{:keys [uid ring-req]}]
  (ws/send! uid [:server/organizer-login (get-in ring-req [:session :identity :username] false)]))

(defmethod ws/event-handler :client/login
  [{uid :uid, dci-number :?data}]
  (try
    (if-let [player (player/player dci-number)]
      (do
        (ws/send! uid [:server/login player])
        (ws/send! uid [:server/player-tournaments (player/tournaments dci-number)])
        (broadcast/login uid dci-number))
      (ws/send! uid [:server/login nil]))
    (catch NumberFormatException _
      (ws/send! uid [:server/login nil]))))

(defmethod ws/event-handler :client/logout
  [{:keys [uid]}]
  (broadcast/logout uid))

(defmethod ws/event-handler :client/tournaments
  [{:keys [uid]}]
  (ws/send! uid [:server/tournaments (tournament/client-tournaments)]))

(defmethod ws/event-handler :client/tournament
  [{uid :uid, id :?data}]
  (ws/send! uid [:server/tournament (tournament/client-tournament id)]))

(defmethod ws/event-handler :client/pairings
  [{uid :uid, [id round] :?data}]
  (ws/send! uid [:server/pairings [id round (tournament/get-round id round)]]))

(defmethod ws/event-handler :client/standings
  [{uid :uid, [id round] :?data}]
  (ws/send! uid [:server/standings [id round (tournament/standings id round false)]]))

(defmethod ws/event-handler :client/pods
  [{uid :uid, [id round] :?data}]
  (ws/send! uid [:server/pods [id round (tournament/pods id round)]]))

(defmethod ws/event-handler :client/seatings
  [{uid :uid, id :?data}]
  (ws/send! uid [:server/seatings [id (tournament/seatings id)]]))

(defmethod ws/event-handler :client/bracket
  [{uid :uid, id :?data}]
  (ws/send! uid [:server/bracket [id (tournament/bracket id)]]))

(defmethod ws/event-handler :client/organizer-tournament
  [{uid :uid, id :?data}]
  (broadcast/watch uid id)
  (ws/send! uid [:server/organizer-tournament (tournament/tournament id)]))

(defmethod ws/event-handler :client/organizer-pairings
  [{uid :uid, [id round] :?data}]
  (ws/send! uid [:server/organizer-pairings (tournament/get-round id round)]))

(defmethod ws/event-handler :client/organizer-standings
  [{uid :uid, [id round] :?data}]
  (ws/send! uid [:server/organizer-standings (tournament/standings id round false)]))

(defmethod ws/event-handler :client/organizer-pods
  [{uid :uid, [id number] :?data}]
  (when-not (Double/isNaN number)
    (ws/send! uid [:server/organizer-pods (tournament/pods id number)])))

(defmethod ws/event-handler :client/organizer-seatings
  [{uid :uid, id :?data}]
  (ws/send! uid [:server/organizer-seatings (tournament/seatings id)]))

(defmethod ws/event-handler :client/deck-construction
  [{uid :uid, id :?data}]
  (ws/send! uid [:server/organizer-seatings (tournament/seatings id)])
  (ws/send! uid [:server/organizer-pods (tournament/latest-pods id)]))

(defmethod ws/event-handler :client/save-decklist
  [{uid :uid, [tournament decklist] :?data}]
  (try
    (let [{:keys [id send-email?]} (decklist/save-decklist tournament decklist)]
      (ws/send! uid [:server/decklist-saved id])
      (when send-email?
        (let [tournament (decklist/get-tournament (:tournament decklist))
              {:keys [subject text]} (email/generate-message tournament (assoc decklist :id id))]
          (email/send-email (get-in decklist [:player :email]) subject text))))
    (catch Exception e
      (log/error e "Error saving decklist")
      (ws/send! uid [:server/decklist-error]))))

(defmethod ws/event-handler :client/decklist-organizer-tournament
  [{uid :uid, id :?data, ring-req :ring-req}]
  (let [tournament (decklist/get-organizer-tournament id)]
    (when (= (get-in ring-req [:session :identity :id]) (:user tournament))
      (ws/send! uid [:server/decklist-organizer-tournament tournament]))))

(defmethod ws/event-handler :client/save-decklist-organizer-tournament
  [{uid :uid, tournament :?data, ring-req :ring-req}]
  (try
    (let [user-id (get-in ring-req [:session :identity :id])
          id (decklist/save-organizer-tournament user-id tournament)]
      (when id
        (ws/send! uid [:server/organizer-tournament-saved id])))
    (catch Exception e
      (log/error e "Error saving tournament")
      (ws/send! uid [:server/decklist-error]))))

(defmethod ws/event-handler :client/load-decklist
  [{uid :uid, id :?data}]
  (ws/send! uid [:server/decklist (decklist/get-decklist id)]))

(defmethod ws/event-handler :client/load-decklists
  [{uid :uid, ids :?data}]
  (ws/send! uid [:server/decklists (->> (map decklist/get-decklist ids)
                                        (sort-by (fn [d]
                                                   [(get-in d [:player :last-name])
                                                    (get-in d [:player :first-name])])))]))

(defmethod ws/event-handler :client/load-decklist-with-id
  [{uid :uid, id :?data}]
  (if-let [decklist (decklist/get-decklist id)]
    (ws/send! uid [:server/decklist (dissoc decklist :id :player)])
    (ws/send! uid [:server/decklist-load-error "Pakkalistaa ei löytynyt"])))

(defmethod ws/event-handler :client/decklist-card-suggestions
  [{[prefix format] :?data, reply-fn :?reply-fn}]
  (reply-fn (decklist/search-cards prefix format)))

(defmethod ws/event-handler :client/load-text-decklist
  [{uid :uid, [text-decklist format] :?data}]
  (ws/send! uid [:server/decklist (decklist/load-text-decklist text-decklist format)]))
