(ns mtg-pairings-server.components.decklist.submit
  (:require [reagent.core :as reagent :refer [atom]]
            [re-frame.core :refer [subscribe dispatch]]
            [cljsjs.material-ui]
            [cljs-react-material-ui.reagent :as ui]
            [cljs-react-material-ui.icons :as icons]
            [oops.core :refer [oget]]
            [mtg-pairings-server.components.autosuggest :refer [autosuggest]]
            [mtg-pairings-server.components.tooltip :refer [tooltip]]
            [mtg-pairings-server.events.decklist :as events]
            [mtg-pairings-server.routes.decklist :as routes]
            [mtg-pairings-server.styles.common :as styles]
            [mtg-pairings-server.subscriptions.common :as common-subs]
            [mtg-pairings-server.subscriptions.decklist :as subs]
            [mtg-pairings-server.util :refer [debounce dissoc-index format-date index-where get-host]]
            [mtg-pairings-server.util.decklist :refer [->text]]
            [mtg-pairings-server.util.mtg :refer [valid-dci?]]
            [mtg-pairings-server.util.material-ui :refer [text-field]]
            [clojure.string :as str]))

(def basic? #{"Plains" "Island" "Swamp" "Mountain" "Forest" "Wastes"
              "Snow-Covered Plains" "Snow-Covered Island" "Snow-Covered Swamp"
              "Snow-Covered Mountain" "Snow-Covered Forest"})

(defn input []
  (let [tournament (subscribe [::subs/tournament])
        mobile? (subscribe [::common-subs/mobile?])
        on-change #(dispatch [::events/add-card %])
        suggestions (atom [])
        fetch-suggestions (debounce (fn [prefix]
                                      (dispatch [::events/card-suggestions
                                                 prefix
                                                 (:format @tournament)
                                                 #(reset! suggestions %)]))
                                    250)
        clear-suggestions #(reset! suggestions [])]
    (fn input-render [_ _]
      (let [width (if @mobile?
                    "calc(100vw - 140px)"
                    256)]
        [autosuggest {:suggestions                    suggestions
                      :on-change                      on-change
                      :on-suggestions-fetch-requested fetch-suggestions
                      :on-suggestions-clear-requested clear-suggestions
                      :id                             :decklist-autosuggest
                      :floating-label-text            "Lisää kortti..."
                      :styles                         {:container {:width width}
                                                       :input     {:width width}}}]))))

(defn valid-player-data? [{:keys [first-name last-name dci]}]
  (and (valid-dci? dci)
       (not (str/blank? first-name))
       (not (str/blank? last-name))))

(defn decklist-errors [decklist]
  (let [cards (reduce (fn [acc {:keys [name quantity]}]
                        (merge-with + acc {name quantity}))
                      {}
                      (concat (:main decklist) (:side decklist)))
        errors (concat [(when-not (valid-player-data? (:player decklist)) {:type :player-data
                                                                           :id   :missing-player-data
                                                                           :text "Osa pelaajan tiedoista puuttuu"})
                        (when (< (get-in decklist [:count :main]) 60) {:type :maindeck
                                                                       :id   :deck-error-maindeck
                                                                       :text "Maindeckissä on alle 60 korttia"})
                        (when (> (get-in decklist [:count :side]) 15) {:type :sideboard
                                                                       :id   :deck-error-sideboard
                                                                       :text "Sideboardilla on yli 15 korttia"})]
                       (for [[card quantity] cards
                             :when (not (basic? card))
                             :when (> quantity 4)]
                         {:type :card-over-4
                          :id   (str "deck-error-card--" card)
                          :card card
                          :text (str "Korttia " card " on yli 4 kappaletta")})
                       (for [{:keys [error name]} (concat (:main decklist) (:side decklist))
                             :when error]
                         {:type :other
                          :id   (str "other-error-card--" name)
                          :text (str error ": " name)}))]
    (filter some? errors)))

(defn cards-with-error [decklist]
  (into {}
        (comp (filter #(= :card-over-4 (:type %)))
              (map (juxt :card :text)))
        (decklist-errors decklist)))

(defn error-icon [error]
  (let [icon [icons/alert-warning {:title error
                                   :style {:color          "#f44336"
                                           :vertical-align :top}}]]
    (if error
      [tooltip {:label error}
       icon]
      icon)))

(defn decklist-table-row [board card error]
  (let [mobile? (subscribe [::common-subs/mobile?])
        on-change (fn [_ _ quantity]
                    (dispatch [::events/set-quantity board (:id card) quantity]))
        on-delete #(dispatch [::events/remove-card board (:id card)])]
    (fn decklist-table-row-render [_ {:keys [name quantity]} error]
      [ui/table-row
       [ui/table-row-column {:class-name :quantity
                             :style      {:padding "0 12px"}}
        (when quantity
          (into [ui/select-field {:value           quantity
                                  :on-change       on-change
                                  :style           {:width          "48px"
                                                    :vertical-align :top}
                                  :menu-style      {:width "48px"}
                                  :icon-style      {:padding-left  0
                                                    :padding-right 0
                                                    :width         "24px"
                                                    :fill          "rgba(0, 0, 0, 0.54)"}
                                  :underline-style {:border-color "rgba(0, 0, 0, 0.24)"}}]
                (for [i (range 1 (if (basic? name)
                                   31
                                   5))]
                  [ui/menu-item {:value           i
                                 :primary-text    i
                                 :inner-div-style {:padding "0 6px"}}])))]
       [ui/table-row-column {:class-name :card
                             :style      {:font-size "16px"
                                          :padding-left (if @mobile? 0 "24px")}}
        name]
       [ui/table-row-column {:class-name :actions
                             :style      {:font-size "16px"
                                          :padding   0}}
        [ui/icon-button {:on-click on-delete}
         [icons/content-remove-circle-outline]]]
       [ui/table-row-column {:class-name :error
                             :style      {:padding  "12px"
                                          :overflow :visible}}
        (when error
          [error-icon error])]])))

(defn decklist-table [decklist board]
  (let [mobile? (subscribe [::common-subs/mobile?])
        header-style {:color       :black
                      :font-weight :bold
                      :font-size   "16px"
                      :height      "36px"}]
    (fn decklist-table-render [decklist board]
      (let [error-cards (cards-with-error @decklist)]
        [:div.deck-table-container
         [:h3 (if (= :main board)
                (str "Main deck (" (get-in @decklist [:count :main]) ")")
                (str "Sideboard (" (get-in @decklist [:count :side]) ")"))]
         [ui/table {:selectable    false
                    :class-name    :deck-table
                    :wrapper-style {:overflow :visible}
                    :body-style    {:overflow :visible}}
          [ui/table-header {:display-select-all  false
                            :adjust-for-checkbox false}
           [ui/table-row {:style {:height "24px"}}
            [ui/table-header-column {:class-name :quantity
                                     :style      (merge header-style
                                                        {:padding "0 12px"})}
             "Määrä"]
            [ui/table-header-column {:class-name :card
                                     :style      (merge header-style
                                                        (when @mobile?
                                                          {:padding-left 0}))}
             "Kortti"]
            [ui/table-header-column {:class-name :actions}]
            [ui/table-header-column {:class-name :error}]]]
          [ui/table-body
           (for [{:keys [id name error] :as card} (get @decklist board)]
             ^{:key (str id "--tr")}
             [decklist-table-row board card (or error (get error-cards name))])]]]))))

(defn player-info [decklist]
  (let [set-first-name #(dispatch [::events/update-player-info :first-name %])
        set-last-name #(dispatch [::events/update-player-info :last-name %])
        set-deck-name #(dispatch [::events/update-player-info :deck-name %])
        set-email #(dispatch [::events/update-player-info :email %])
        set-dci #(dispatch [::events/update-player-info :dci %])]
    (fn player-info-render [_]
      [:div#player-info
       [:div.full-width
        [text-field {:on-change           set-deck-name
                     :floating-label-text "Pakan nimi"
                     :full-width          true
                     :value               (get-in @decklist [:player :deck-name])
                     :style               {:vertical-align :top}}]]
       [:div.half-width.left
        (let [value (get-in @decklist [:player :first-name])]
          [text-field {:on-change           set-first-name
                       :floating-label-text "Etunimi"
                       :full-width          true
                       :value               value
                       :error-text          (when (str/blank? value)
                                              "Etunimi on pakollinen")
                       :style               {:vertical-align :top}}])]
       [:div.half-width.right
        (let [value (get-in @decklist [:player :last-name])]
          [text-field {:on-change           set-last-name
                       :floating-label-text "Sukunimi"
                       :full-width          true
                       :value               value
                       :error-text          (when (str/blank? value)
                                              "Etunimi on pakollinen")
                       :style               {:vertical-align :top}}])]
       [:div.half-width.left
        (let [value (get-in @decklist [:player :dci])]
          [text-field {:on-change           set-dci
                       :floating-label-text "DCI-numero"
                       :full-width          true
                       :value               value
                       :error-text          (when-not (valid-dci? value)
                                              "Virheellinen DCI-numero")
                       :style               {:vertical-align :top}}])]
       [:div.half-width.right
        [text-field {:on-change           set-email
                     :floating-label-text "Sähköposti"
                     :full-width          true
                     :value               (get-in @decklist [:player :email])
                     :style               {:vertical-align :top}
                     :disabled            (get-in @decklist [:player :email-disabled?])
                     :title               (when (get-in @decklist [:player :email-disabled?])
                                            "Tästä pakkalistasta on jo lähetetty sähköpostiviesti.")}]]])))

(defn decklist-import []
  (let [mobile? (subscribe [::common-subs/mobile?])
        selected (atom nil)
        on-active (fn [tab]
                    (let [value (oget tab "props" "value")]
                      (reset! selected (if (= value @selected)
                                         nil
                                         value))))
        address (atom "")
        address-on-change #(reset! address %)
        import-from-address #(dispatch [::events/import-address @address])
        decklist (atom "")
        decklist-on-change #(reset! decklist %)
        import-decklist (fn []
                          (dispatch [::events/import-text @decklist])
                          (reset! selected nil))
        border (styles/border "1px" :solid (styles/palette :light-grey))]

    (fn decklist-import-render []
      [:div.decklist-import
       [ui/tabs {:value                   @selected
                 :content-container-style (when @selected
                                            {:border-bottom border
                                             :border-left   (when-not @mobile? border)
                                             :border-right  (when-not @mobile? border)})}
        [ui/tab {:label     "Lataa aiempi lista"
                 :value     "load-previous"
                 :on-active on-active
                 :style     {:color :black}}
         [:div.info
          [:h3
           "Lataa aiempi lista"]
          [:p
           "Lataa aiemmin syötetty pakkalista antamalla sen osoite (esim. "
           [:span.address "https://decklist.pairings.fi/abcd..."]
           ")."]]
         [:div.form
          [text-field {:on-change           address-on-change
                       :floating-label-text "Osoite"
                       :full-width          true}]
          [:br]
          [ui/raised-button {:label    "Lataa"
                             :disabled (str/blank? @address)
                             :on-click import-from-address}]]]
        [ui/tab {:label     "Lataa tekstilista"
                 :value     "load-text"
                 :on-active on-active
                 :style     {:color :black}}
         [:div.info
          [:h3
           "Lataa tekstimuotoinen lista"]
          [:p
           "Kopioi tekstikenttään tekstimuotoinen lista."
           "Listassa tulee olla seuraavassa muodossa: lukumäärä, välilyönti, kortin nimi. Esimerkki:"]
          [:pre
           "4 Lightning Bolt\n4 Chain Lightning\n..."]
          [:p "Maindeckin ja sideboardin väliin tulee rivi, jolla lukee pelkästään \"Sideboard\"."]]
         [:div.form
          [text-field {:on-change      decklist-on-change
                       :multi-line     true
                       :rows           7
                       :rows-max       7
                       :textarea-style {:background-color "rgba(0, 0, 0, 0.05)"}
                       :full-width     true}]
          [:br]
          [ui/raised-button {:label    "Lataa"
                             :disabled (str/blank? @decklist)
                             :on-click import-decklist}]]]]])))

(defn error-list [errors]
  [ui/list
   (for [error errors]
     ^{:key (str "error--" (name (:type error)))}
     [ui/list-item {:primary-text (:text error)
                    :left-icon    (reagent/as-element [:div
                                                       [error-icon nil]])}])])

(defn decklist-submit []
  (let [decklist (subscribe [::subs/decklist])
        select-main #(dispatch [::events/select-board :main])
        select-side #(dispatch [::events/select-board :side])
        button-style {:position  :relative
                      :top       "-5px"
                      :width     "70px"
                      :min-width "70px"}
        tournament (subscribe [::subs/tournament])
        saving? (subscribe [::subs/saving?])
        saved? (subscribe [::subs/saved?])
        error? (subscribe [::subs/error?])
        page (subscribe [::common-subs/page])
        save-decklist #(dispatch [::events/save-decklist (:id @tournament) @decklist])]
    (fn decklist-submit-render []
      (let [errors (decklist-errors @decklist)]
        [:div#decklist-submit
         [:h2 "Lähetä pakkalista"]
         [:p.intro
          "Lähetä pakkalistasi "
          [:span.tournament-date
           (format-date (:date @tournament))]
          " pelattavaan turnaukseen "
          [:span.tournament-name
           (:name @tournament)]
          ", jonka formaatti on "
          [:span.tournament-format
           (case (:format @tournament)
             :standard "Standard"
             :modern "Modern"
             :legacy "Legacy")]
          "."]
         [:h3 "Pakkalista"]
         [input]
         [ui/raised-button
          {:label        "Main"
           :on-click     select-main
           :primary      (= :main (:board @decklist))
           :style        button-style
           :button-style {:border-radius "2px 0 0 2px"}}]
         [ui/raised-button
          {:label        "Side"
           :on-click     select-side
           :primary      (= :side (:board @decklist))
           :style        button-style
           :button-style {:border-radius "0 2px 2px 0"}}]
         [:div
          [decklist-table decklist :main]
          [decklist-table decklist :side]]
         [decklist-import]
         [:h3 "Pelaajan tiedot"]
         [player-info decklist]
         [ui/raised-button
          {:label    "Tallenna"
           :on-click save-decklist
           :primary  true
           :disabled (boolean (or @saving? (seq errors)))
           :style    {:margin-top "24px"}}]
         (when @saving?
           [ui/circular-progress
            {:size  36
             :style {:margin         "24px 0 0 24px"
                     :vertical-align :top}}])
         (when @saved?
           (let [url (str (get-host) (routes/old-decklist-path {:id (:id @page)}))]
             [:div.success-notice
              [:h4 "Tallennus onnistui!"]
              [:p
               "Pakkalistasi tallennus onnistui. Pääset muokkaamaan pakkalistaasi osoitteessa "
               [:a {:href url}
                url]
               ". Jos annoit sähköpostiosoitteesi, pakkalistasi sekä sama osoite lähetettiin sinulle myös sähköpostitse. "]]))
         (when @error?
           [:div.error-notice
            [:h4 "Tallennus epäonnistui"]
            [:p "Pakkalistan tallennus epäonnistui. Voit kopioida pakkalistasi tekstimuodossa alta ja yrittää myöhemmin uudelleen."]
            [:pre (->text @decklist)]])
         [error-list errors]]))))
