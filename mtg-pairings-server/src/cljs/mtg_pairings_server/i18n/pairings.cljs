(ns mtg-pairings-server.i18n.pairings
  (:require [mtg-pairings-server.i18n.common :refer [make-translate]]))

(def ^:private translations
  {:filter      {:organizer      {:fi "Turnausjärjestäjä"
                                  :en "Organizer"}
                 :all-organizers {:fi "Kaikki järjestäjät"
                                  :en "All organizers"}
                 :date           {:fi "Päivämäärä"
                                  :en "Date"}
                 :date-from      {:fi "Alkaen"
                                  :en "From"}
                 :date-to        {:fi "Asti"
                                  :en "To"}
                 :player-count   {:fi "Pelaajamäärä"
                                  :en "Player count"}
                 :clear-filters  {:fi "Poista valinnat"
                                  :en "Clear filters"}
                 :title          {:fi "Hakutyökalut"
                                  :en "Filters"}}
   :header      {:front-page {:fi "Etusivu"
                              :en "Front page"}
                 :archive    {:fi "Turnausarkisto"
                              :en "Tournament archive"}
                 :logout     {:fi "Kirjaudu ulos"
                              :en "Log out"}
                 :dci        {:fi "DCI-numero"
                              :en "DCI number"}
                 :login      {:fi "Kirjaudu"
                              :en "Log in"}}
   :common      {:table     {:fi "Pöytä"
                             :en "Table"}
                 :player    {:fi "Pelaaja"
                             :en "Player"}
                 :player-n  {:fi "Pelaaja %d"
                             :en "Player %d"}
                 :players   {:fi "Pelaajat"
                             :en "Players"}
                 :seat      {:fi "Paikka"
                             :en "Seat"}
                 :seat-n    {:fi "Paikka %d"
                             :en "Seat %d"}
                 :points    {:fi "Pist."
                             :en "Pts"}
                 :pairings  {:fi "Pairings, kierros %d"
                             :en "Pairings for round %d"}
                 :standings {:fi "Standings, kierros %d"
                             :en "Standings for round %d"}
                 :pods      {:fi "Draftipodit"
                             :en "Draft pods"}
                 :seatings  {:fi "Seatings"
                             :en "Seatings"}
                 :bracket   {:fi "Playoff bracket"
                             :en "Playoff bracket"}
                 :started   {:fi "Kierros alkoi klo %s"
                             :en "Round started at %s"}}
   :pairings    {:result {:fi "Tulos"
                          :en "Result"}}
   :player      {:round     {:fi "Kierros %d"
                             :en "Round %d"}
                 :pod       {:fi "Pod %d"
                             :en "Pod %d"}
                 :seating   {:fi "Seating"
                             :en "Seating"}
                 :standings {:fi "Standings, kierros %d"
                             :en "Standings for round %d"}}
   :tournaments {:active                {:fi "Aktiiviset turnaukset"
                                         :en "Active tournaments"}
                 :to-archive            {:fi "Turnausarkistoon"
                                         :en "To tournament archive"}
                 :no-active-tournaments {:fi "Ei aktiivisia turnauksia"
                                         :en "No active tournaments"}}
   :pager       {:no-results {:fi "Ei hakutuloksia"
                              :en "No results"}}
   :front-page  {:newest-pairing {:fi "Uusin pairing"
                                  :en "Newest pairing"}}})

(def translate (make-translate translations))
