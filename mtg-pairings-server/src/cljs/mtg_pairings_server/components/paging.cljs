(ns mtg-pairings-server.components.paging
  (:require [reagent.core :refer [atom]]
            [re-frame.core :refer [subscribe]]))

(defn page-button [page-atom num]
  [:div.button.select-page {:on-click (when (not= @page-atom num)
                                        #(reset! page-atom num))
                            :class    (when (= @page-atom num)
                                        :selected)}
   (inc num)])

(defn pager [page-atom num-pages]
  (let [shown-pages (concat [nil]
                            (when (pos? @page-atom) [0])
                            (when (= @page-atom 3) [1])
                            (when (> @page-atom 1) [(dec @page-atom)])
                            [@page-atom]
                            (when (< @page-atom (- num-pages 2)) [(inc @page-atom)])
                            (when (= @page-atom (- num-pages 4)) [(- num-pages 2)])
                            (when (< @page-atom (dec num-pages)) [(dec num-pages)]))]
    [:div.pager
     (if (pos? @page-atom)
       [:div.button.prev-page {:on-click #(swap! page-atom dec)} "<"]
       [:div.button.prev-page.disabled "<"])
     (mapcat (fn [[prev cur]]
               [(when (and (some? prev) (> (- cur prev) 1))
                  [:div.button.separator.disabled "···"])
                [page-button page-atom cur]])
             (partition 2 1 shown-pages))
     (if (< @page-atom (dec num-pages))
       [:div.button.next-page {:on-click #(swap! page-atom inc)} ">"]
       [:div.button.next-page.disabled ">"])]))

(defn with-paging [subscription component]
  (let [page (atom 0)
        items-per-page 10
        data (subscribe subscription)]
    (fn with-paging-render [subscription component]
      (let [num-pages (Math/ceil (/ (count @data) items-per-page))]
        [:div
         [pager page num-pages]
         [component (->> @data
                         (drop (* @page items-per-page))
                         (take items-per-page))]
         [pager page num-pages]]))))
