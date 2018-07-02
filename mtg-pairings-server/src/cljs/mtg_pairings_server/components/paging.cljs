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
  (let [shown-pages (if (<= num-pages 7)
                      (range 0 num-pages)
                      (concat [0]
                              (cond
                                (<= @page-atom 3) (range 1 5)
                                (<= (- num-pages @page-atom) 4) (range (- num-pages 5) (dec num-pages))
                                :else [(dec @page-atom) @page-atom (inc @page-atom)])
                              [(dec num-pages)]))]
    [:div.pager
     (if (pos? @page-atom)
       [:div.button.prev-page {:on-click #(swap! page-atom dec)} "<"]
       [:div.button.prev-page.disabled "<"])
     (mapcat (fn [[prev cur]]
               [(when (and (some? prev) (> (- cur prev) 1))
                  [:div.button.separator.disabled "···"])
                [page-button page-atom cur]])
             (partition 2 1 (cons nil shown-pages)))
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
