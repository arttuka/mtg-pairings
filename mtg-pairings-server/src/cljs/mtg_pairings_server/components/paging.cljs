(ns mtg-pairings-server.components.paging
  (:require [re-frame.core :refer [dispatch subscribe]]
            [reagent-material-ui.components :as ui]
            [reagent-material-ui.icons.chevron-left :refer [chevron-left]]
            [reagent-material-ui.icons.chevron-right :refer [chevron-right]]
            [reagent-material-ui.styles :refer [styled with-styles]]
            [mtg-pairings-server.util :refer [indexed]]))

(def button-group ((with-styles (fn [{:keys [spacing]}]
                                  {:root    {:display   :flex
                                             :max-width "405px"
                                             :margin    (spacing 2)}
                                   :grouped {:flex       "1 0 0"
                                             :min-width  0
                                             :box-shadow :none}}))
                   ui/button-group))

(defn get-shown-pages [selected-page num-pages]
  (if (<= num-pages 7)
    (range num-pages)
    (cond
      (<= selected-page 3) (concat (range 0 5) [:separator-2 (dec num-pages)])
      (<= (- num-pages selected-page) 4) (concat [0 :separator-1] (range (- num-pages 5) num-pages))
      :else [0 :separator-1 (dec selected-page) selected-page (inc selected-page) :separator-2 (dec num-pages)])))

(defn pager [{:keys [event subscription num-pages]}]
  (let [selected-page (subscribe [subscription])
        dispatch-page (fn [page]
                        (.scrollTo js/window 0 0)
                        (dispatch [event page]))]
    (fn pager-render [{:keys [event subscription num-pages]}]
      (let [selected-page @selected-page
            shown-pages (get-shown-pages selected-page num-pages)]
        [button-group {:variant :outlined}
         [ui/button {:on-click (when (pos? selected-page)
                                 #(dispatch-page (dec selected-page)))
                     :disabled (zero? selected-page)}
          [chevron-left]]
         (for [[index page] (indexed shown-pages)]
           (if (number? page)
             (let [selected? (= selected-page page)]
               ^{:key (str  "pager-" index)}
               [ui/button {:on-click (when-not selected? #(dispatch-page page))
                           :variant  (if selected? :contained :outlined)
                           :color    (if selected? :secondary :default)}
                (inc page)])
             ^{:key (str "pager-" index)}
             [ui/button {}
              "···"]))
         [ui/button {:on-click (when (< selected-page (dec num-pages))
                                 #(dispatch-page (inc selected-page)))
                     :disabled (>= selected-page (dec num-pages))}
          [chevron-right]]]))))

(def circular-progress (styled ui/circular-progress {:margin  "48px auto 0"
                                                     :display :block}))

(defn with-paging [page-event page-subscription data-subscription component]
  (let [page (subscribe [page-subscription])
        items-per-page 10
        data (subscribe [data-subscription])]
    (fn with-paging-render [page-event page-subscription data-subscription component]
      (let [num-pages (Math/ceil (/ (count @data) items-per-page))
            pager-props {:event        page-event
                         :subscription page-subscription
                         :num-pages    num-pages}]
        (if (seq @data)
          [:div
           [pager pager-props]
           [component (->> @data
                           (drop (* @page items-per-page))
                           (take items-per-page))]
           [pager pager-props]]
          [circular-progress {:size      100
                              :thickness 5}])))))
