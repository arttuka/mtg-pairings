(ns mtg-pairings-server.components.decklist.organizer
  (:require [reagent.core :as reagent :refer [atom]]
            [re-frame.core :refer [subscribe dispatch]]
            [cljsjs.material-ui]
            [cljs-react-material-ui.reagent :as ui]
            [cljs-time.coerce :as coerce]
            [oops.core :refer [oget]]
            [mtg-pairings-server.events :as events]
            [mtg-pairings-server.routes :as routes]
            [mtg-pairings-server.subscriptions :as subs]
            [mtg-pairings-server.util :refer [format-date format-date-time to-local-date indexed]]
            [mtg-pairings-server.util.material-ui :refer [text-field]]))

(def table-header-style {:color       :black
                         :font-weight :bold
                         :font-size   "16px"
                         :height      "36px"})

(defn tournament-row [tournament]
  (let [column-style {:font-size "14px"}
        edit-url (routes/decklist-organizer-tournament-path {:id (:id tournament)})
        submit-url (routes/new-decklist-submit-path {:id (:id tournament)})
        link-props {:href     edit-url
                    :on-click #(dispatch [::events/load-decklist-organizer-tournament (:id tournament)])}]
    [ui/table-row
     [ui/table-row-column {:class-name :date
                           :style      column-style}
      [:a.tournament-link link-props
       (format-date (:date tournament))]]
     [ui/table-row-column {:class-name :deadline
                           :style      column-style}
      [:a.tournament-link link-props
       (format-date-time (:deadline tournament))]]
     [ui/table-row-column {:class-name :name
                           :style      column-style}
      [:a.tournament-link link-props
       (:name tournament)]]
     [ui/table-row-column {:class-name :decklists
                           :style      column-style}
      [:a.tournament-link link-props
       (:decklist tournament)]]
     [ui/table-row-column {:class-name :submit-page
                           :style      column-style}
      [:a {:href submit-url}
       (str "https://pairings.fi" submit-url)]]]))

(defn all-tournaments []
  (let [tournaments (subscribe [::subs/decklist-organizer-tournaments])]
    (fn all-tournaments-render []
      [:div#decklist-organizer-tournaments
       [ui/table {:selectable false
                  :class-name :tournaments}
        [ui/table-header {:display-select-all  false
                          :adjust-for-checkbox false}
         [ui/table-row {:style {:height "24px"}}
          [ui/table-header-column {:class-name :date
                                   :style      table-header-style}
           "Päivä"]
          [ui/table-header-column {:class-name :deadline
                                   :style      table-header-style}
           "Deadline"]
          [ui/table-header-column {:class-name :name
                                   :style      table-header-style}
           "Turnaus"]
          [ui/table-header-column {:class-name :decklists
                                   :style      table-header-style}
           "Dekkilistoja"]
          [ui/table-header-column {:class-name :submit-page
                                   :style      table-header-style}
           "Listojen lähetyssivu"]]]
        [ui/table-body
         (for [tournament @tournaments]
           ^{:key (str (:id tournament) "--row")}
           [tournament-row tournament])]]])))

(defn decklist-table [decklists on-select]
  [ui/table {:class-name       :tournaments
             :multi-selectable true
             :on-row-selection on-select}
   [ui/table-header
    [ui/table-row {:style {:height "24px"}}
     [ui/table-header-column {:class-name :dci
                              :style      table-header-style}
      "DCI"]
     [ui/table-header-column {:class-name :name
                              :style      table-header-style}
      "Nimi"]
     [ui/table-header-column {:class-name :submitted
                              :style      table-header-style}
      "Lähetetty"]]]
   [ui/table-body
    {:deselect-on-clickaway false}
    (for [decklist decklists
          :let [column-style {:font-size "14px"
                              :padding   0}
                decklist-url (routes/decklist-organizer-view-path {:id (:id decklist)})
                link-props {:href     decklist-url
                            :on-click #(dispatch [::events/load-organizer-tournament-decklist (:id decklist)])}]]
      ^{:key (str (:id decklist) "--row")}
      [ui/table-row {}
       [ui/table-row-column {:class-name :dci
                             :style      column-style}
        [:a.decklist-link link-props
         (:dci decklist)]]
       [ui/table-row-column {:class-name :name
                             :style      column-style}
        [:a.decklist-link link-props
         (str (:last-name decklist) ", " (:first-name decklist))]]
       [ui/table-row-column {:class-name :submitted
                             :style      column-style}
        [:a.decklist-link link-props
         (format-date-time (:submitted decklist))]]])]])

(defn tournament [id]
  (let [saved-tournament (subscribe [::subs/decklist-organizer-tournament])
        saving? (subscribe [::subs/decklist-saving?])
        tournament (atom @saved-tournament)
        set-name #(swap! tournament assoc :name %)
        set-date (fn [_ date]
                   (swap! tournament assoc :date (to-local-date date)))
        set-format (fn [_ _ format]
                     (swap! tournament assoc :format (keyword format)))
        save-tournament #(dispatch [::events/save-decklist-organizer-tournament
                                    (select-keys @tournament [:id :name :format :date :deadline])])
        selected-decklists (atom [])
        on-select (fn [selection]
                    (let [selection (js->clj selection)]
                      (reset! selected-decklists (case selection
                                                   "all" (:decklist @tournament)
                                                   "none" []
                                                   (map (:decklist @tournament) selection)))))
        load-selected-decklists #(dispatch [::events/load-organizer-tournament-decklists
                                            (map :id @selected-decklists)])]
    (fn tournament-render [id]
      (when (and (nil? @tournament)
                 (some? @saved-tournament))
        (reset! tournament @saved-tournament))
      [:div#decklist-organizer-tournament
       [:div.tournament-info
        [:div
         [text-field {:on-change           set-name
                      :floating-label-text "Turnauksen nimi"
                      :value               (:name @tournament "")}]]
        [:div
         [ui/select-field {:on-change           set-format
                           :value               (:format @tournament)
                           :floating-label-text "Formaatti"}
          [ui/menu-item {:value        :standard
                         :primary-text "Standard"}]
          [ui/menu-item {:value        :modern
                         :primary-text "Modern"}]
          [ui/menu-item {:value        :legacy
                         :primary-text "Legacy"}]]]
        [:div
         [ui/date-picker {:value                  (some-> (:date @tournament)
                                                          (coerce/to-long)
                                                          (js/Date.))
                          :on-change              set-date
                          :container              :inline
                          :dialog-container-style {:left "-9999px"}
                          :floating-label-text    "Päivämäärä"
                          :locale                 "fi-FI"
                          :auto-ok                true
                          :DateTimeFormat         (oget js/Intl "DateTimeFormat")}]]
        [:div
         [ui/raised-button {:label    "Tallenna"
                            :on-click save-tournament
                            :primary  true
                            :disabled @saving?}]
         (if @saving?
           [ui/circular-progress
            {:size  36
             :style {:margin-left    "24px"
                     :margin-right   "24px"
                     :vertical-align :top}}]
           [:div.placeholder
            {:style {:display        :inline-block
                     :width          "84px"
                     :height         "36px"
                     :vertical-align :top}}])
         [ui/raised-button {:label    "Tulosta valitut listat"
                            :href     (routes/decklist-organizer-print-path)
                            :on-click load-selected-decklists
                            :primary  true
                            :disabled (empty? @selected-decklists)}]]]
       [:div.decklists
        [decklist-table (:decklist @tournament) on-select]]])))

(defn decklist-card [card]
  [:div.card
   [:div.quantity
    (:quantity card)]
   [:div.name
    (:name card)]])

(defn render-decklist [decklist tournament]
  (let [{:keys [player main side id], counts :count} decklist
        {:keys [last-name first-name dci deck-name]} player
        dci (vec dci)
        {tournament-id :id, tournament-name :name, date :date} tournament]
    [:div.organizer-decklist
     [:div.first-letters
      [:div.label "Alkukirjaimet"]
      [:div.letter
       (nth last-name 0)]
      [:div.letter
       (nth last-name 1)]
      [:div.letter
       (nth last-name 2)]]
     [:div.deck-info
      [:div.tournament-date
       [:div.label "Päivä:"]
       [:div.value (format-date date)]]
      [:div.tournament-name
       [:div.label "Turnaus:"]
       [:div.value
        [:a {:href (routes/decklist-organizer-tournament-path {:id tournament-id})}
         tournament-name]]]
      [:div.deck-name
       [:div.label "Pakka:"]
       [:div.value deck-name]]]
     [:div.player-info
      [:div.name
       [:div.last-name
        [:div.label "Sukunimi:"]
        [:div.value last-name]]
       [:div.first-name
        [:div.label "Etunimi:"]
        [:div.value first-name]]]
      [:div.dci
       [:div.label "DCI:"]
       [:div.value
        (for [index (range 10)]
          ^{:key (str id "--dci--" index)}
          [:span.digit (get dci index)])]]]
     [:div.maindeck
      [:h3 "Maindeck (" (:main counts) ")"]
      [:div.cards
       (for [card main]
         ^{:key (str id "--main--" (:name card))}
         [decklist-card card])]]
     [:div.sideboard
      {:class (str "sideboard-" (count side))}
      [:h3 "Sideboard (" (:side counts) ")"]
      [:div.cards
       (for [card side]
         ^{:key (str id "--side--" (:name card))}
         [decklist-card card])]]]))

(defn view-decklist []
  (let [decklist (subscribe [::subs/decklist])
        tournament (subscribe [::subs/decklist-organizer-tournament])]
    (fn view-decklist-render []
      [render-decklist @decklist @tournament])))

(defn view-decklists []
  (let [decklists (subscribe [::subs/decklists])
        tournament (subscribe [::subs/decklist-organizer-tournament])
        print-page #(when (and (seq @decklists)
                               @tournament)
                      (.print js/window))]
    (reagent/create-class
     {:component-did-mount  print-page
      :component-did-update print-page
      :reagent-render       (fn view-decklists-render []
                              [:div
                               (doall (for [decklist @decklists]
                                        ^{:key (:id decklist)}
                                        [render-decklist decklist @tournament]))])})))
