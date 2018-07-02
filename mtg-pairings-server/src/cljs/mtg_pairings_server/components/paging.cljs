(ns mtg-pairings-server.components.paging
  (:require [re-frame.core :refer [dispatch subscribe]]))

(defn page-button [on-click selected-page num]
  [:div.button.select-page {:on-click (when (not= selected-page num)
                                        on-click)
                            :class    (when (= selected-page num)
                                        :selected)}
   (inc num)])

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
         (if (pos? @selected-page)
           [:div.button.prev-page {:on-click #(dispatch [event (dec @selected-page)])} "<"]
           [:div.button.prev-page.disabled "<"])
         (doall
           (mapcat (fn [[prev cur]]
                     [(when (and (some? prev) (> (- cur prev) 1))
                        ^{:key (str id "separator-" cur)}
                        [:div.button.separator.disabled "···"])
                      ^{:key (str id "button-" cur)}
                      [page-button #(dispatch [event cur]) @selected-page cur]])
                   (partition 2 1 (cons nil shown-pages))))
         (if (< @selected-page (dec num-pages))
           [:div.button.next-page {:on-click #(dispatch [event (inc @selected-page)] inc)} ">"]
           [:div.button.next-page.disabled ">"])]))))

(defn with-paging [page-event page-subscription data-subscription component]
  (let [page (subscribe page-subscription)
        items-per-page 10
        data (subscribe data-subscription)]
    (fn with-paging-render [page-event page-subsription data-subscription component]
      (let [num-pages (Math/ceil (/ (count @data) items-per-page))]
        [:div
         [pager (str page-event "-1-") page-event page-subscription num-pages]
         [component (->> @data
                         (drop (* @page items-per-page))
                         (take items-per-page))]
         [pager (str page-event "-2-") page-event page-subscription num-pages]]))))
