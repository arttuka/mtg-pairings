(ns mtg-pairings-server.components.decklist.submitted-decklists-table
  (:require [reagent.core :as reagent :refer [atom]]
            [re-frame.core :refer [subscribe dispatch]]
            [reagent-material-ui.core.checkbox :refer [checkbox]]
            [reagent-material-ui.core.link :refer [link]]
            [reagent-material-ui.core.table :refer [table]]
            [reagent-material-ui.core.table-body :refer [table-body]]
            [reagent-material-ui.core.table-cell :refer [table-cell]]
            [reagent-material-ui.core.table-head :refer [table-head]]
            [reagent-material-ui.core.table-row :refer [table-row]]
            [reagent-material-ui.core.table-sort-label :refer [table-sort-label]]
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
        [table-cell {:class class
                     :align :left}
         [table-sort-label {:active    active?
                            :direction (if (:ascending @sort-data) :asc :desc)
                            :on-click  on-click}
          label]]))))

(defn styles [theme]
  (merge (table/table-styles theme)
         {:dci-column       (table/table-cell-style {:width 150})
          :name-column      (table/table-cell-style {:width 400})
          :submitted-column (table/table-cell-style {})}))

(defn submitted-decklists-table* [{:keys [classes decklists selected-decklists
                                          on-select on-select-all translate]}]
  (let [selected-decklists (set selected-decklists)]
    [table
     [table-head
      [table-row
       [table-cell {:padding :checkbox}
        [checkbox {:indeterminate (and (seq selected-decklists)
                                       (< (count selected-decklists) (count decklists)))
                   :checked       (= (count decklists) (count selected-decklists))
                   :on-change     on-select-all}]]
       [table-cell {:class (:table-header-cell classes)}
        (translate :organizer.dci)]
       [sortable-header {:class  (:table-header-cell classes)
                         :column :name
                         :label  (translate :organizer.name)}]
       [sortable-header {:class  (:table-header-cell classes)
                         :column :submitted
                         :label  (translate :organizer.sent)}]]]
     [table-body
      (for [decklist decklists
            :let [decklist-url (routes/organizer-view-path {:id (:id decklist)})
                  link-props {:class    (:link classes)
                              :href     decklist-url
                              :on-click #(dispatch [::events/load-decklist (:id decklist)])}]]
        ^{:key (str (:id decklist) "--row")}
        [table-row
         [table-cell {:padding :checkbox}
          [checkbox {:checked   (contains? selected-decklists (:id decklist))
                     :on-change (on-select (:id decklist))}]]
         [table-cell {:class (:dci-column classes)}
          [link link-props
           (:dci decklist)]]
         [table-cell {:class (:name-column classes)}
          [link link-props
           (str (:last-name decklist) ", " (:first-name decklist))]]
         [table-cell {:class (:submitted-column classes)}
          [link link-props
           (format-date-time (:submitted decklist))]]])]]))

(def submitted-decklists-table ((with-styles styles) submitted-decklists-table*))
