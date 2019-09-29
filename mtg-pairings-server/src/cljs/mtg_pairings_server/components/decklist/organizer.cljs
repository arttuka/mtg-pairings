(ns mtg-pairings-server.components.decklist.organizer
  (:require [reagent.core :as reagent :refer [atom]]
            [re-frame.core :refer [subscribe dispatch]]
            [cljsjs.material-ui]
            [cljs-react-material-ui.reagent :as ui]
            [cljs-react-material-ui.icons :as icons]
            [cljs-time.coerce :as coerce]
            [clojure.string :as str]
            [oops.core :refer [oget]]
            [mtg-pairings-server.components.decklist.print :refer [render-decklist]]
            [mtg-pairings-server.components.language-selector :refer [language-selector]]
            [mtg-pairings-server.events.decklist :as events]
            [mtg-pairings-server.routes.decklist :as routes]
            [mtg-pairings-server.subscriptions.decklist :as subs]
            [mtg-pairings-server.styles.common :refer [palette]]
            [mtg-pairings-server.util :refer [format-date format-date-time to-local-date indexed get-host]]
            [mtg-pairings-server.util.material-ui :refer [text-field]]))

(defn header []
  (let [logged-in? (subscribe [::subs/user])
        translate (subscribe [::subs/translate])
        button-style {:margin-left  "12px"
                      :margin-right "12px"}]

    (fn header-render []
      (let [disabled? (not @logged-in?)
            translate @translate]
        [ui/toolbar {:class-name :decklist-organizer-header
                     :style      {:background-color (palette :primary1-color)}}
         [ui/toolbar-group {:first-child true}
          [ui/raised-button {:href     (routes/organizer-path)
                             :label    (translate :organizer.all-tournaments)
                             :style    button-style
                             :disabled disabled?}]
          [ui/raised-button {:href     (routes/organizer-new-tournament-path)
                             :label    (translate :organizer.new-tournament)
                             :icon     (reagent/as-element [icons/content-add
                                                            {:style {:height         "36px"
                                                                     :width          "30px"
                                                                     :padding        "6px 0 6px 6px"
                                                                     :vertical-align :top}}])
                             :style    button-style
                             :disabled disabled?}]]
         [ui/toolbar-group {:last-child true}
          [language-selector]
          [ui/raised-button {:href     "/logout"
                             :label    (translate :organizer.log-out)
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
  (let [column-style {:font-size "14px"
                      :padding   0}
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
                           :style      {:font-size "14px"}}
      [list-submit-link (:id tournament)]]]))

(defn only-upcoming-toggle []
  (let [value (subscribe [::subs/only-upcoming?])
        translate (subscribe [::subs/translate])
        on-toggle (fn [_ value]
                    (dispatch [::events/set-only-upcoming value]))]
    (fn only-upcoming-toggle-render []
      (let [translate @translate]
        [ui/toggle {:toggled        @value
                    :on-toggle      on-toggle
                    :label          (translate :organizer.show-only-upcoming)
                    :label-position :right
                    :thumb-style    {:background-color (palette :light-grey)}
                    :track-style    {:background-color (palette :grey)}}]))))

(defn all-tournaments []
  (let [tournaments (subscribe [::subs/filtered-organizer-tournaments])
        translate (subscribe [::subs/translate])]
    (fn all-tournaments-render []
      (let [translate @translate]
        [:div#decklist-organizer-tournaments
         [only-upcoming-toggle]
         [ui/table {:selectable false
                    :class-name :tournaments}
          [ui/table-header {:display-select-all  false
                            :adjust-for-checkbox false}
           [ui/table-row {:style {:height "24px"}}
            [ui/table-header-column {:class-name :date
                                     :style      table-header-style}
             (translate :organizer.date)]
            [ui/table-header-column {:class-name :deadline
                                     :style      table-header-style}
             (translate :organizer.deadline)]
            [ui/table-header-column {:class-name :name
                                     :style      table-header-style}
             (translate :organizer.tournament.title)]
            [ui/table-header-column {:class-name :decklists
                                     :style      table-header-style}
             (translate :organizer.decklists)]
            [ui/table-header-column {:class-name :submit-page
                                     :style      table-header-style}
             (translate :organizer.submit-page)]]]
          [ui/table-body
           (for [tournament @tournaments]
             ^{:key (str (:id tournament) "--row")}
             [tournament-row tournament])]]]))))

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

(defn decklist-table [decklists selected-decklists on-select translate]
  [ui/table {:class-name        :tournaments
             :multi-selectable  true
             :on-row-selection  on-select
             :all-rows-selected (= (count decklists) (count selected-decklists))}
   [ui/table-header
    [ui/table-row {:style {:height "24px"}}
     [ui/table-header-column {:class-name :dci
                              :style      table-header-style}
      (translate :organizer.dci)]
     [sortable-header {:class-name :name
                       :column     :name}
      (translate :organizer.name)]
     [sortable-header {:class-name :submitted
                       :column     :submitted}
      (translate :organizer.sent)]]]
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
        error? (subscribe [::subs/error :save-tournament])
        translate (subscribe [::subs/translate])]
    (fn notices-render []
      (let [translate @translate]
        [:div.notices
         (when @saved?
           [notice :success (translate :organizer.save.success)])
         (when @error?
           [notice :error (translate :organizer.save.fail)])]))))

(defn date-picker [opts]
  [ui/date-picker (merge {:container              :inline
                          :dialog-container-style {:left "-9999px"}
                          :locale                 "fi-FI"
                          :auto-ok                true
                          :DateTimeFormat         (oget js/Intl "DateTimeFormat")
                          :text-field-style       {:width "128px"}}
                         opts)])

(defn tournament [id]
  (let [saved-tournament (subscribe [::subs/organizer-tournament])
        decklists (subscribe [::subs/organizer-decklists])
        saving? (subscribe [::subs/saving?])
        translate (subscribe [::subs/translate])
        tournament (atom nil)
        set-name #(swap! tournament assoc :name %)
        set-date (fn [_ date]
                   (swap! tournament assoc :date date))
        set-deadline (fn [_ date]
                       (swap! tournament assoc :deadline date))
        set-format (fn [_ _ format]
                     (swap! tournament assoc :format (keyword format)))
        save-tournament #(dispatch [::events/save-tournament
                                    (-> @tournament
                                        (select-keys [:id :name :format :date :deadline])
                                        (update :date to-local-date)
                                        (update :deadline coerce/to-date-time))])
        selected-decklists (atom #{})
        on-select (fn [selection]
                    (let [selection (js->clj selection)]
                      (reset! selected-decklists
                              (set (case selection
                                     "all" (map :id @decklists)
                                     "none" []
                                     (map (comp :id @decklists) selection))))))
        load-selected-decklists #(dispatch [::events/load-decklists @selected-decklists])]
    (fn tournament-render [id]
      (when (and (nil? @tournament)
                 (some? @saved-tournament))
        (reset! tournament (-> @saved-tournament
                               (update :date coerce/to-date)
                               (update :deadline coerce/to-date))))
      (let [translate @translate]
        [:div#decklist-organizer-tournament
         [:div.tournament-info
          [:div.fields
           [:div.field
            (let [value (:name @tournament "")]
              [text-field {:on-change           set-name
                           :floating-label-text (translate :organizer.tournament.name)
                           :value               value
                           :error-text          (when (str/blank? value)
                                                  (translate :organizer.tournament.name-error))}])]
           [:div.field
            (let [value (:format @tournament)]
              [ui/select-field {:on-change           set-format
                                :value               value
                                :floating-label-text (translate :organizer.tournament.format)
                                :error-text          (when-not value
                                                       (translate :organizer.tournament.format-error))
                                :style               {:width "128px"}}
               [ui/menu-item {:value        :standard
                              :primary-text "Standard"}]
               [ui/menu-item {:value        :modern
                              :primary-text "Modern"}]
               [ui/menu-item {:value        :legacy
                              :primary-text "Legacy"}]])]
           [:div.field
            (let [value (:date @tournament)]
              [date-picker {:value               value
                            :error-text          (when-not value
                                                   (translate :organizer.tournament.date-error))
                            :on-change           set-date
                            :floating-label-text (translate :organizer.tournament.date)
                            :min-date            (js/Date.)}])]
           [:div.field
            (let [value (:deadline @tournament)]
              [date-picker {:value               value
                            :error-text          (when-not value
                                                   (translate :organizer.tournament.deadline-error))
                            :on-change           set-deadline
                            :floating-label-text (translate :organizer.tournament.deadline)
                            :min-date            (js/Date.)}])]
           [:div.field
            (let [value (:deadline @tournament)]
              [ui/time-picker {:value               value
                               :floating-label-text (translate :organizer.tournament.deadline-time)
                               :on-change           set-deadline
                               :format              "24hr"
                               :auto-ok             true
                               :minutes-step        10
                               :text-field-style    {:width "128px"}}])]]
          [:div.link
           (when id
             [:p
              (translate :organizer.submit-page)
              ": "
              [list-submit-link id]])]
          [:div.buttons
           [ui/raised-button {:label    (translate :organizer.save.title)
                              :on-click save-tournament
                              :primary  true
                              :disabled (or @saving?
                                            (str/blank? (:name @tournament))
                                            (nil? (:format @tournament))
                                            (nil? (:date @tournament))
                                            (nil? (:deadline @tournament)))
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
           [ui/raised-button {:label    (translate :organizer.print-lists)
                              :href     (routes/organizer-print-path)
                              :on-click load-selected-decklists
                              :primary  true
                              :disabled (empty? @selected-decklists)
                              :style    {:margin-top "12px"
                                         :width      "200px"}}]]]
         [:div.decklists
          (if (seq @decklists)
            [decklist-table @decklists @selected-decklists on-select translate]
            [:p (translate :organizer.no-lists)])]]))))

(defn view-decklist []
  (let [decklist (subscribe [::subs/decklist-by-type])
        tournament (subscribe [::subs/organizer-tournament])
        translate (subscribe [::subs/translate])]
    (fn view-decklist-render []
      [render-decklist @decklist @tournament @translate])))

(defn view-decklists []
  (let [decklists (subscribe [::subs/decklists-by-type])
        tournament (subscribe [::subs/organizer-tournament])
        translate (subscribe [::subs/translate])
        printed? (clojure.core/atom false)
        print-page #(when (and (seq @decklists)
                               @tournament
                               (not @printed?))
                      (reset! printed? true)
                      (.print js/window))]
    (reagent/create-class
     {:component-did-mount  print-page
      :component-did-update print-page
      :reagent-render       (fn view-decklists-render []
                              [:div
                               (doall (for [decklist @decklists]
                                        ^{:key (:id decklist)}
                                        [render-decklist decklist @tournament @translate]))])})))

(defn ^:private no-op [])

(defn login []
  (let [translate (subscribe [::subs/translate])]
    (fn login-render []
      (let [translate @translate]
        [:div#decklist-organizer-login
         [:p (translate :organizer.log-in.text)]
         [:form {:action (str "/login?next=" (oget js/window "location" "pathname"))
                 :method :post}
          [:input {:type  :hidden
                   :name  :__anti-forgery-token
                   :value (oget js/window "csrf_token")}]
          [text-field {:name                :username
                       :floating-label-text (translate :organizer.log-in.username)
                       :on-change           no-op}]
          [text-field {:name                :password
                       :type                :password
                       :floating-label-text (translate :organizer.log-in.password)
                       :on-change           no-op}]
          [ui/raised-button {:type    :submit
                             :label   (translate :organizer.log-in.button)
                             :primary true}]]]))))
