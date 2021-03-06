(ns mtg-pairings-server.components.paging
  (:require [re-frame.core :refer [dispatch subscribe]]
            [cljsjs.material-ui]
            [cljs-react-material-ui.reagent :as ui]
            [cljs-react-material-ui.icons :as icons]))

(def style {:width     "45px"
            :min-width "45px"})

(defn page-button [on-click selected-page num]
  [ui/raised-button {:on-click   (when (not= selected-page num)
                                   on-click)
                     :secondary  (= selected-page num)
                     :label      (inc num)
                     :class-name :page-button
                     :style      style}])

(defn pager [id event subscription num-pages]
  (let [selected-page (subscribe subscription)]
    (fn pager-render [id event subscription num-pages]
      (let [shown-pages (if (<= num-pages 7)
                          (range 0 num-pages)
                          (concat [0]
                                  (cond
                                    (<= @selected-page 3) (range 1 5)
                                    (<= (- num-pages @selected-page) 4) (range (- num-pages 5) (dec num-pages))
                                    :else [(dec @selected-page) @selected-page (inc @selected-page)])
                                  [(dec num-pages)]))]
        [:div.pager
         [ui/raised-button {:icon       (icons/navigation-chevron-left)
                            :on-click   (when (pos? @selected-page)
                                          #(dispatch [event (dec @selected-page)]))
                            :disabled   (zero? @selected-page)
                            :class-name :page-button
                            :style      style}]
         (doall
          (mapcat (fn [[prev cur]]
                    [(when (and (some? prev) (> (- cur prev) 1))
                       ^{:key (str id "separator-" cur)}
                       [ui/raised-button {:label      "···"
                                          :class-name :page-button
                                          :style      style}])
                     ^{:key (str id "button-" cur)}
                     [page-button #(dispatch [event cur]) @selected-page cur]])
                  (partition 2 1 (cons nil shown-pages))))
         [ui/raised-button {:icon       (icons/navigation-chevron-right)
                            :on-click   (when (< @selected-page (dec num-pages))
                                          #(dispatch [event (inc @selected-page)]))
                            :disabled   (>= @selected-page (dec num-pages))
                            :class-name :page-button
                            :style      style}]]))))

(defn with-paging [page-event page-subscription data-subscription component]
  (let [page (subscribe page-subscription)
        items-per-page 10
        data (subscribe data-subscription)]
    (fn with-paging-render [page-event page-subsription data-subscription component]
      (let [num-pages (Math/ceil (/ (count @data) items-per-page))]
        (if (seq @data)
          [:div
           [pager (str page-event "-1-") page-event page-subscription num-pages]
           [component (->> @data
                           (drop (* @page items-per-page))
                           (take items-per-page))]
           [pager (str page-event "-2-") page-event page-subscription num-pages]]
          [ui/circular-progress {:style     {:margin  "48px auto 0"
                                             :display :block}
                                 :size      100
                                 :thickness 5}])))))
