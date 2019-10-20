(ns mtg-pairings-server.components.decklist.tournament
  (:require [reagent.core :as reagent :refer [atom]]
            [re-frame.core :refer [subscribe dispatch]]
            [reagent-material-ui.components :as ui]
            [reagent-material-ui.pickers :as pickers]
            [reagent-material-ui.styles :refer [with-styles]]
            [clojure.string :as str]
            [mtg-pairings-server.components.decklist.submitted-decklists-table :refer [submitted-decklists-table]]
            [mtg-pairings-server.events.decklist :as events]
            [mtg-pairings-server.routes.decklist :as routes]
            [mtg-pairings-server.subscriptions.decklist :as subs]
            [mtg-pairings-server.util :refer [format-date-time get-host]]
            [mtg-pairings-server.util.material-ui :refer [text-field wrap-on-change]]))

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

(defn notices [class]
  (let [saved? (subscribe [::subs/saved?])
        error? (subscribe [::subs/error :save-tournament])
        translate (subscribe [::subs/translate])]
    (fn notices-render [class]
      (let [translate @translate]
        [:div {:class class}
         (when @saved?
           [notice :success (translate :organizer.save.success)])
         (when @error?
           [notice :error (translate :organizer.save.fail)])]))))

(defn tournament-info [props]
  (let [translate (subscribe [::subs/translate])
        saved-tournament (subscribe [::subs/organizer-tournament])
        tournament (atom nil)
        saving? (subscribe [::subs/saving?])
        set-name #(swap! tournament assoc :name %)
        set-date #(swap! tournament assoc :date %)
        set-deadline #(swap! tournament assoc :deadline %)
        set-format (wrap-on-change #(swap! tournament assoc :format (keyword %)))
        save-tournament #(dispatch [::events/save-tournament (select-keys @tournament [:id :name :format :date :deadline])])]
    (fn [{:keys [classes id selected-decklists]}]
      (when (and (nil? @tournament)
                 (some? @saved-tournament))
        (reset! tournament @saved-tournament))
      (let [translate @translate
            load-selected-decklists #(dispatch [::events/load-decklists selected-decklists])]
        [:div {:class (:info-container classes)}
         [:div {:class (:field-container classes)}
          (let [value (:name @tournament "")]
            [text-field {:classes    {:root (:field classes)}
                         :on-change  set-name
                         :label      (translate :organizer.tournament.name)
                         :value      value
                         :error-text (when (str/blank? value)
                                       (translate :organizer.tournament.name-error))}])
          (let [value (:format @tournament)]
            [ui/form-control {:classes {:root (:field classes)}
                              :error   (nil? value)}
             [ui/input-label {:html-for :tournament-format}
              (translate :organizer.tournament.format)]
             [ui/select {:on-change   set-format
                         :value       (or value "")
                         :label       (translate :organizer.tournament.format)
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
                (translate :organizer.tournament.format-error))]])
          (let [value (:date @tournament)]
            [pickers/date-picker {:classes   {:root (:field classes)}
                                  :value     value
                                  :label     (translate :organizer.tournament.date)
                                  :on-change set-date
                                  :variant   :inline
                                  :auto-ok   true
                                  :format    "dd.MM.yyyy"}])
          (let [value (:deadline @tournament)]
            [pickers/date-time-picker {:classes   {:root (:field classes)}
                                       :value     value
                                       :label     (translate :organizer.tournament.deadline)
                                       :on-change set-deadline
                                       :variant   :inline
                                       :auto-ok   true
                                       :format    "dd.MM.yyyy HH:mm"
                                       :ampm      false}])]
         (when id
           [:p
            (translate :organizer.submit-page)
            ": "
            (let [submit-url (routes/new-decklist-path {:id id})]
              [ui/link {:href submit-url}
               (str (get-host) submit-url)])])
         [:div
          [:div {:class (:button-container classes)}
           [ui/button {:on-click   save-tournament
                       :variant    :contained
                       :color      :primary
                       :disabled   (or @saving?
                                       (str/blank? (:name @tournament))
                                       (nil? (:format @tournament))
                                       (nil? (:date @tournament))
                                       (nil? (:deadline @tournament)))
                       :full-width true}
            (translate :organizer.save.title)]
           [ui/button {:href       (routes/organizer-print-path)
                       :on-click   load-selected-decklists
                       :variant    :outlined
                       :color      :primary
                       :disabled   (empty? selected-decklists)
                       :full-width true}
            (translate :organizer.print-lists)]]
          [:div {:class (:notices-container classes)}
           (if @saving?
             [ui/circular-progress {:size    36
                                    :classes {:root (:circular-progress classes)}}]
             [:div {:class (:placeholder classes)}])
           [notices (:notices-container classes)]]]]))))

(defn tournament-styles [{:keys [spacing]}]
  {:root-container    {:padding (spacing 2)}
   :info-container    {:display        :flex
                       :flex-direction :column}
   :field-container   {:display :flex}
   :field             {:flex           "0 1 200px"
                       :margin         (spacing 0 1)
                       "&:first-child" {:margin-left 0}}
   :button-container  {:display         "inline-flex"
                       :flex-direction  :column
                       :justify-content "space-between"
                       :width           240
                       :height          96}
   :notices-container {:display "inline-block"}
   :circular-progress {:margin         (spacing 0 2)
                       :vertical-align :bottom}
   :placeholder       {:width   68
                       :height  36
                       :display "inline-block"}})

(defn tournament* [props]
  (let [decklists (subscribe [::subs/organizer-decklists])
        translate (subscribe [::subs/translate])
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
                                  #{})))]
    (fn tournament-render [{:keys [id classes]}]
      (let [translate @translate]
        [:div#decklist-organizer-tournament {:class (:root-container classes)}
         [tournament-info {:id                 id
                           :classes            classes
                           :selected-decklists @selected-decklists}]
         (if (seq @decklists)
           [submitted-decklists-table {:decklists          @decklists
                                       :selected-decklists @selected-decklists
                                       :on-select          on-select
                                       :on-select-all      on-select-all
                                       :translate          translate}]
           [:p (translate :organizer.no-lists)])]))))

(def tournament ((with-styles tournament-styles) tournament*))
