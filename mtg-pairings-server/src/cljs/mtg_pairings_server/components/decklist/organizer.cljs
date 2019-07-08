(ns mtg-pairings-server.components.decklist.organizer
  (:require [reagent.core :as reagent :refer [atom]]
            [re-frame.core :refer [subscribe dispatch]]
            [cljsjs.material-ui]
            [cljs-react-material-ui.reagent :as ui]
            [cljs-react-material-ui.icons :as icons]
            [cljs-time.coerce :as coerce]
            [clojure.string :as str]
            [oops.core :refer [oget]]
            [mtg-pairings-server.events.decklist :as events]
            [mtg-pairings-server.routes.decklist :as routes]
            [mtg-pairings-server.subscriptions.decklist :as subs]
            [mtg-pairings-server.styles.common :refer [palette]]
            [mtg-pairings-server.util :refer [format-date format-date-time to-local-date indexed get-host]]
            [mtg-pairings-server.util.material-ui :refer [text-field]]))

(defn header []
  (let [logged-in? (subscribe [::subs/user])
        button-style {:margin-left  "12px"
                      :margin-right "12px"}]

    (fn header-render []
      (let [disabled? (not @logged-in?)]
        [ui/toolbar {:class-name :decklist-organizer-header
                     :style      {:background-color (palette :primary1-color)}}
         [ui/toolbar-group {:first-child true}
          [ui/raised-button {:href     (routes/organizer-path)
                             :label    "Kaikki turnaukset"
                             :style    button-style
                             :disabled disabled?}]
          [ui/raised-button {:href     (routes/organizer-new-tournament-path)
                             :label    "Uusi turnaus"
                             :icon     (reagent/as-element [icons/content-add
                                                            {:style {:height         "36px"
                                                                     :width          "30px"
                                                                     :padding        "6px 0 6px 6px"
                                                                     :vertical-align :top}}])
                             :style    button-style
                             :disabled disabled?}]]
         [ui/toolbar-group {:last-child true}
          [ui/raised-button {:href     "/logout"
                             :label    "Kirjaudu ulos"
                             :style    button-style
                             :disabled disabled?}]]]))))

(def table-header-style {:color       :black
                         :font-weight :bold
                         :font-size   "16px"
                         :height      "36px"})

(defn list-submit-link [tournament-id]
  (let [submit-url (routes/new-decklist-path {:id tournament-id})]
    [:a {:href   submit-url
         :target :_blank}
     (str (get-host) submit-url)]))

(defn tournament-row [tournament]
  (let [column-style {:font-size "14px"}
        link-props {:href (routes/organizer-tournament-path {:id (:id tournament)})}]
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

(defn sortable-header [{:keys [class-name column]} & children]
  (let [sort-data (subscribe [::subs/decklist-sort])]
    (fn sortable-header-render [{:keys [class-name column]} & children]
      (let [selected? (= column (:key @sort-data))
            icon (if (and selected?
                          (not (:ascending @sort-data)))
                   icons/hardware-keyboard-arrow-up
                   icons/hardware-keyboard-arrow-down)
            style (cond-> (assoc table-header-style :cursor :pointer)
                    selected? (assoc :color (:accent1-color palette)))]
        [ui/table-header-column {:class-name class-name
                                 :style      style
                                 :on-click   #(dispatch [::events/sort-decklists column])}
         [icon {:style {:vertical-align :baseline
                        :position       :absolute
                        :left           0
                        :color          nil}}]
         children]))))

(defn decklist-table [decklists selected-decklists on-select]
  [ui/table {:class-name        :tournaments
             :multi-selectable  true
             :on-row-selection  on-select
             :all-rows-selected (= (count decklists) (count selected-decklists))}
   [ui/table-header
    [ui/table-row {:style {:height "24px"}}
     [ui/table-header-column {:class-name :dci
                              :style      table-header-style}
      "DCI"]
     [sortable-header {:class-name :name
                       :column     :name}
      "Nimi"]
     [sortable-header {:class-name :submitted
                       :column     :submitted}
      "Lähetetty"]]]
   [ui/table-body
    {:deselect-on-clickaway false}
    (for [decklist decklists
          :let [column-style {:font-size "14px"
                              :padding   0}
                decklist-url (routes/organizer-view-path {:id (:id decklist)})
                link-props {:href     decklist-url
                            :on-click #(dispatch [::events/load-decklist (:id decklist)])}]]
      ^{:key (str (:id decklist) "--row")}
      [ui/table-row {:selected (contains? selected-decklists (:id decklist))}
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

(defn notice [type text]
  (let [[color background] (case type
                             :success [:black (:primary1-color palette)]
                             :error [:white (:error-color palette)])
        display? (atom true)
        on-delete #(reset! display? false)]
    (fn notice-render [type text]
      (when @display?
        [ui/chip
         {:background-color  background
          :label-color       color
          :on-request-delete on-delete}
         text]))))

(defn notices []
  (let [saved? (subscribe [::subs/saved?])
        error? (subscribe [::subs/error :save-tournament])]
    (fn notices-render []
      [:div.notices
       (when @saved?
         [notice :success "Tallennus onnistui"])
       (when @error?
         [notice :error "Tallennus epäonnistui"])])))

(defn tournament [id]
  (let [saved-tournament (subscribe [::subs/organizer-tournament])
        decklists (subscribe [::subs/organizer-decklists])
        saving? (subscribe [::subs/saving?])
        tournament (atom @saved-tournament)
        set-name #(swap! tournament assoc :name %)
        set-date (fn [_ date]
                   (swap! tournament assoc :date (to-local-date date)))
        set-format (fn [_ _ format]
                     (swap! tournament assoc :format (keyword format)))
        save-tournament #(dispatch [::events/save-tournament
                                    (select-keys @tournament [:id :name :format :date :deadline])])
        selected-decklists (atom #{})
        on-select (fn [selection]
                    (let [selection (js->clj selection)]
                      (reset! selected-decklists
                              (into #{} (case selection
                                          "all" (map :id @decklists)
                                          "none" []
                                          (map (comp :id @decklists) selection))))))
        load-selected-decklists #(dispatch [::events/load-decklists @selected-decklists])]
    (fn tournament-render [id]
      (when (and (nil? @tournament)
                 (some? @saved-tournament))
        (reset! tournament @saved-tournament))
      [:div#decklist-organizer-tournament
       [:div.tournament-info
        [:div.fields
         [:div.field
          (let [value (:name @tournament "")]
            [text-field {:on-change           set-name
                         :floating-label-text "Turnauksen nimi"
                         :value               value
                         :error-text          (when (str/blank? value)
                                                "Nimi on pakollinen")}])]
         [:div.field
          (let [value (:format @tournament)]
            [ui/select-field {:on-change           set-format
                              :value               value
                              :floating-label-text "Formaatti"
                              :error-text          (when-not value
                                                     "Formaatti on pakollinen")}
             [ui/menu-item {:value        :standard
                            :primary-text "Standard"}]
             [ui/menu-item {:value        :modern
                            :primary-text "Modern"}]
             [ui/menu-item {:value        :legacy
                            :primary-text "Legacy"}]])]
         [:div.field
          (let [value (some-> (:date @tournament)
                              (coerce/to-long)
                              (js/Date.))]
            [ui/date-picker {:value                  value
                             :error-text             (when-not value
                                                       "Päivämäärä on pakollinen")
                             :on-change              set-date
                             :container              :inline
                             :dialog-container-style {:left "-9999px"}
                             :floating-label-text    "Päivämäärä"
                             :locale                 "fi-FI"
                             :auto-ok                true
                             :DateTimeFormat         (oget js/Intl "DateTimeFormat")}])]]
        [:div.link
         (when id
           [:p
            "Listojen lähetyssivu: "
            [list-submit-link id]])]
        [:div.buttons
         [ui/raised-button {:label    "Tallenna"
                            :on-click save-tournament
                            :primary  true
                            :disabled (or @saving?
                                          (str/blank? (:name @tournament))
                                          (nil? (:format @tournament))
                                          (nil? (:date @tournament)))
                            :style    {:width "200px"}}]
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
         [notices]
         [:br]
         [ui/raised-button {:label    "Tulosta valitut listat"
                            :href     (routes/organizer-print-path)
                            :on-click load-selected-decklists
                            :primary  true
                            :disabled (empty? @selected-decklists)
                            :style    {:margin-top "12px"
                                       :width      "200px"}}]]]
       [:div.decklists
        [decklist-table @decklists @selected-decklists on-select]]])))

(defn decklist-card [card]
  [:div.card
   [:div.quantity
    (:quantity card)]
   [:div.name
    (:name card)]])

(defn render-decklist [decklist tournament]
  (let [{:keys [player main side id], counts :count} decklist
        {:keys [last-name first-name dci deck-name]} player
        [l1 l2 l3] last-name
        dci (vec dci)
        {tournament-id :id, tournament-name :name, date :date} tournament]
    [:div.organizer-decklist
     [:div.first-letters
      [:div.label "Alkukirjaimet"]
      [:div.letter l1]
      [:div.letter l2]
      [:div.letter l3]]
     [:div.deck-info
      [:div.tournament-date
       [:div.label "Päivä:"]
       [:div.value (format-date date)]]
      [:div.tournament-name
       [:div.label "Turnaus:"]
       [:div.value
        [:a {:href (routes/organizer-tournament-path {:id tournament-id})}
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
     [:div.decklists
      [:div.maindeck
       [:h3 "Maindeck (" (:main counts) ")"]
       [:div.cards
        (for [card main]
          ^{:key (str id "--main--" (:name card))}
          [decklist-card card])]]
      [:div.sideboard
       [:h3 "Sideboard (" (:side counts) ")"]
       [:div.cards
        (for [card side]
          ^{:key (str id "--side--" (:name card))}
          [decklist-card card])]]]]))

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
