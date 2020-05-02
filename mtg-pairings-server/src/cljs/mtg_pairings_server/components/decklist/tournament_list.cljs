(ns mtg-pairings-server.components.decklist.tournament-list
  (:require [reagent.core :as reagent :refer [atom]]
            [re-frame.core :refer [subscribe dispatch]]
            [reagent-material-ui.core.form-control-label :refer [form-control-label]]
            [reagent-material-ui.core.link :refer [link]]
            [reagent-material-ui.core.switch-component :refer [switch]]
            [reagent-material-ui.core.table :refer [table]]
            [reagent-material-ui.core.table-body :refer [table-body]]
            [reagent-material-ui.core.table-cell :refer [table-cell]]
            [reagent-material-ui.core.table-head :refer [table-head]]
            [reagent-material-ui.core.table-row :refer [table-row]]
            [reagent-material-ui.styles :refer [with-styles]]
            [mtg-pairings-server.components.decklist.table :as table]
            [mtg-pairings-server.events.decklist :as events]
            [mtg-pairings-server.routes.decklist :as routes]
            [mtg-pairings-server.subscriptions.decklist :as subs]
            [mtg-pairings-server.util :refer [format-date format-date-time get-host]]))

(defn tournament-row [{:keys [classes tournament]}]
  (let [link-props {:class (:link classes)
                    :href  (routes/organizer-tournament-path {:id (:id tournament)})}
        submit-url (routes/new-decklist-path {:id (:id tournament)})]
    [table-row
     [table-cell {:class (:date-column classes)}
      [link link-props
       (format-date (:date tournament))]]
     [table-cell {:class (:deadline-column classes)}
      [link link-props
       (format-date-time (:deadline tournament))]]
     [table-cell {:class (:name-column classes)}
      [link link-props
       (:name tournament)]]
     [table-cell {:class (:decklists-column classes)}
      [link link-props
       (:decklist tournament)]]
     [table-cell {:class (:submit-column classes)}
      [link {:class  (:link classes)
             :href   submit-url
             :target :_blank}
       (str (get-host) submit-url)]]]))

(defn only-upcoming-toggle [props]
  (let [value (subscribe [::subs/only-upcoming?])
        translate (subscribe [::subs/translate])
        on-change (fn [_ value]
                    (dispatch [::events/set-only-upcoming value]))]
    (fn only-upcoming-toggle-render [{:keys [class]}]
      (let [translate @translate]
        [form-control-label
         {:class   class
          :label   (translate :organizer.show-only-upcoming)
          :control (reagent/as-element
                    [switch {:checked   @value
                             :on-change on-change
                             :color     :primary}])}]))))

(defn tournament-list-styles [{:keys [palette spacing] :as theme}]
  (merge (table/table-styles theme)
         {:gutters          {:margin (spacing 0 2)}
          :date-column      (table/table-cell-style {:width 130})
          :deadline-column  (table/table-cell-style {:width 160})
          :name-column      (table/table-cell-style {:width 400})
          :decklists-column (table/table-cell-style {:width 130})
          :submit-column    (table/table-cell-style {})}))

(defn tournament-list* [props]
  (let [tournaments (subscribe [::subs/filtered-organizer-tournaments])
        translate (subscribe [::subs/translate])]
    (fn all-tournaments-render [{:keys [classes]}]
      (let [translate @translate
            header (fn [text]
                     [table-cell {:class (:table-header-cell classes)}
                      text])]
        [:<>
         [only-upcoming-toggle {:class (:gutters classes)}]
         [table {:class (:gutters classes)}
          [table-head
           [table-row
            (header (translate :organizer.date))
            (header (translate :organizer.deadline))
            (header (translate :organizer.tournament.title))
            (header (translate :organizer.decklists))
            (header (translate :organizer.submit-page))]]
          [table-body
           (for [tournament @tournaments]
             ^{:key (str (:id tournament) "--row")}
             [tournament-row {:classes    classes
                              :tournament tournament}])]]]))))

(def tournament-list ((with-styles tournament-list-styles) tournament-list*))
