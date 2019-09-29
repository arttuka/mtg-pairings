(ns mtg-pairings-server.components.decklist.organizer
  (:require [reagent.core :as reagent :refer [atom]]
            [re-frame.core :refer [subscribe dispatch]]
            [reagent-material-ui.components :as ui]
            [reagent-material-ui.icons.add :refer [add]]
            [reagent-material-ui.icons.keyboard-arrow-down :refer [keyboard-arrow-down]]
            [reagent-material-ui.icons.keyboard-arrow-up :refer [keyboard-arrow-up]]
            [clojure.string :as str]
            [oops.core :refer [oget]]
            [mtg-pairings-server.components.date-picker :refer [date-picker date-time-picker]]
            [mtg-pairings-server.components.decklist.print :refer [render-decklist]]
            [mtg-pairings-server.components.language-selector :refer [language-selector]]
            [mtg-pairings-server.events.decklist :as events]
            [mtg-pairings-server.routes.decklist :as routes]
            [mtg-pairings-server.subscriptions.decklist :as subs]
            [mtg-pairings-server.styles.common :refer [palette]]
            [mtg-pairings-server.util :refer [format-date format-date-time to-local-date indexed get-host]]
            [mtg-pairings-server.util.material-ui :refer [text-field wrap-on-change]]))

(defn header []
  (let [logged-in? (subscribe [::subs/user])
        translate (subscribe [::subs/translate])
        button-style {:margin-left  "12px"
                      :margin-right "12px"
                      :width        "180px"}]

    (fn header-render []
      (let [disabled? (not @logged-in?)
            translate @translate]
        [ui/app-bar {:color    :default
                     :position :static}
         [ui/toolbar {:class-name :decklist-organizer-header}
          [ui/button {:href     (routes/organizer-path)
                      :style    button-style
                      :variant  :outlined
                      :disabled disabled?}
           (translate :organizer.all-tournaments)]
          [ui/button {:href     (routes/organizer-new-tournament-path)
                      :style    button-style
                      :variant  :outlined
                      :disabled disabled?}
           [add]
           (translate :organizer.new-tournament)]
          [:div {:style {:flex "1 0 0"}}]
          [language-selector]
          [ui/button {:href     "/logout"
                      :style    button-style
                      :variant  :outlined
                      :disabled disabled?}
           (translate :organizer.log-out)]]]))))

(def table-header-style {:color       :black
                         :font-weight :bold
                         :font-size   "16px"})

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
     [ui/table-cell {:class-name :date
                     :style      column-style}
      [:a.tournament-link link-props
       (format-date (:date tournament))]]
     [ui/table-cell {:class-name :deadline
                     :style      column-style}
      [:a.tournament-link link-props
       (format-date-time (:deadline tournament))]]
     [ui/table-cell {:class-name :name
                     :style      column-style}
      [:a.tournament-link link-props
       (:name tournament)]]
     [ui/table-cell {:class-name :decklists
                     :style      column-style}
      [:a.tournament-link link-props
       (:decklist tournament)]]
     [ui/table-cell {:class-name :submit-page
                     :style      {:font-size "14px"}}
      [list-submit-link (:id tournament)]]]))

(defn only-upcoming-toggle []
  (let [value (subscribe [::subs/only-upcoming?])
        translate (subscribe [::subs/translate])
        on-change (fn [_ value]
                    (dispatch [::events/set-only-upcoming value]))]
    (fn only-upcoming-toggle-render []
      (let [translate @translate]
        [ui/form-control-label
         {:label   (translate :organizer.show-only-upcoming)
          :control (reagent/as-element
                    [ui/switch {:checked   @value
                                :on-change on-change
                                :color     :primary}])}]))))

(defn all-tournaments []
  (let [tournaments (subscribe [::subs/filtered-organizer-tournaments])
        translate (subscribe [::subs/translate])]
    (fn all-tournaments-render []
      (let [translate @translate]
        [:div#decklist-organizer-tournaments
         [only-upcoming-toggle]
         [ui/table {:class :tournaments}
          [ui/table-head
           [ui/table-row {:style {:height "24px"}}
            [ui/table-cell {:class :date
                            :style table-header-style}
             (translate :organizer.date)]
            [ui/table-cell {:class :deadline
                            :style table-header-style}
             (translate :organizer.deadline)]
            [ui/table-cell {:class :name
                            :style table-header-style}
             (translate :organizer.tournament.title)]
            [ui/table-cell {:class :decklists
                            :style table-header-style}
             (translate :organizer.decklists)]
            [ui/table-cell {:class :submit-page
                            :style table-header-style}
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
                   keyboard-arrow-up
                   keyboard-arrow-down)
            style (cond-> (assoc table-header-style
                                 :cursor :pointer
                                 :position :relative)
                    selected? (assoc :color (:accent1-color palette)))]
        [ui/table-cell {:class    class-name
                        :style    style
                        :on-click #(dispatch [::events/sort-decklists column])}
         [icon {:style {:width    "20px"
                        :height   "20px"
                        :position :absolute
                        :left     "-4px"}}]
         children]))))

(defn decklist-table [decklists selected-decklists on-select on-select-all translate]
  [ui/table
   [ui/table-head
    [ui/table-row {:style {:height "24px"}}
     [ui/table-cell {:padding :checkbox}
      [ui/checkbox {:indeterminate (and (seq selected-decklists)
                                        (< (count selected-decklists) (count decklists)))
                    :checked       (= (count decklists) (count selected-decklists))
                    :on-change     on-select-all}]]
     [ui/table-cell {:class :dci
                     :style table-header-style}
      (translate :organizer.dci)]
     [sortable-header {:class  :name
                       :column :name}
      (translate :organizer.name)]
     [sortable-header {:class  :submitted
                       :column :submitted}
      (translate :organizer.sent)]]]
   [ui/table-body
    (for [decklist decklists
          :let [decklist-url (routes/organizer-view-path {:id (:id decklist)})
                link-props {:href     decklist-url
                            :on-click #(dispatch [::events/load-decklist (:id decklist)])}]]
      ^{:key (str (:id decklist) "--row")}
      [ui/table-row
       [ui/table-cell {:padding :checkbox}
        [ui/checkbox {:checked   (contains? selected-decklists (:id decklist))
                      :on-change (on-select (:id decklist))}]]
       [ui/table-cell {:class   :dci
                       :padding :none}
        [:a.decklist-link link-props
         (:dci decklist)]]
       [ui/table-cell {:class   :name
                       :padding :none}
        [:a.decklist-link link-props
         (str (:last-name decklist) ", " (:first-name decklist))]]
       [ui/table-cell {:class   :submitted
                       :padding :none}
        [:a.decklist-link link-props
         (format-date-time (:submitted decklist))]]])]])

(defn notice [type text]
  (let [display? (atom true)
        on-delete #(reset! display? false)]
    (fn notice-render [type text]
      (when @display?
        [ui/chip
         {:color     (case type
                       :success :primary
                       :error :secondary)
          :on-delete on-delete
          :label     text}]))))

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

(defn tournament [id]
  (let [saved-tournament (subscribe [::subs/organizer-tournament])
        decklists (subscribe [::subs/organizer-decklists])
        saving? (subscribe [::subs/saving?])
        translate (subscribe [::subs/translate])
        tournament (atom nil)
        set-name #(swap! tournament assoc :name %)
        set-date #(swap! tournament assoc :date %)
        set-deadline (fn [dl]
                       (.log js/console dl)
                       (swap! tournament assoc :deadline dl))
        set-format (wrap-on-change #(swap! tournament assoc :format (keyword %)))
        save-tournament #(dispatch [::events/save-tournament (select-keys @tournament [:id :name :format :date :deadline])])
        selected-decklists (atom #{})
        on-select (fn [decklist-id]
                    (fn [e]
                      (swap! selected-decklists
                             (if (.. e -target -checked) conj disj)
                             decklist-id)))
        on-select-all (fn [e]
                        (reset! selected-decklists
                                (if (.. e -target -checked)
                                  (set (map :id @decklists))
                                  #{})))
        load-selected-decklists #(dispatch [::events/load-decklists @selected-decklists])]
    (fn tournament-render [id]
      (when (and (nil? @tournament)
                 (some? @saved-tournament))
        (reset! tournament @saved-tournament))
      (let [translate @translate]
        [:div#decklist-organizer-tournament
         [:div.tournament-info
          [:div.fields
           [:div.field
            (let [value (:name @tournament "")]
              [text-field {:on-change  set-name
                           :label      (translate :organizer.tournament.name)
                           :value      value
                           :error-text (when (str/blank? value)
                                         (translate :organizer.tournament.name-error))}])]
           [:div.field
            (let [value (:format @tournament)]
              [ui/form-control {:error (not value)}
               [ui/input-label {:html-for :tournament-format}
                (translate :organizer.tournament.format)]
               [ui/select {:on-change   set-format
                           :value       value
                           :label       (translate :organizer.tournament.format)
                           :style       {:width "128px"}
                           :input-props {:name :tournament-format
                                         :id   :tournament-format}}
                [ui/menu-item {:value :standard}
                 "Standard"]
                [ui/menu-item {:value :modern}
                 "Modern"]
                [ui/menu-item {:value :legacy}
                 "Legacy"]]
               [ui/form-helper-text
                (when-not value
                  (translate :organizer.tournament.format-error))]])]
           [:div.field
            (let [value (:date @tournament)]
              [date-picker {:value     value
                            :label     (translate :organizer.tournament.date)
                            :on-change set-date
                            :variant   :inline
                            :auto-ok   true
                            :format    "DD.MM.YYYY"}])]
           [:div.field
            (let [value (:deadline @tournament)]
              [date-time-picker {:value     value
                                 :label     (translate :organizer.tournament.deadline)
                                 :on-change set-deadline
                                 :variant   :inline
                                 :auto-ok   true
                                 :format    "DD.MM.YYYY HH:mm"
                                 :ampm      false}])]]
          [:div.link
           (when id
             [:p
              (translate :organizer.submit-page)
              ": "
              [list-submit-link id]])]
          [:div.buttons
           [ui/button {:on-click save-tournament
                       :variant  :contained
                       :color    :primary
                       :disabled (or @saving?
                                     (str/blank? (:name @tournament))
                                     (nil? (:format @tournament))
                                     (nil? (:date @tournament))
                                     (nil? (:deadline @tournament)))
                       :style    {:width "240px"}}
            (translate :organizer.save.title)]
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
           [ui/button {:href     (routes/organizer-print-path)
                       :on-click load-selected-decklists
                       :variant  :outlined
                       :color    :primary
                       :disabled (empty? @selected-decklists)
                       :style    {:margin-top "12px"
                                  :width      "240px"}}
            (translate :organizer.print-lists)]]]
         [:div.decklists
          (if (seq @decklists)
            [decklist-table @decklists @selected-decklists on-select on-select-all translate]
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
          [text-field {:name      :username
                       :label     (translate :organizer.log-in.username)
                       :on-change no-op
                       :style     {:margin "0 8px"}}]
          [text-field {:name      :password
                       :type      :password
                       :label     (translate :organizer.log-in.password)
                       :on-change no-op
                       :style     {:margin "0 8px"}}]
          [ui/button {:type    :submit
                      :variant :outlined
                      :color   :primary
                      :style   {:margin         "0 8px"
                                :vertical-align :bottom}}
           (translate :organizer.log-in.button)]]]))))
