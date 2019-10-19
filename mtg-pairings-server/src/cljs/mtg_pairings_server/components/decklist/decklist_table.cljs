(ns mtg-pairings-server.components.decklist.decklist-table
  (:require [reagent.core :as reagent :refer [atom]]
            [re-frame.core :refer [subscribe dispatch]]
            [reagent-material-ui.components :as ui]
            [reagent-material-ui.styles :refer [with-styles]]
            [mtg-pairings-server.components.decklist.table :as table]
            [mtg-pairings-server.events.decklist :as events]
            [mtg-pairings-server.routes.decklist :as routes]
            [mtg-pairings-server.subscriptions.decklist :as subs]
            [mtg-pairings-server.util :refer [format-date-time]]))

(defn sortable-header [{:keys [column]}]
  (let [sort-data (subscribe [::subs/decklist-sort])
        on-click #(dispatch [::events/sort-decklists column])]
    (fn [{:keys [class column label]}]
      (let [active? (= column (:key @sort-data))]
        [ui/table-cell {:class class
                        :align :left}
         [ui/table-sort-label {:active    active?
                               :direction (if (:ascending @sort-data) :asc :desc)
                               :on-click  on-click}
          label]]))))

(defn decklist-table-styles [theme]
  (merge (table/table-styles theme)
         {:dci-column       (table/table-cell-style {:width 150})
          :name-column      (table/table-cell-style {:width 400})
          :submitted-column (table/table-cell-style {})}))

(defn decklist-table* [{:keys [classes decklists selected-decklists
                               on-select on-select-all translate]}]
  (let [selected-decklists (set selected-decklists)]
    [ui/table
     [ui/table-head
      [ui/table-row
       [ui/table-cell {:padding :checkbox}
        [ui/checkbox {:indeterminate (and (seq selected-decklists)
                                          (< (count selected-decklists) (count decklists)))
                      :checked       (= (count decklists) (count selected-decklists))
                      :on-change     on-select-all}]]
       [ui/table-cell {:class (:table-header-cell classes)}
        (translate :organizer.dci)]
       [sortable-header {:class  (:table-header-cell classes)
                         :column :name
                         :label  (translate :organizer.name)}]
       [sortable-header {:class  (:table-header-cell classes)
                         :column :submitted
                         :label  (translate :organizer.sent)}]]]
     [ui/table-body
      (for [decklist decklists
            :let [decklist-url (routes/organizer-view-path {:id (:id decklist)})
                  link-props {:class    (:link classes)
                              :href     decklist-url
                              :on-click #(dispatch [::events/load-decklist (:id decklist)])}]]
        ^{:key (str (:id decklist) "--row")}
        [ui/table-row
         [ui/table-cell {:padding :checkbox}
          [ui/checkbox {:checked   (contains? selected-decklists (:id decklist))
                        :on-change (on-select (:id decklist))}]]
         [ui/table-cell {:class (:dci-column classes)}
          [ui/link link-props
           (:dci decklist)]]
         [ui/table-cell {:class (:name-column classes)}
          [ui/link link-props
           (str (:last-name decklist) ", " (:first-name decklist))]]
         [ui/table-cell {:class (:submitted-column classes)}
          [ui/link link-props
           (format-date-time (:submitted decklist))]]])]]))

(def decklist-table ((with-styles decklist-table-styles) decklist-table*))
