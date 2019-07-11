(ns mtg-pairings-server.service.email
  (:require [aleph.http :as http]
            [byte-streams :as bs]
            [config.core :refer [env]]
            [cheshire.core :as json]
            [clojure.string :as str]
            [taoensso.timbre :as log]
            [mtg-pairings-server.util :refer [format-date]]
            [mtg-pairings-server.util.decklist :refer [decklist-url ->text]])
  (:import (clojure.lang ExceptionInfo)))

(def mailgun-endpoint "https://api.eu.mailgun.net/v3/m.pairings.fi/messages")

(defn send-email [to subject text]
  (try
    @(http/post mailgun-endpoint
                {:basic-auth  ["api" (env :mailgun-api-key)]
                 :form-params {:from    "Pairings.fi <noreply@pairings.fi>"
                               :to      to
                               :subject subject
                               :text    text}})
    (catch ExceptionInfo ex
      (let [response (-> (ex-data ex)
                         (:body)
                         (bs/to-string)
                         (json/parse-string true))]
        (log/error "Error sending email" response)))))

(defn generate-message [tournament decklist]
  (let [{:keys [id player]} decklist
        {:keys [dci first-name last-name deck-name]} player
        {:keys [name date]} tournament]
    {:subject (str "Pakkalistasi turnaukseen " name)
     :text    (str "Pakkalistasi turnaukseen " name
                   " on tallennettu. Pääset muokkaamaan pakkalistaasi osoitteessa "
                   (decklist-url id) " . "
                   "Huomaathan, että linkin avulla kuka tahansa pääsee katsomaan ja muokkaamaan listaasi. "
                   "Älä siis päästä linkkiä vääriin käsiin.\n\n"
                   "Turnaus: " name "\n"
                   "Päivämäärä: " (format-date date) "\n"
                   "Pelaaja: " first-name " " last-name " (" dci ")\n"
                   (when-not (str/blank? deck-name)
                     (str "Pakka: " deck-name "\n"))
                   "\n"
                   (->text decklist)
                   "\n"
                   "Tämä viesti on lähetetty automaattisesti eikä siihen voi vastata.\n")}))
