(ns mtg-pairings-server.components.paging
  (:require [re-frame.core :refer [dispatch subscribe]]
            [reagent-material-ui.components :as ui]
            [reagent-material-ui.icons.chevron-left :refer [chevron-left]]
            [reagent-material-ui.icons.chevron-right :refer [chevron-right]]
            [reagent-material-ui.styles :refer [styled with-styles]]
            [mtg-pairings-server.util :refer [keyword->str]]))

(defn styles [{:keys [spacing]}]
  {:button-group {:display   :flex
                  :max-width "405px"
                  :margin    (spacing 2)}
   :button       {:flex      "1 0 0"
                  :min-width 0}})

(defn page-button [{:keys [class on-click selected-page num]}]
  (let [selected? (= selected-page num)]
    [ui/button {:class    class
                :on-click (when-not selected?
                            on-click)
                :variant (when selected?
                           :contained)
                :color    (when selected?
                            :secondary)}
     (inc num)]))

(defn get-shown-pages [selected-page num-pages]
  (if (<= num-pages 7)
    (range num-pages)
    (cond
      (<= selected-page 3) (concat (range 0 5) [:separator-2 (dec num-pages)])
      (<= (- num-pages selected-page) 4) (concat [0 :separator-1] (range (- num-pages 5) num-pages))
      :else [0 :separator-1 (dec selected-page) selected-page (inc selected-page) :separator-2 (dec num-pages)])))

(defn pager* [{:keys [id event subscription num-pages]}]
  (let [selected-page (subscribe [(keyword subscription)])
        dispatch-page #(dispatch [(keyword event) %])]
    (fn pager-render [{:keys [classes id event subscription num-pages]}]
      (let [selected-page @selected-page
            shown-pages (get-shown-pages selected-page num-pages)
            buttons (concat [[ui/button {:class    (:button classes)
                                         :on-click (when (pos? selected-page)
                                                     #(dispatch-page (dec selected-page)))
                                         :disabled (zero? selected-page)}
                              [chevron-left]]]
                            (for [page shown-pages]
                              (if (number? page)
                                (page-button {:class         (:button classes)
                                              :on-click      #(dispatch [(keyword event) page])
                                              :selected-page selected-page
                                              :num           page})
                                [ui/button {:class (:button classes)}
                                 "···"]))
                            [[ui/button {:class    (:button classes)
                                         :on-click (when (< selected-page (dec num-pages))
                                                     #(dispatch-page (inc selected-page)))
                                         :disabled (>= selected-page (dec num-pages))}
                              [chevron-right]]])]
        (into [ui/button-group {:class   (:button-group classes)
                                :variant :outlined}]
              buttons)))))

(def pager ((with-styles styles) pager*))

(def circular-progress (styled ui/circular-progress {:margin  "48px auto 0"
                                                     :display :block}))

(defn with-paging [page-event page-subscription data-subscription component]
  (let [page (subscribe [page-subscription])
        items-per-page 10
        data (subscribe [data-subscription])]
    (fn with-paging-render [page-event page-subscription data-subscription component]
      (let [num-pages (Math/ceil (/ (count @data) items-per-page))
            pager-props {:event        (keyword->str page-event)
                         :subscription (keyword->str page-subscription)
                         :num-pages    num-pages}]
        (if (seq @data)
          [:div
           [pager (assoc pager-props :id (str page-event "-1-"))]
           [component (->> @data
                           (drop (* @page items-per-page))
                           (take items-per-page))]
           [pager (assoc pager-props :id (str page-event "-2-"))]]
          [circular-progress {:size      100
                              :thickness 5}])))))
