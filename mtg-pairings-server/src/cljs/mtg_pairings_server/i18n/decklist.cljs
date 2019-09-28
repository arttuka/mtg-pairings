(ns mtg-pairings-server.i18n.decklist
  (:require [clojure.string :as str]
            [goog.string :as gstring]
            [goog.string.format]
            [mtg-pairings-server.util :as util]))

(def ^:private translations
  {:organizer {:all-tournaments {:fi "Kaikki turnaukset"
                                 :en "All tournaments"}
               :new-tournament  {:fi "Uusi turnaus"
                                 :en "New tournament"}
               :log-out         {:fi "Kirjaudu ulos"
                                 :en "Log out"}
               :date            {:fi "Päivä"
                                 :en "Date"}
               :deadline        {:fi "Deadline"
                                 :en "Deadline"}
               :tournament      {:title          {:fi "Turnaus"
                                                  :en "Tournament"}
                                 :name           {:fi "Turnauksen nimi"
                                                  :en "Name of tournament"}
                                 :name-error     {:fi "Nimi on pakollinen"
                                                  :en "Name is required"}
                                 :format         {:fi "Formaatti"
                                                  :en "Format"}
                                 :format-error   {:fi "Formaatti on pakollinen"
                                                  :en "Format is required"}
                                 :date           {:fi "Päivämäärä"
                                                  :en "Date"}
                                 :date-error     {:fi "Päivämäärä on pakollinen"
                                                  :en "Date is required"}
                                 :deadline       {:fi "Deadline"
                                                  :en "Deadline"}
                                 :deadline-error {:fi "Listojen lähettämisen deadline on pakollinen"
                                                  :en "Deadline for submitting lists is required"}
                                 :deadline-time  {:fi "Deadline klo"
                                                  :en "Deadline time"}}
               :decklists       {:fi "Dekkilistoja"
                                 :en "Decklists"}
               :submit-page     {:fi "Listojen lähetyssivu"
                                 :en "Page for submitting lists"}
               :dci             {:fi "DCI"
                                 :en "DCI"}
               :name            {:fi "Nimi"
                                 :en "Name"}
               :sent            {:fi "Lähetetty"
                                 :en "Sent"}
               :save            {:title   {:fi "Tallenna"
                                           :en "Save"}
                                 :success {:fi "Tallennus onnistui"
                                           :en "Saving successful"}
                                 :fail    {:fi "Tallennus epäonnistui"
                                           :en "Saving failed"}}
               :print-lists     {:fi "Tulosta valitut listat"
                                 :en "Print selected lists"}
               :no-lists        {:fi "Ei lähetettyjä listoja"
                                 :en "No submitted decklists"}
               :log-in          {:text     {:fi "Kirjaudu sisään MtgSuomi-tunnuksillasi"
                                            :en "Login with your MtgSuomi account"}
                                 :username {:fi "Käyttäjätunnus"
                                            :en "Username"}
                                 :password {:fi "Salasana"
                                            :en "Password"}
                                 :button   {:fi "Kirjaudu"
                                            :en "Log in"}}}
   :submit    {:add-card       {:fi "Lisää kortti..."
                                :en "Add card..."}
               :error          {:missing-player-data   {:fi "Osa pelaajan tiedoista puuttuu"
                                                        :en "Player information missing"}
                                :deck-error-maindeck   {:fi "Maindeckissä on alle 60 korttia"
                                                        :en "Less than 60 cards in main deck"}
                                :deck-error-sideboard  {:fi "Sideboardilla on yli 15 korttia"
                                                        :en "More than 15 cards in sideboard"}
                                :card-over-4           {:fi "Korttia on yli 4 kappaletta"
                                                        :en "More than 4 copies of card"}
                                :first-name            {:fi "Etunimi on pakollinen"
                                                        :en "First name is required"}
                                :last-name             {:fi "Sukunimi on pakollinen"
                                                        :en "Last name is required"}
                                :dci                   {:fi "Virheellinen DCI-numero"
                                                        :en "Invalid DCI number"}
                                :email                 {:fi "Virheellinen sähköposti"
                                                        :en "Invalid email address"}
                                :address               {:fi "Virheellinen osoite"
                                                        :en "Invalid address"}
                                :not-found             {:fi "Pakkalistaa ei löytynyt"
                                                        :en "Decklist not found"}
                                :decklist-import-error {:fi "Virhe pakkalistan latauksessa"
                                                        :en "Error importing decklist"}}
               :quantity       {:fi "Määrä"
                                :en "Qty"}
               :card           {:fi "Kortti"
                                :en "Card"}
               :deck-name      {:fi "Pakan nimi"
                                :en "Deck name"}
               :first-name     {:fi "Etunimi"
                                :en "First name"}
               :last-name      {:fi "Sukunimi"
                                :en "Last name"}
               :dci            {:fi "DCI-numero"
                                :en "DCI number"}
               :email          {:fi "Sähköposti"
                                :en "Email address"}
               :email-disabled {:fi "Tästä pakkalistasta on jo lähetetty sähköposti"
                                :en "An email has already been sent about this decklist"}
               :load           {:fi "Lataa"
                                :en "Load"}
               :load-previous  {:label   {:fi "Lataa aiempi lista"
                                          :en "Load previous list"}
                                :text    [{:fi "Lataa aiemmin syötetty pakkalista antamalla sen osoite (esim. "
                                           :en "Load a previously submitted decklist by giving its address (for example "}
                                          {:fi ")."
                                           :en ")."}]
                                :address {:fi "Osoite"
                                          :en "Address"}}
               :load-text      {:label  {:fi "Lataa tekstilista"
                                         :en "Load from text"}
                                :header {:fi "Lataa tekstimuotoinen lista"
                                         :en "Load decklist in text form"}
                                :info   [{:fi "Kopioi tekstikenttään tekstimuotoinen lista. Listassa tulee olla seuraavassa muodossa: lukumäärä, välilyönti, kortin nimi. Esimerkki:"
                                          :en "Copy a decklist into the text field. The list must be in the following format: number, space, card name. Example:"}
                                         {:fi "Maindeckin ja sideboardin väliin tulee rivi, jolla lukee pelkästään \"Sideboard\"."
                                          :en "Main deck and sideboard are separated by a line that reads only \"Sideboard\"."}]}
               :decklist       {:fi "Pakkalista"
                                :en "Deck list"}
               :player-info    {:fi "Pelaajan tiedot"
                                :en "Player information"}
               :save           {:button  {:fi "Tallenna"
                                          :en "Save"}
                                :success {:header {:fi "Tallennus onnistui!"
                                                   :en "Decklist saved successfully!"}
                                          :info   [{:fi "Pakkalistasi tallennus onnistui. Pääset muokkaamaan pakkalistaasi osoitteessa "
                                                    :en "Decklist saved successfully. You can edit your decklist at "}
                                                   {:fi ". Jos annoit sähköpostiosoitteesi, pakkalistasi sekä sama osoite lähetettiin sinulle myös sähköpostitse."
                                                    :en ". If you gave your email address, your decklist and the address have been sent to your email."}]}
                                :error   {:header {:fi "Tallennus epäonnistui"
                                                   :en "Error while saving decklist"}
                                          :info   {:fi "Pakkalistan tallennus epäonnistui. Voit kopioida pakkalistasi tekstimuodossa alta ja yrittää myöhemmin uudelleen."
                                                   :en "Saving your decklist failed. You may copy your list in text format from below and try again later."}}}
               :header         {:fi "Lähetä pakkalista"
                                :en "Submit decklist"}
               :intro          [{:fi "Lähetä pakkalistasi turnaukseen "
                                 :en "Submit your decklist for tournament "}
                                {:fi ", jonka päivämäärä on "
                                 :en " on "}
                                {:fi " ja jonka formaatti on "
                                 :en " whose format is "}
                                {:fi "Lista on lähetettävä viimeistään "
                                 :en "Deadline for submitting lists is at "}]
               :time-until-deadline {:fi "Aikaa jäljellä %d pv %d h %d min."
                                     :en "Time left %d d %d h %d min."}
               :deadline-gone  {:fi "Listojen lähetys tähän turnaukseen on päättynyt."
                                :en "Submitting lists for this tournament has ended."}
               :your-decklist  {:fi "Lähettämäsi lista"
                                :en "Your decklist"}}})

(defn translate [language key & args]
  (if-let [translation (get-in translations (concat (util/split-key key true) [language]))]
    (apply gstring/format translation args)
    (throw (js/Error. (str "No translation found for language "
                           (name language)
                           " and key "
                           key)))))
