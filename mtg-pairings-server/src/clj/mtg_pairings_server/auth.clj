(ns mtg-pairings-server.auth
  (:require [buddy.auth.backends.session :refer [session-backend]]
            [buddy.auth.middleware :refer [wrap-authentication wrap-authorization]]
            [clojure.string :as str]
            [compojure.api.sweet :refer :all]
            [config.core :refer [env]]
            [ring.util.response :refer [redirect]]
            [schema.core :as s]
            [korma.core :as sql]
            [mtg-pairings-server.sql-db :as db]
            [mtg-pairings-server.util.sql :as sql-util])
  (:import (java.security MessageDigest)))

(defonce auth-backend (session-backend))

(defn wrap-auth [handler]
  (-> handler
      (wrap-authorization auth-backend)
      (wrap-authentication auth-backend)))

(defn ^:private ->hex [bytes]
  (apply str (for [b bytes] (format "%02x" b))))

(defn ^:private authenticate [username password]
  (when-let [user (sql-util/select-unique-or-nil db/smf-user
                    (sql/fields [:id_member :id] [:passwd :hash])
                    (sql/where {:member_name username}))]
    (let [input (str (str/lower-case username) password)
          md (MessageDigest/getInstance "SHA-1")
          _ (.update md (.getBytes input "UTF-8"))
          sha1 (->hex (.digest md))]
      (when (= sha1 (:hash user))
        {:id       (:id user)
         :username username}))))

(defn organizer-path []
  (if (str/blank? (env :decklist-prefix))
    "/decklist/organizer"
    "/organizer"))

(defroutes auth-routes
  (POST "/login" request
    :form-params [username :- s/Str
                  password :- s/Str
                  __anti-forgery-token :- s/Str]
    :query-params [next :- s/Str]
    (if-let [user (authenticate username password)]
      (let [new-session (assoc (:session request) :identity user)]
        (-> (redirect next :see-other)
            (assoc :session new-session)))
      (redirect (organizer-path) :see-other)))
  (GET "/logout" request
    (let [new-session (dissoc (:session request) :identity)]
      (-> (redirect (organizer-path) :see-other)
          (assoc :session new-session)))))
