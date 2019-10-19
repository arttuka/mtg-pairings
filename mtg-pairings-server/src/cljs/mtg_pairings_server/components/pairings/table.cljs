(ns mtg-pairings-server.components.pairings.table
  (:require [re-frame.core :refer [dispatch]]
            [mtg-pairings-server.util.material-ui :as mui-util]
            [reagent-material-ui.components :as ui]
            [reagent-material-ui.icons.keyboard-arrow-down :refer [keyboard-arrow-down]]
            [reagent-material-ui.styles :refer [styled with-styles]]))

(defn table-styles [{:keys [palette] :as theme}]
  {:table        {:line-height               "24px"
                  :border-spacing            0
                  (mui-util/on-mobile theme) {:width "100%"}}
   :table-header {:line-height "36px"}
   :table-row    {"&:nth-child(odd)" {:background-color (get-in palette [:primary :100])}}})

(def arrow-down (styled keyboard-arrow-down (fn [{:keys [theme]}]
                                              {:margin-top                "2px"
                                               :margin-bottom             "2px"
                                               (mui-util/on-mobile theme) {:margin-left  "-3px"
                                                                           :margin-right "-3px"}})))

(defn sortable-header-button-styles [theme]
  {:root  {:padding-top               "4px"
           :padding-bottom            "4px"
           (mui-util/on-mobile theme) {:padding-left  0
                                       :padding-right 0}
           :text-align                :left}
   :label {:justify-content "flex-start"
           :text-transform  :none
           :font-weight     :bold
           :font-size       "16px"}})

(def sortable-header-button ((with-styles sortable-header-button-styles) ui/button))

(defn sortable-header [{:keys [class column sort-key dispatch-key label]}]
  [:th {:class class}
   [sortable-header-button {:color      (if (= column sort-key)
                                          :secondary
                                          :default)
                            :full-width true
                            :on-click   #(dispatch [dispatch-key column])}
    [arrow-down]
    label]])
