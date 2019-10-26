(ns mtg-pairings-server.components.decklist.submit
  (:require [reagent.core :as reagent :refer [atom]]
            [reagent.ratom :refer [make-reaction]]
            [re-frame.core :refer [subscribe dispatch]]
            [reagent-material-ui.components :as ui]
            [reagent-material-ui.styles :refer [with-styles]]
            [cljs-time.core :as time]
            [mtg-pairings-server.components.autocomplete :refer [autocomplete]]
            [mtg-pairings-server.components.decklist.decklist-import :refer [decklist-import]]
            [mtg-pairings-server.components.decklist.decklist-table :refer [decklist-table]]
            [mtg-pairings-server.components.decklist.icons :refer [error-icon]]
            [mtg-pairings-server.components.decklist.player-info :refer [player-info]]
            [mtg-pairings-server.components.decklist.print :refer [render-decklist]]
            [mtg-pairings-server.components.language-selector :refer [language-selector]]
            [mtg-pairings-server.events.decklist :as events]
            [mtg-pairings-server.routes.decklist :as routes]
            [mtg-pairings-server.subscriptions.common :as common-subs]
            [mtg-pairings-server.subscriptions.decklist :as subs]
            [mtg-pairings-server.util :refer [debounce format-date format-date-time get-host]]
            [mtg-pairings-server.util.decklist :refer [->text card-types decklist-errors]]
            [mtg-pairings-server.util.material-ui :as mui-util]
            [mtg-pairings-server.util :as util]))

(defn input-styles [{:keys [spacing] :as theme}]
  (let [on-desktop (mui-util/on-desktop theme)
        on-mobile (mui-util/on-mobile theme)]
    {:autocomplete-container {:flex 1}
     :menu-container         {:z-index 2}
     :button-group           {:flex "0 0 140px"}
     :container              {:display     :flex
                              :align-items "flex-end"
                              :padding     (spacing 0 1)
                              on-mobile    {:width "100%"}
                              on-desktop   {:width 400}}}))

(defn board-button [{:keys [label on-click selected?]}]
  [ui/button {:on-click on-click
              :color    (if selected? :primary :default)
              :variant  (if selected? :contained :outlined)}
   label])

(defn input* [props]
  (let [tournament (subscribe [::subs/tournament])
        translate (subscribe [::subs/translate])
        suggestions (atom [])
        on-select #(do (dispatch [::events/add-card %])
                       (reset! suggestions []))
        fetch-suggestions (debounce (fn [prefix]
                                      (dispatch [::events/card-suggestions
                                                 prefix
                                                 (:format @tournament)
                                                 #(reset! suggestions %)]))
                                    250)
        clear-suggestions #(reset! suggestions [])
        select-main #(dispatch [::events/select-board :main])
        select-side #(dispatch [::events/select-board :side])]
    (fn input-render [{:keys [classes selected-board]}]
      (let [translate @translate]
        [:div {:class (:container classes)}
         [autocomplete {:classes            {:container      (:autocomplete-container classes)
                                             :menu-container (:menu-container classes)}
                        :fetch-suggestions  fetch-suggestions
                        :clear-suggestions  clear-suggestions
                        :label              (translate :submit.add-card)
                        :suggestion->string (fn [item] (:name item ""))
                        :suggestions        suggestions
                        :on-select          on-select}]
         [ui/button-group {:classes    {:root (:button-group classes)}
                           :variant    :outlined
                           :full-width true}
          (board-button {:on-click  select-main
                         :selected? (= "main" selected-board)
                         :label     "Main"})
          (board-button {:on-click  select-side
                         :selected? (= "side" selected-board)
                         :label     "Side"})]]))))

(def input ((with-styles input-styles) input*))

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
  (let [saving? (subscribe [::subs/saving?])
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
         [input {:selected-board (:board @decklist)}]

         [:div
          [decklist-table {:board :main}]
          [decklist-table {:board :side}]]
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
