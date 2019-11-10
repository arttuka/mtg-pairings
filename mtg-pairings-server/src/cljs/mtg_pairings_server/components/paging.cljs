(ns mtg-pairings-server.components.paging
  (:require [re-frame.core :refer [dispatch subscribe]]
            [reagent-material-ui.components :as ui]
            [reagent-material-ui.icons.chevron-left :refer [chevron-left]]
            [reagent-material-ui.icons.chevron-right :refer [chevron-right]]
            [reagent-material-ui.styles :refer [with-styles]]
            [mtg-pairings-server.subscriptions.pairings :as subs]
            [mtg-pairings-server.util :refer [indexed]]
            [mtg-pairings-server.util.styles :refer [on-desktop]]))

(def button-group ((with-styles (fn [{:keys [spacing]}]
                                  {:root    {:display   :flex
                                             :margin    (spacing 2)
                                             on-desktop {:width 405}}
                                   :grouped {:box-shadow :none
                                             :flex       "1 0 11.1%"}}))
                   ui/button-group))

(def empty-separator [ui/button {:disabled true
                                 :style    {:flex 1000}
                                 :key      "empty-separator"}
                      ""])

(defn get-shown-pages [selected-page num-pages]
  (cond
    (< num-pages 7) (concat (range num-pages) [:empty-separator])
    (= num-pages 7) (range num-pages)
    (<= selected-page 3) (concat (range 0 5) [:separator-2 (dec num-pages)])
    (<= (- num-pages selected-page) 4) (concat [0 :separator-1] (range (- num-pages 5) num-pages))
    :else [0 :separator-1 (dec selected-page) selected-page (inc selected-page) :separator-2 (dec num-pages)]))

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
           (cond
             (number? page)
             (let [selected? (= selected-page page)]
               ^{:key (str "pager-" index)}
               [ui/button {:on-click (when-not selected? #(dispatch-page page))
                           :variant  (if selected? :contained :outlined)
                           :color    (if selected? :secondary :default)}
                (inc page)])

             (= :empty-separator page)
             empty-separator

             :else
             ^{:key (str "pager-" index)}
             [ui/button {:disabled true}
              "···"]))
         [ui/button {:on-click (when (< selected-page (dec num-pages))
                                 #(dispatch-page (inc selected-page)))
                     :disabled (>= selected-page (dec num-pages))}
          [chevron-right]]]))))

(def no-results ((with-styles (fn [{:keys [spacing]}]
                                {:root {:margin (spacing 2)}}))
                 ui/typography))

(defn with-paging [page-event page-subscription data-subscription component]
  (let [page (subscribe [page-subscription])
        items-per-page 10
        data (subscribe [data-subscription])
        translate (subscribe [::subs/translate])]
    (fn with-paging-render [page-event page-subscription data-subscription component]
      (let [num-pages (Math/ceil (/ (count @data) items-per-page))
            pager-props {:event        page-event
                         :subscription page-subscription
                         :num-pages    num-pages}]
        [:<>
         [pager pager-props]
         (if (seq @data)
           [component (->> @data
                           (drop (* @page items-per-page))
                           (take items-per-page))]
           [no-results {:variant :h6}
            (@translate :pager.no-results)])
         [pager pager-props]]))))
