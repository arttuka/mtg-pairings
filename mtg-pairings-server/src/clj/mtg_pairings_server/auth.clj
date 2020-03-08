(ns mtg-pairings-server.auth
  (:require [clojure.string :as str]
            [compojure.api.sweet :refer :all]
            [config.core :refer [env]]
            [ring.util.response :refer [redirect]]
            [schema.core :as s]
            [korma.core :as sql]
            [mtg-pairings-server.service.player :as player]
            [mtg-pairings-server.sql-db :as db]
            [mtg-pairings-server.util.broadcast :as broadcast]
            [mtg-pairings-server.util.sql :as sql-util])
  (:import (java.security MessageDigest)))

(defn ^:private ->hex [bytes]
  (str/join (for [b bytes] (format "%02x" b))))

(defn ^:private authenticate [username password]
  (when-let [user (sql-util/select-unique-or-nil db/smf-user
                    (sql/fields [:id_member :id] [:passwd :hash])
                    (sql/where {:member_name username}))]
    (let [input (str (str/lower-case username) password)
          md (doto (MessageDigest/getInstance "SHA-1")
               (.update (.getBytes input "UTF-8")))
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
        (assoc (redirect next :see-other)
               :session new-session))
      (redirect (organizer-path) :see-other)))
  (POST "/dci-login" request
    :form-params [dci :- s/Str
                  __anti-forgery-token :- s/Str]
    :query-params [next :- s/Str]
    (if-let [player (player/player dci)]
      (let [new-session (assoc (:session request) :dci (:dci player))]
        (assoc (redirect next :see-other) :session new-session))
      (redirect (str next "?login-failed"))))
  (GET "/dci-logout" request
    (let [dci (get-in request [:session :dci])
          new-session (dissoc (:session request) :dci)]
      (when dci
        (broadcast/logout-dci dci))
      (assoc (redirect "/" :see-other)
             :session new-session)))
  (GET "/logout" request
    (let [new-session (dissoc (:session request) :identity)]
      (assoc (redirect (organizer-path) :see-other)
             :session new-session))))
