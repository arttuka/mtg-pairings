(ns mtg-pairings-server.components.decklist.submit
  (:require [reagent.core :as reagent :refer [atom]]
            [re-frame.core :refer [subscribe dispatch]]
            [cljsjs.material-ui]
            [cljs-react-material-ui.reagent :as ui]
            [cljs-react-material-ui.icons :as icons]
            [prop-types]
            [cljs.core.async :refer [<!] :refer-macros [go]]
            [cljs-http.client :as http]
            [mtg-pairings-server.components.autosuggest :refer [autosuggest]]
            [mtg-pairings-server.events :as events]
            [mtg-pairings-server.subscriptions :as subs]
            [mtg-pairings-server.util :refer [debounce dissoc-index format-date index-where]]
            [mtg-pairings-server.util.mtg :refer [valid-dci?]]
            [mtg-pairings-server.util.material-ui :refer [get-theme text-field]]
            [clojure.string :as str]))

(def basic? #{"Plains" "Island" "Swamp" "Mountain" "Forest" "Wastes"
              "Snow-Covered Plains" "Snow-Covered Island" "Snow-Covered Swamp"
              "Snow-Covered Mountain" "Snow-Covered Forest"})

(defn get-cards [prefix format]
  (go (:body (<! (http/get "/api/card/search"
                           {:query-params {:prefix prefix
                                           :format (name format)}})))))

(defn input [on-change]
  (let [tournament (subscribe [::subs/decklist-tournament])
        suggestions (atom [])
        fetch-suggestions (debounce (fn [prefix]
                                      (go
                                        (reset! suggestions
                                                (<! (get-cards prefix (:format @tournament))))))
                                    250)
        clear-suggestions #(reset! suggestions [])]
    (fn input-render [_ _]
      [autosuggest {:suggestions                    suggestions
                    :on-change                      on-change
                    :on-suggestions-fetch-requested fetch-suggestions
                    :on-suggestions-clear-requested clear-suggestions
                    :id                             :decklist-autosuggest
                    :floating-label-text            "Lisää kortti..."}])))

(defn valid-player-data? [{:keys [first-name last-name dci]}]
  (and (valid-dci? dci)
       (not (str/blank? first-name))
       (not (str/blank? last-name))))

(defn decklist-errors [decklist]
  (let [cards (reduce (fn [acc {:keys [name quantity]}]
                        (merge-with + acc {name quantity}))
                      {}
                      (concat (:main decklist) (:side decklist)))
        errors (list* (when-not (valid-player-data? (:player decklist)) {:type :player-data
                                                                         :id   :missing-player-data
                                                                         :text "Osa pelaajan tiedoista puuttuu"})
                      (when (< (get-in decklist [:count :main]) 60) {:type :maindeck
                                                                     :id   :deck-error-maindeck
                                                                     :text "Maindeckissä on alle 60 korttia"})
                      (when (> (get-in decklist [:count :side]) 15) {:type :sideboard
                                                                     :id   :deck-error-sideboard
                                                                     :text "Sideboardilla on yli 15 korttia"})
                      (for [[card quantity] cards
                            :when (not (basic? card))
                            :when (> quantity 4)]
                        {:type :card-over-4
                         :id   (str "deck-error-card--" card)
                         :card card
                         :text (str "Korttia " card " on yli 4 kappaletta")}))]
    (filter some? errors)))

(defn cards-with-error [decklist]
  (into #{}
        (comp (filter #(= :card-over-4 (:type %)))
              (map :card))
        (decklist-errors decklist)))

(defn add-card [{:keys [board] :as decklist} card]
  (if (some #(= card (:name %)) (get decklist board))
    decklist
    (-> decklist
        (update board conj {:name card, :quantity 1})
        (update-in [:count board] inc))))

(defn set-quantity [decklist board name quantity]
  (let [index (index-where #(= name (:name %)) (get decklist board))
        orig-quantity (get-in decklist [board index :quantity])]
    (-> decklist
        (assoc-in [board index :quantity] quantity)
        (update-in [:count board] + (- quantity orig-quantity)))))

(defn remove-card [decklist board name]
  (let [cards (get decklist board)
        index (index-where #(= name (:name %)) cards)
        n (get-in decklist [board index :quantity])]
    (-> decklist
        (assoc board (dissoc-index cards index))
        (update-in [:count board] - n))))

(defn error-icon []
  (reagent/create-class
   {:context-types  #js {:muiTheme prop-types/object.isRequired}
    :reagent-render (fn error-icon-render []
                      (let [palette (:palette (get-theme (reagent/current-component)))]
                        [icons/alert-warning {:color (:accent3Color palette)
                                              :style {:vertical-align :top}}]))}))

(defn decklist-table-row [decklist board card error?]
  (let [on-change (fn [_ _ quantity]
                    (swap! decklist set-quantity board (:name card) quantity))
        on-delete (fn []
                    (swap! decklist remove-card board (:name card)))]
    (fn decklist-table-row-render [_ _ {:keys [name quantity]} error?]
      [ui/table-row
       [ui/table-row-column {:class-name :quantity
                             :style      {:padding "0 12px"}}
        [ui/select-field {:value           quantity
                          :on-change       on-change
                          :style           {:width          "48px"
                                            :vertical-align :top}
                          :menu-style      {:width "48px"}
                          :icon-style      {:padding-left  0
                                            :padding-right 0
                                            :width         "24px"
                                            :fill          "rgba(0, 0, 0, 0.54)"}
                          :underline-style {:border-color "rgba(0, 0, 0, 0.24)"}}
         (for [i (range 1 (if (basic? name)
                            31
                            5))]
           ^{:key (str name "--quantity--" i)}
           [ui/menu-item {:value           i
                          :primary-text    i
                          :inner-div-style {:padding "0 6px"}}])]]
       [ui/table-row-column {:class-name :card
                             :style      {:font-size "14px"}}
        name]
       [ui/table-row-column {:class-name :actions
                             :style      {:font-size "14px"
                                          :padding   0}}
        [ui/icon-button {:on-click on-delete}
         [icons/content-remove-circle-outline]]]
       [ui/table-row-column {:class-name :error
                             :style      {:padding "12px"}}
        (when error?
          [error-icon])]])))

(defn decklist-table [decklist board]
  (let [header-style {:color       :black
                      :font-weight :bold
                      :font-size   "16px"
                      :height      "36px"}]
    (fn decklist-table-render [decklist board]
      (let [error-cards (cards-with-error @decklist)]
        [:div.deck-table-container
         [:h3 (if (= :main board)
                (str "Main deck (" (get-in @decklist [:count :main]) ")")
                (str "Sideboard (" (get-in @decklist [:count :side]) ")"))]
         [ui/table {:selectable false
                    :class-name :deck-table}
          [ui/table-header {:display-select-all  false
                            :adjust-for-checkbox false}
           [ui/table-row {:style {:height "24px"}}
            [ui/table-header-column {:class-name :quantity
                                     :style      (merge header-style
                                                        {:padding "0 12px"})}
             "Määrä"]
            [ui/table-header-column {:class-name :card
                                     :style      header-style}
             "Kortti"]
            [ui/table-header-column {:class-name :actions}]
            [ui/table-header-column {:class-name :error}]]]
          [ui/table-body
           (for [{:keys [name] :as card} (get @decklist board)]
             ^{:key (str name "--" board "--tr")}
             [decklist-table-row decklist board card (contains? error-cards name)])]]]))))

(defn player-info [decklist]
  (let [set-first-name #(swap! decklist assoc-in [:player :first-name] %)
        set-last-name #(swap! decklist assoc-in [:player :last-name] %)
        set-deck-name #(swap! decklist assoc-in [:player :deck-name] %)
        set-email #(swap! decklist assoc-in [:player :email] %)
        set-dci #(swap! decklist assoc-in [:player :dci] %)]
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
                     :style               {:vertical-align :top}}]]])))

(def empty-decklist {:main   []
                     :side   []
                     :count  {:main 0
                              :side 0}
                     :board  :main
                     :player {:dci        ""
                              :first-name ""
                              :last-name  ""
                              :deck-name  ""
                              :email      ""}})

(defn error-list [errors]
  [ui/list
   (for [error errors]
     ^{:key (str "error--" (name (:type error)))}
     [ui/list-item {:primary-text (:text error)
                    :left-icon    (reagent/as-element [:div
                                                       [error-icon]])}])])

(defn decklist-submit []
  (let [saved-decklist (subscribe [::subs/decklist])
        decklist (atom (or @saved-decklist empty-decklist))
        on-change (fn [card]
                    (swap! decklist add-card card))
        select-main #(swap! decklist assoc :board :main)
        select-side #(swap! decklist assoc :board :side)
        button-style {:position  :relative
                      :top       "-5px"
                      :width     "70px"
                      :min-width "70px"}
        tournament (subscribe [::subs/decklist-tournament])
        saving? (subscribe [::subs/decklist-saving?])
        saved? (subscribe [::subs/decklist-saved?])
        page (subscribe [::subs/page])
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
         [input on-change]
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
         [error-list errors]
         (when @saved?
           (let [url (str "https://pairings.fi/decklist/" (:id @page))]
             [:div.success-notice
              [:h4 "Tallennus onnistui!"]
              [:p
               "Pakkalistasi tallennus onnistui. Pääset muokkaamaan pakkalistaasi osoitteessa "
               [:a {:href url}
                url]
               ". Jos annoit sähköpostiosoitteesi, pakkalistasi sekä sama osoite lähetettiin sinulle myös sähköpostitse. "]]))]))))
