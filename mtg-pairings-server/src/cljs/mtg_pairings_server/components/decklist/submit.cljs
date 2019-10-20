(ns mtg-pairings-server.components.decklist.submit
  (:require [reagent.core :as reagent :refer [atom]]
            [reagent.ratom :refer [make-reaction]]
            [re-frame.core :refer [subscribe dispatch]]
            [clojure.string :as str]
            [reagent-material-ui.components :as ui]
            [reagent-material-ui.icons.delete-icon :refer [delete]]
            [reagent-material-ui.icons.warning :refer [warning]]
            [reagent-material-ui.styles :as styles]
            [cljs-time.core :as time]
            [oops.core :refer [oget]]
            [mtg-pairings-server.components.autosuggest :refer [autosuggest]]
            [mtg-pairings-server.components.decklist.decklist-import :refer [decklist-import]]
            [mtg-pairings-server.components.decklist.player-info :refer [player-info]]
            [mtg-pairings-server.components.decklist.print :refer [render-decklist]]
            [mtg-pairings-server.components.language-selector :refer [language-selector]]
            [mtg-pairings-server.components.tooltip :refer [tooltip]]
            [mtg-pairings-server.events.decklist :as events]
            [mtg-pairings-server.routes.decklist :as routes]
            [mtg-pairings-server.subscriptions.common :as common-subs]
            [mtg-pairings-server.subscriptions.decklist :as subs]
            [mtg-pairings-server.util :refer [debounce format-date format-date-time index-where get-host valid-email?]]
            [mtg-pairings-server.util.decklist :refer [->text card-types]]
            [mtg-pairings-server.util.mtg :refer [valid-dci?]]
            [mtg-pairings-server.util.material-ui :refer [text-field wrap-on-change]]
            [mtg-pairings-server.util :as util]))

(def basic? #{"Plains" "Island" "Swamp" "Mountain" "Forest" "Wastes"
              "Snow-Covered Plains" "Snow-Covered Island" "Snow-Covered Swamp"
              "Snow-Covered Mountain" "Snow-Covered Forest"})

(defn input []
  (let [tournament (subscribe [::subs/tournament])
        mobile? (subscribe [::common-subs/mobile?])
        translate (subscribe [::subs/translate])
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
                    256)
            translate @translate]
        [autosuggest {:suggestions                    suggestions
                      :on-change                      on-change
                      :on-suggestions-fetch-requested fetch-suggestions
                      :on-suggestions-clear-requested clear-suggestions
                      :suggestion->string             :name
                      :id                             :decklist-autosuggest
                      :label                          (translate :submit.add-card)
                      :styles                         {:container {:width width}
                                                       :input     {:width width}}}]))))

(defn valid-player-data? [{:keys [first-name last-name dci]}]
  (and (valid-dci? dci)
       (not (str/blank? first-name))
       (not (str/blank? last-name))))

(defn decklist-errors [decklist]
  (let [all-cards (concat (mapcat (:main decklist) card-types) (:side decklist))
        cards (reduce (fn [acc {:keys [name quantity]}]
                        (merge-with + acc {name quantity}))
                      {}
                      all-cards)
        errors (concat [(when-not (valid-player-data? (:player decklist)) {:type :player-data
                                                                           :id   :missing-player-data})
                        (when (< (get-in decklist [:count :main]) 60) {:type :maindeck
                                                                       :id   :deck-error-maindeck})
                        (when (> (get-in decklist [:count :side]) 15) {:type :sideboard
                                                                       :id   :deck-error-sideboard})]
                       (for [[card quantity] cards
                             :when (not (basic? card))
                             :when (> quantity 4)]
                         {:type :card-over-4
                          :id   (str "deck-error-card--" card)
                          :card card
                          :text :card-over-4})
                       (for [{:keys [error name]} all-cards
                             :when error]
                         {:type :other
                          :id   (str "other-error-card--" name)
                          :text error
                          :card name}))]
    (filter some? errors)))

(defn cards-with-error [decklist]
  (into {}
        (comp (filter #(= :card-over-4 (:type %)))
              (map (juxt :card :text)))
        (decklist-errors decklist)))

(def warning-icon (styles/styled warning (fn [{:keys [theme]}]
                                           {:color          (get-in theme [:palette :error :main])
                                            :vertical-align :top})))

(defn error-icon [error]
  (let [icon [warning-icon {:title error}]]
    (if error
      [tooltip {:label error}
       icon]
      icon)))

(defn decklist-table-row [board card error]
  (let [translate (subscribe [::subs/translate])
        on-change (wrap-on-change #(dispatch [::events/set-quantity board (:id card) %]))
        on-delete #(dispatch [::events/remove-card board (:id card)])]
    (fn decklist-table-row-render [_ card error]
      (let [translate @translate]
        [:div.deck-table-container-row
         [:div.table-cell.quantity
          (when (:quantity card)
            (into [ui/select {:value     (:quantity card)
                              :on-change on-change
                              :style     {:width      "48px"
                                          :margin-top "6px"}}]
                  (for [i (range 1 (if (basic? (:name card))
                                     31
                                     5))]
                    [ui/menu-item {:value i
                                   :style {:min-height "32px"}}
                     (str i)])))]
         [:div.table-cell.card
          (:name card)]
         (when error
           [:div.table-cell.error
            [error-icon (translate (str "submit.error." (name error)))]])
         [:div.table-cell.actions
          [ui/icon-button {:on-click on-delete}
           [delete]]]]))))

(defn table-body-by-type [decklist board error-cards translate]
  (mapcat (fn [type]
            (when-let [cards (get-in decklist [board type])]
              (list*
               ^{:key (str (name type) "--header")}
               [:div.deck-table-container-row.deck-table-container-header
                [:div.table-cell.quantity]
                [:div.table-cell.card
                 (translate (str "card-type." (name type)))]]
               (for [{:keys [id name error] :as card} cards]
                 ^{:key (str id "--tr")}
                 [decklist-table-row board card (or error (get error-cards name))]))))
          card-types))

(defn decklist-table [decklist board]
  (let [translate (subscribe [::subs/translate])]
    (fn decklist-table-render [decklist board]
      (let [translate @translate
            error-cards (cards-with-error @decklist)]
        [:div.deck-table-container
         [:h3 (if (= :main board)
                (str "Main deck (" (get-in @decklist [:count :main]) ")")
                (str "Sideboard (" (get-in @decklist [:count :side]) ")"))]
         [:div.deck-table-container-rows
          [:div.deck-table-container-row.deck-table-container-header
           [:div.table-cell.quantity
            (translate :submit.quantity)]
           [:div.table-cell.card
            (translate :submit.card)]]
          (case board
            :main (table-body-by-type @decklist :main error-cards translate)
            :side (for [{:keys [id name error] :as card} (:side @decklist)]
                    ^{:key (str id "--tr")}
                    [decklist-table-row :side card (or error (get error-cards name))]))]]))))

(defn error-list [errors]
  (let [translate (subscribe [::subs/translate])]
    (fn error-list-render [errors]
      (let [translate @translate]
        [ui/list
         (for [{:keys [id text card] :as error} errors
               :let [error-text (if (string? text)
                                  text
                                  (translate (str "submit.error." (name (or text id)))))]]
           ^{:key (str "error--" (name (:id error)))}
           [ui/list-item
            [ui/list-item-icon
             [error-icon nil]]
            [ui/list-item-text {:primary (str error-text
                                              (when card
                                                (str ": " card)))}]])]))))

(defn decklist-submit-form [tournament decklist]
  (let [select-main #(dispatch [::events/select-board :main])
        select-side #(dispatch [::events/select-board :side])
        saving? (subscribe [::subs/saving?])
        saved? (subscribe [::subs/saved?])
        error? (subscribe [::subs/error :save-decklist])
        page (subscribe [::common-subs/page])
        translate (subscribe [::subs/translate])
        save-decklist #(dispatch [::events/save-decklist (:id @tournament)])]
    (fn decklist-submit-form-render [tournament decklist]
      (let [translate @translate
            errors (decklist-errors @decklist)]
        [:div
         [:h3
          (translate :submit.decklist)]
         [input]
         [ui/button-group {:variant :outlined
                           :style   {:width          "140px"
                                     :vertical-align :bottom}}
          [ui/button {:on-click select-main
                      :color    (when (= :main (:board @decklist))
                                  :primary)
                      :variant  (when (= :main (:board @decklist))
                                  :contained)}
           "Main"]
          [ui/button {:on-click select-side
                      :color    (when (= :side (:board @decklist))
                                  :primary)
                      :variant  (when (= :side (:board @decklist))
                                  :contained)}
           "Side"]]
         [:div
          [decklist-table decklist :main]
          [decklist-table decklist :side]]
         [decklist-import]
         [:h3
          (translate :submit.player-info)]
         [player-info]
         [ui/button {:on-click save-decklist
                     :variant  :contained
                     :color    :primary
                     :disabled (boolean (or @saving? (seq errors)))
                     :style    {:margin-top "24px"}}
          (translate :submit.save.button)]
         (when @saving?
           [ui/circular-progress
            {:size  36
             :style {:margin         "24px 0 0 24px"
                     :vertical-align :top}}])
         (when @saved?
           (let [url (str (get-host) (routes/old-decklist-path {:id (:id @page)}))]
             [:div.success-notice
              [:h4
               (translate :submit.save.success.header)]
              [:p
               (translate :submit.save.success.info.0)
               [:a {:href url}
                url]
               (translate :submit.save.success.info.1)]]))
         (when @error?
           [:div.error-notice
            [:h4
             (translate :submit.save.error.header)]
            [:p
             (translate :submit.save.error.info)]
            [:pre
             (->text @decklist)]])
         [error-list errors]]))))

(defn decklist-submit []
  (let [tournament (subscribe [::subs/tournament])
        decklist (subscribe [::subs/decklist-by-type])
        translate (subscribe [::subs/translate])
        deadline-gone? (atom false)
        update-deadline (fn update-deadline []
                          (if (time/after? (time/now) (:deadline @tournament))
                            (reset! deadline-gone? true)
                            (.setTimeout js/window update-deadline 1000)))]
    (update-deadline)
    (fn decklist-submit-render []
      (let [translate @translate
            until-deadline (util/interval (time/now) (:deadline @tournament))]
        [:div#decklist-submit
         [:div {:class (when @deadline-gone? :no-print)}
          [language-selector]
          [:h2.top-header (translate :submit.header)]
          [:p.intro
           (translate :submit.intro.0)
           [:span.tournament-name
            (:name @tournament)]
           (translate :submit.intro.1)
           [:span.tournament-date
            (format-date (:date @tournament))]
           (translate :submit.intro.2)
           [:span.tournament-format
            (case (:format @tournament)
              :standard "Standard"
              :modern "Modern"
              :legacy "Legacy")]
           "."]
          [:p.intro
           (translate :submit.intro.3)
           [:span.tournament-deadline
            (format-date-time (:deadline @tournament))]
           ". "
           (translate :submit.time-until-deadline
                      (:days until-deadline) (:hours until-deadline) (:minutes until-deadline))]
          (if-not @deadline-gone?
            [decklist-submit-form tournament decklist]
            [:div
             [:p.deadline-gone
              (translate :submit.deadline-gone)]
             (when (:id @decklist)
               [:h3
                (translate :submit.your-decklist)])])]
         (when (and @deadline-gone? (:id @decklist))
           [render-decklist @decklist @tournament translate])]))))
