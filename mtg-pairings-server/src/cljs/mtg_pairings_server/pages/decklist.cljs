(ns mtg-pairings-server.pages.decklist
  (:require [reagent.core :as reagent :refer [atom]]
            [re-frame.core :refer [subscribe]]
            [cljsjs.material-ui]
            [cljs-react-material-ui.reagent :as ui]
            [cljs-react-material-ui.icons :as icons]
            [prop-types]
            [cljs.core.async :refer [<!] :refer-macros [go]]
            [cljs-http.client :as http]
            [mtg-pairings-server.components.autosuggest :refer [autosuggest]]
            [mtg-pairings-server.subscriptions :as subs]
            [mtg-pairings-server.util :refer [indexed]]
            [mtg-pairings-server.util.material-ui :refer [get-theme]]))

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
        autosuggest-on-change (fn [value]
                                (on-change value))
        on-suggestions-fetch-requested (fn [prefix]
                                         (go
                                          (reset! suggestions (<! (get-cards prefix (:format @tournament))))))
        on-suggestions-clear-requested (fn []
                                         (reset! suggestions []))]
    (fn input-render [_ _]
      [autosuggest {:suggestions                    suggestions
                    :on-change                      autosuggest-on-change
                    :on-suggestions-fetch-requested on-suggestions-fetch-requested
                    :on-suggestions-clear-requested on-suggestions-clear-requested
                    :id                             :decklist-autosuggest}])))

(defn deck-errors [deck]
  (let [cards (reduce (fn [acc {:keys [name quantity]}]
                        (merge-with + acc {name quantity}))
                      {}
                      (concat (:main deck) (:side deck)))
        errors (list* (when (< (get-in deck [:count :main]) 60) {:type :maindeck
                                                                 :id   :deck-error-maindeck
                                                                 :text "Maindeckissä on alle 60 korttia"})
                      (when (> (get-in deck [:count :side]) 15) {:type :sideboard
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

(defn cards-with-error [deck]
  (into #{}
        (comp (filter #(= :card-over-4 (:type %)))
              (map :card))
        (deck-errors deck)))

(defn add-card [{:keys [board] :as deck} card]
  (if (some #(= card (:name %)) (get deck board))
    deck
    (-> deck
        (update board conj {:name card, :quantity 1})
        (update-in [:count board] inc))))

(defn set-quantity [deck board index quantity]
  (let [orig-quantity (get-in deck [board index :quantity])]
    (-> deck
        (assoc-in [board index :quantity] quantity)
        (update-in [:count board] + (- quantity orig-quantity)))))

(defn error-icon []
  (reagent/create-class
   {:context-types  #js {:muiTheme prop-types/object.isRequired}
    :reagent-render (fn error-icon-render []
                      (let [palette (:palette (get-theme (reagent/current-component)))]
                        [icons/alert-warning {:color (:accent3Color palette)
                                              :style {:vertical-align :top}}]))}))

(defn deck-table-row [deck board index card error?]
  (let [on-change (fn [_ _ quantity]
                    (swap! deck set-quantity board index quantity))]
    (fn deck-table-row-render [_ _ _ {:keys [name quantity]} error?]
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
       [ui/table-row-column {:class-name :error
                             :style      {:padding "12px"}}
        (when error?
          [error-icon])]])))

(defn deck-table [deck board]
  (let [header-style {:color       :black
                      :font-weight :bold
                      :font-size   "16px"
                      :height      "36px"}]
    (fn deck-table-render [deck board]
      (let [error-cards (cards-with-error @deck)]
        [:div.deck-table-container
         [:h2 (if (= :main board)
                (str "Main deck (" (get-in @deck [:count :main]) ")")
                (str "Sideboard (" (get-in @deck [:count :side]) ")"))]
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
            [ui/table-header-column {:class-name :error}]]]
          [ui/table-body
           (for [[index {:keys [name] :as card}] (indexed (get @deck board))]
             ^{:key (str name "--" board "--tr")}
             [deck-table-row deck board index card (contains? error-cards name)])]]]))))

(defonce deck (atom {:main   []
                     :side   []
                     :count  {:main 0
                              :side 0}
                     :board  :main
                     :name   ""
                     :player {:dci        ""
                              :first-name ""
                              :last-name  ""
                              :email      ""}}))

(defn decklist-submit []
  (let [on-change (fn [card]
                    (swap! deck add-card card))
        select-main #(swap! deck assoc :board :main)
        select-side #(swap! deck assoc :board :side)
        button-style {:position  :relative
                      :top       "-3px"
                      :width     "70px"
                      :min-width "70px"}]
    (fn decklist-submit-render []
      [:div#decklist-submit
       [input on-change]
       [ui/raised-button
        {:label        "Main"
         :on-click     select-main
         :primary      (= :main (:board @deck))
         :style        button-style
         :button-style {:border-radius "2px 0 0 2px"}}]
       [ui/raised-button
        {:label        "Side"
         :on-click     select-side
         :primary      (= :side (:board @deck))
         :style        button-style
         :button-style {:border-radius "0 2px 2px 0"}}]
       [:div
        [deck-table deck :main]
        [deck-table deck :side]]])))
