(ns mtg-pairings-server.components.decklist.tournament-list
  (:require [reagent.core :as reagent :refer [atom]]
            [re-frame.core :refer [subscribe dispatch]]
            [reagent-material-ui.components :as ui]
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
    [ui/table-row
     [ui/table-cell {:class (:date-column classes)}
      [ui/link link-props
       (format-date (:date tournament))]]
     [ui/table-cell {:class (:deadline-column classes)}
      [ui/link link-props
       (format-date-time (:deadline tournament))]]
     [ui/table-cell {:class (:name-column classes)}
      [ui/link link-props
       (:name tournament)]]
     [ui/table-cell {:class (:decklists-column classes)}
      [ui/link link-props
       (:decklist tournament)]]
     [ui/table-cell {:class (:submit-column classes)}
      [ui/link {:class  (:link classes)
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
        [ui/form-control-label
         {:class   class
          :label   (translate :organizer.show-only-upcoming)
          :control (reagent/as-element
                    [ui/switch {:checked   @value
                                :on-change on-change
                                :color     :primary}])}]))))

(defn tournament-list-styles [{:keys [palette spacing] :as theme}]
  (merge (table/table-styles theme)
         {:gutters           {:margin (spacing 0 2)}
          :date-column       (table/table-cell-style {:width 130})
          :deadline-column   (table/table-cell-style {:width 160})
          :name-column       (table/table-cell-style {:width 400})
          :decklists-column  (table/table-cell-style {:width 130})
          :submit-column     (table/table-cell-style {})}))

(defn tournament-list* [props]
  (let [tournaments (subscribe [::subs/filtered-organizer-tournaments])
        translate (subscribe [::subs/translate])]
    (fn all-tournaments-render [{:keys [classes]}]
      (let [translate @translate
            header (fn [text]
                     [ui/table-cell {:class (:table-header-cell classes)}
                      text])]
        [:<>
         [only-upcoming-toggle {:class (:gutters classes)}]
         [ui/table {:class (:gutters classes)}
          [ui/table-head
           [ui/table-row
            (header (translate :organizer.date))
            (header (translate :organizer.deadline))
            (header (translate :organizer.tournament.title))
            (header (translate :organizer.decklists))
            (header (translate :organizer.submit-page))]]
          [ui/table-body
           (for [tournament @tournaments]
             ^{:key (str (:id tournament) "--row")}
             [tournament-row {:classes    classes
                              :tournament tournament}])]]]))))

(def tournament-list ((with-styles tournament-list-styles) tournament-list*))
