(ns mtg-pairings-server.components.date-picker
  (:require [reagent.core :as reagent :refer [atom]]
            [cljs.core.async :refer [<! timeout]]
            [cljsjs.react-day-picker]
            [cljs-time.coerce :as coerce]
            [mtg-pairings-server.util.util :refer [format-date parse-date]])
  (:require-macros [cljs.core.async.macros :refer [go]]))

(defn date-picker [{:keys [on-day-click selected-day] :as opts}]
  (let [open (atom false)
        picker-has-focus? (atom false)
        value (atom (or (format-date selected-day) ""))]
    (fn date-picker-render [{:keys [on-day-click selected-day] :as opts}]
      (let [click-handler (fn [d e]
                            (let [date (if (.-selected e)
                                         nil
                                         (coerce/to-local-date d))]
                              (on-day-click date)
                              (reset! value (or (format-date date) ""))
                              (reset! open false)))]
        [:div.date-picker
         [:input {:type     :text
                  :on-focus #(reset! open true)
                  :on-blur  #(go
                               (<! (timeout 100))
                               (when-not @picker-has-focus?
                                 (reset! open false)))
                  :on-change (fn [e]
                               (let [v (.-value (.-target e))]
                                 (reset! value v)
                                 (try
                                   (let [d (parse-date v)]
                                     (on-day-click d))
                                   (catch js/Error _
                                     (on-day-click nil)))))
                  :value    @value
                  :size     12}]
         (when selected-day
           [:div.clear-selection {:on-click #(do
                                               (reset! value "")
                                               (on-day-click nil))}
            [:i.glyphicon.glyphicon-circle-remove]])
         [:div.date-picker-container
          {:style {:display (if @open :inline-block :none)}}
          [:> js/DayPicker {:on-day-click  click-handler
                            :on-focus      #(reset! picker-has-focus? true)
                            :on-blur       #(do
                                              (reset! picker-has-focus? false)
                                              (reset! open false))
                            :selected-days (js/Date. (coerce/to-long selected-day))}]]]))))
