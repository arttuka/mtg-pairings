(ns mtg-pairings-server.components.decklist.organizer
  (:require [reagent.core :as reagent :refer [atom]]
            [re-frame.core :refer [subscribe dispatch]]
            [cljsjs.material-ui]
            [cljs-react-material-ui.reagent :as ui]
            [cljs-react-material-ui.icons :as icons]
            [cljs-time.coerce :as coerce]
            [oops.core :refer [oget]]
            [mtg-pairings-server.events.decklist :as events]
            [mtg-pairings-server.routes.decklist :as routes]
            [mtg-pairings-server.subscriptions.decklist :as subs]
            [mtg-pairings-server.util :refer [format-date format-date-time to-local-date indexed]]
            [mtg-pairings-server.util.material-ui :refer [text-field]]))

(def table-header-style {:color       :black
                         :font-weight :bold
                         :font-size   "16px"
                         :height      "36px"})

(defn list-submit-link [tournament-id]
  (let [submit-url (routes/new-decklist-submit-path {:id tournament-id})]
    [:a {:href   submit-url
         :target :_blank}
     (str "https://pairings.fi" submit-url)]))

(defn tournament-row [tournament]
  (let [column-style {:font-size "14px"}
        edit-url (routes/decklist-organizer-tournament-path {:id (:id tournament)})
        link-props {:href     edit-url
                    :on-click #(dispatch [::events/load-organizer-tournament (:id tournament)])}]
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
      [list-submit-link (:id tournament)]]]))

(defn all-tournaments []
  (let [tournaments (subscribe [::subs/organizer-tournaments])]
    (fn all-tournaments-render []
      [:div#decklist-organizer-tournaments
       [ui/raised-button {:href  (routes/decklist-organizer-new-tournament-path)
                          :label "Uusi turnaus"
                          :icon  (reagent/as-element [icons/content-add
                                                      {:style {:height         "36px"
                                                               :width          "30px"
                                                               :padding        "6px 0 6px 6px"
                                                               :vertical-align :top}}])}]
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
                            :on-click #(dispatch [::events/load-decklist (:id decklist)])}]]
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
  (let [saved-tournament (subscribe [::subs/organizer-tournament])
        saving? (subscribe [::subs/saving?])
        tournament (atom @saved-tournament)
        set-name #(swap! tournament assoc :name %)
        set-date (fn [_ date]
                   (swap! tournament assoc :date (to-local-date date)))
        set-format (fn [_ _ format]
                     (swap! tournament assoc :format (keyword format)))
        save-tournament #(dispatch [::events/save-tournament
                                    (select-keys @tournament [:id :name :format :date :deadline])])
        selected-decklists (atom [])
        on-select (fn [selection]
                    (let [selection (js->clj selection)]
                      (reset! selected-decklists (case selection
                                                   "all" (:decklist @tournament)
                                                   "none" []
                                                   (map (:decklist @tournament) selection)))))
        load-selected-decklists #(dispatch [::events/load-decklists
                                            (map :id @selected-decklists)])]
    (fn tournament-render [id]
      (when (and (nil? @tournament)
                 (some? @saved-tournament))
        (reset! tournament @saved-tournament))
      [:div#decklist-organizer-tournament
       [:div.tournament-info
        [:div.field
         [text-field {:on-change           set-name
                      :floating-label-text "Turnauksen nimi"
                      :value               (:name @tournament "")}]]
        [:div.field
         [ui/select-field {:on-change           set-format
                           :value               (:format @tournament)
                           :floating-label-text "Formaatti"}
          [ui/menu-item {:value        :standard
                         :primary-text "Standard"}]
          [ui/menu-item {:value        :modern
                         :primary-text "Modern"}]
          [ui/menu-item {:value        :legacy
                         :primary-text "Legacy"}]]]
        [:div.field
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
        [:div.link
         (when id
           [:p
            "Listojen lähetyssivu: "
            [list-submit-link id]])]
        [:div.buttons
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
        tournament (subscribe [::subs/organizer-tournament])]
    (fn view-decklist-render []
      [render-decklist @decklist @tournament])))

(defn view-decklists []
  (let [decklists (subscribe [::subs/decklists])
        tournament (subscribe [::subs/organizer-tournament])
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

(defn ^:private no-op [])

(defn login []
  [:div#decklist-organizer-login
   [:p "Kirjaudu sisään MtgSuomi-tunnuksillasi."]
   [:form {:action (str "/login?next=" (oget js/window "location" "pathname"))
           :method :post}
    [:input {:type  :hidden
             :name  :__anti-forgery-token
             :value (oget js/window "csrf_token")}]
    [text-field {:name                :username
                 :floating-label-text "Käyttäjätunnus"
                 :on-change           no-op}]
    [text-field {:name                :password
                 :type                :password
                 :floating-label-text "Salasana"
                 :on-change           no-op}]
    [ui/raised-button {:type    :submit
                       :label   "Kirjaudu"
                       :primary true}]]])