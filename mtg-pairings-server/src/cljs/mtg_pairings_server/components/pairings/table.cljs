(ns mtg-pairings-server.components.pairings.table
  (:require [re-frame.core :refer [dispatch]]
            [reagent-material-ui.core.button :refer [button]]
            [reagent-material-ui.icons.keyboard-arrow-down :refer [keyboard-arrow-down]]
            [reagent-material-ui.styles :refer [with-styles]]
            [mtg-pairings-server.util.styles :refer [on-mobile]]))

(defn table-styles [{:keys [palette]}]
  {:table        {:line-height    "24px"
                  :border-spacing 0
                  :font-size      16
                  on-mobile       {:width "100%"}}
   :table-header {:line-height "36px"}
   :table-row    {"&:nth-child(odd)" {:background-color (get-in palette [:primary 100])}}})

(def arrow-down ((with-styles {:root {:margin-top    "2px"
                                      :margin-bottom "2px"
                                      on-mobile      {:margin-left  "-3px"
                                                      :margin-right "-3px"}}})
                 keyboard-arrow-down))

(def sortable-header-button-styles
  {:root  {:padding-top    "4px"
           :padding-bottom "4px"
           on-mobile       {:padding-left  0
                            :padding-right 0}
           :text-align     :left}
   :label {:justify-content :flex-start
           :text-transform  :none
           :font-weight     :bold
           :font-size       "16px"}})

(def sortable-header-button ((with-styles sortable-header-button-styles) button))

(defn sortable-header [{:keys [class column sort-key dispatch-key label]}]
  [:th {:class class}
   [sortable-header-button {:color      (if (= column sort-key)
                                          :secondary
                                          :default)
                            :full-width true
                            :on-click   #(dispatch [dispatch-key column])}
    [arrow-down]
    label]])