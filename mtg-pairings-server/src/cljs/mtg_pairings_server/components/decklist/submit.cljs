(ns mtg-pairings-server.components.decklist.submit
  (:require [reagent.core :as reagent :refer [atom]]
            [reagent.ratom :refer [make-reaction]]
            [re-frame.core :refer [subscribe dispatch]]
            [clojure.string :as str]
            [cljsjs.material-ui]
            [cljs-react-material-ui.reagent :as ui]
            [cljs-react-material-ui.icons :as icons]
            [cljs-time.core :as time]
            [oops.core :refer [oget]]
            [mtg-pairings-server.components.autosuggest :refer [autosuggest]]
            [mtg-pairings-server.components.decklist.print :refer [render-decklist]]
            [mtg-pairings-server.components.language-selector :refer [language-selector]]
            [mtg-pairings-server.components.tooltip :refer [tooltip]]
            [mtg-pairings-server.events.decklist :as events]
            [mtg-pairings-server.routes.decklist :as routes]
            [mtg-pairings-server.styles.common :as styles]
            [mtg-pairings-server.subscriptions.common :as common-subs]
            [mtg-pairings-server.subscriptions.decklist :as subs]
            [mtg-pairings-server.util :refer [debounce format-date format-date-time index-where get-host valid-email?]]
            [mtg-pairings-server.util.decklist :refer [->text card-types]]
            [mtg-pairings-server.util.mtg :refer [valid-dci?]]
            [mtg-pairings-server.util.material-ui :refer [text-field]]
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
                      :floating-label-text            (translate :submit.add-card)
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

(defn error-icon [error]
  (let [icon [icons/alert-warning {:title error
                                   :style {:color          (:error-color styles/palette)
                                           :vertical-align :top}}]]
    (if error
      [tooltip {:label error}
       icon]
      icon)))

(defn decklist-table-row [board card error]
  (let [translate (subscribe [::subs/translate])
        on-change (fn [_ _ quantity]
                    (dispatch [::events/set-quantity board (:id card) quantity]))
        on-delete #(dispatch [::events/remove-card board (:id card)])]
    (fn decklist-table-row-render [_ card error]
      (let [translate @translate]
        [:div.deck-table-container-row
         [:div.table-cell.quantity
          (when (:quantity card)
            (into [ui/select-field {:value           (:quantity card)
                                    :on-change       on-change
                                    :style           {:width          "48px"
                                                      :vertical-align :top}
                                    :menu-style      {:width "48px"}
                                    :icon-style      {:padding-left  0
                                                      :padding-right 0
                                                      :width         "24px"
                                                      :fill          "rgba(0, 0, 0, 0.54)"}
                                    :underline-style {:border-color "rgba(0, 0, 0, 0.24)"}
                                    :label-style     {:padding-right "30px"
                                                      :text-align    :right}}]
                  (for [i (range 1 (if (basic? (:name card))
                                     31
                                     5))]
                    [ui/menu-item {:value           i
                                   :primary-text    i
                                   :inner-div-style {:padding "0 6px"}}])))]
         [:div.table-cell.card
          (:name card)]
         (when error
           [:div.table-cell.error
            [error-icon (translate (str "submit.error." (name error)))]])
         [:div.table-cell.actions
          [ui/icon-button {:on-click on-delete}
           [icons/action-delete]]]]))))

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
      (.log js/console "rendering decklist-table" @decklist)
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

(defn player-info [player]
  (let [set-first-name #(dispatch [::events/update-player-info :first-name %])
        set-last-name #(dispatch [::events/update-player-info :last-name %])
        set-deck-name #(dispatch [::events/update-player-info :deck-name %])
        set-email #(dispatch [::events/update-player-info :email %])
        set-dci #(dispatch [::events/update-player-info :dci %])
        translate (subscribe [::subs/translate])]
    (fn player-info-render [_]
      (let [translate @translate]
        [:div#player-info
         [:div.full-width
          [text-field {:on-change           set-deck-name
                       :floating-label-text (translate :submit.deck-name)
                       :full-width          true
                       :value               (:deck-name @player)
                       :style               {:vertical-align :top}}]]
         [:div.half-width.left
          (let [value (:first-name @player)]
            [text-field {:on-change           set-first-name
                         :floating-label-text (translate :submit.first-name)
                         :full-width          true
                         :value               value
                         :error-text          (when (str/blank? value)
                                                (translate :submit.error.first-name))
                         :style               {:vertical-align :top}}])]
         [:div.half-width.right
          (let [value (:last-name @player)]
            [text-field {:on-change           set-last-name
                         :floating-label-text (translate :submit.last-name)
                         :full-width          true
                         :value               value
                         :error-text          (when (str/blank? value)
                                                (translate :submit.error.last-name))
                         :style               {:vertical-align :top}}])]
         [:div.half-width.left
          (let [value (:dci @player)]
            [text-field {:on-change           set-dci
                         :floating-label-text (translate :submit.dci)
                         :full-width          true
                         :value               value
                         :error-text          (when-not (valid-dci? value)
                                                (translate :submit.error.dci))
                         :style               {:vertical-align :top}}])]
         [:div.half-width.right
          (let [value (:email @player)]
            [text-field {:on-change           set-email
                         :floating-label-text (translate :submit.email)
                         :full-width          true
                         :value               value
                         :error-text          (when-not (or (str/blank? value)
                                                            (valid-email? value))
                                                (translate :submit.error.email))
                         :style               {:vertical-align :top}
                         :disabled            (:email-disabled? @player)
                         :title               (when (:email-disabled? @player)
                                                (translate :submit.email-disabled))}])]]))))

(defn valid-code [address]
  (when address
    (let [[_ code] (re-find #"/([A-z0-9_-]{22})$" address)]
      code)))

(defn decklist-import []
  (let [mobile? (subscribe [::common-subs/mobile?])
        loaded? (subscribe [::subs/loaded?])
        translate (subscribe [::subs/translate])
        selected (atom nil)
        on-active (fn [tab]
                    (let [value (oget tab "props" "value")]
                      (reset! selected (when (not= value @selected)
                                         value))))
        address (atom "")
        code (make-reaction #(valid-code @address))
        address-on-change #(reset! address %)
        import-from-address #(dispatch [::events/import-address @code])
        address-on-key-press (fn [e]
                               (when (= "Enter" (.-key e))
                                 (import-from-address)))
        decklist (atom "")
        decklist-on-change #(reset! decklist %)
        import-decklist (fn []
                          (dispatch [::events/import-text @decklist])
                          (reset! selected nil))
        import-error (subscribe [::subs/error :import-address])
        border (styles/border "1px" :solid (styles/palette :light-grey))]
    (add-watch loaded? ::decklist-import
               (fn [_ _ _ new]
                 (when new
                   (reset! selected nil)
                   (reset! address ""))))
    (fn decklist-import-render []
      (let [translate @translate]
        [:div.decklist-import
         [ui/tabs {:value                   @selected
                   :content-container-style (when @selected
                                              {:border-bottom border
                                               :border-left   (when-not @mobile? border)
                                               :border-right  (when-not @mobile? border)})}
          [ui/tab {:label     (translate :submit.load-previous.label)
                   :value     "load-previous"
                   :on-active on-active
                   :style     {:color :black}}
           [:div.info
            [:h3
             (translate :submit.load-previous.label)]
            [:p
             (translate :submit.load-previous.text.0)
             [:span.address "https://decklist.pairings.fi/abcd..."]
             (translate :submit.load-previous.text.1)]]
           [:div.form
            [:div.text-field-container
             [text-field {:on-change           address-on-change
                          :on-key-press        address-on-key-press
                          :floating-label-text (translate :submit.load-previous.address)
                          :full-width          true
                          :error-text          (when-not (or (str/blank? @address)
                                                             @code)
                                                 (translate :submit.error.address))}]]
            [:br]
            [ui/raised-button {:label    (translate :submit.load)
                               :disabled (nil? @code)
                               :on-click import-from-address}]
            (when @import-error
              [:p.decklist-import-error
               (translate (case @import-error
                            :not-found :submit.error.not-found
                            :submit.error.decklist-import-error))])]]
          [ui/tab {:label     (translate :submit.load-text.label)
                   :value     "load-text"
                   :on-active on-active
                   :style     {:color :black}}
           [:div.info
            [:h3
             (translate :submit.load-text.header)]
            [:p
             (translate :submit.load-text.info.0)]
            [:pre
             "4 Lightning Bolt\n4 Chain Lightning\n..."]
            [:p
             (translate :submit.load-text.info.1)]]
           [:div.form
            [text-field {:on-change      decklist-on-change
                         :multi-line     true
                         :rows           7
                         :rows-max       7
                         :textarea-style {:background-color "rgba(0, 0, 0, 0.05)"}
                         :full-width     true
                         :name           :text-decklist}]
            [:br]
            [ui/raised-button {:label    (translate :submit.load)
                               :disabled (str/blank? @decklist)
                               :on-click import-decklist}]]]]]))))

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
           [ui/list-item {:primary-text (str error-text
                                             (when card
                                               (str ": " card)))
                          :left-icon    (reagent/as-element [:div
                                                             [error-icon nil]])}])]))))

(defn decklist-submit-form [tournament decklist]
  (let [player (reagent/cursor decklist [:player])
        select-main #(dispatch [::events/select-board :main])
        select-side #(dispatch [::events/select-board :side])
        button-style {:position  :relative
                      :top       "-5px"
                      :width     "70px"
                      :min-width "70px"}
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
         [:h3
          (translate :submit.player-info)]
         [player-info player]
         [ui/raised-button
          {:label    (translate :submit.save.button)
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
