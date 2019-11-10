(ns mtg-pairings-server.components.organizer.common
  (:require [reagent.core :as reagent :refer [atom]]
            [reagent.ratom :refer-macros [reaction]]
            [re-frame.core :refer [subscribe]]
            [reagent-material-ui.components :as ui]
            [reagent-material-ui.styles :refer [with-styles]]
            [mtg-pairings-server.subscriptions.common :as common-subs]
            [mtg-pairings-server.util.styles :refer [ellipsis-overflow]]))

(def number-style {:text-align  :center
                   :font-weight :bold
                   :font-size   20
                   :flex        "0 0 40px"})

(def player-style (merge {:flex 1}
                         ellipsis-overflow))

(def column ((with-styles {:root        {:display        :flex
                                         :flex-direction :column
                                         :flex-wrap      :wrap
                                         :align-content  :space-around
                                         :align-items    :center
                                         :font-size      "16px"
                                         :line-height    "24px"
                                         :overflow       :hidden}
                           :menu-shown  {:height "calc(100vh - 103px)"}
                           :menu-hidden {:height "calc(100vh - 47px)"}})
             (fn column [{:keys [classes menu-hidden? children ref]}]
               [:div {:class [(:root classes)
                              (if menu-hidden?
                                (:menu-hidden classes)
                                (:menu-shown classes))]
                      :ref   ref}
                children])))

(defn calculate-row-width [n width height]
  (let [per-column (quot height 24)
        required-columns (Math/ceil (/ n per-column))
        row-width (/ width required-columns)]
    (when (< row-width 480)
      (- row-width 10))))

(defn resizing-column [{:keys [items]}]
  (let [column-node (atom nil)
        column-ref #(reset! column-node %)
        window-size (subscribe [::common-subs/window-size])
        row-width (reaction (when-let [node @column-node]
                              (let [[width _] @window-size
                                    height (.-clientHeight node)]
                                (calculate-row-width (count @items) width height))))]
    (fn [{:keys [items component menu-hidden? key-fn]}]
      [column {:menu-hidden? menu-hidden?
               :ref          column-ref}
       (doall (for [item @items]
                ^{:key (key-fn item)}
                [component {:data  item
                            :width @row-width}]))])))

(def row ((with-styles (fn [{:keys [palette]}]
                         {:root {:display           :flex
                                 :width             470
                                 :margin            [[0 5]]
                                 "&:nth-child(odd)" {:background-color (get-in palette [:grey 300])}}}))
          (fn row [{:keys [class-name classes children style]}]
            [:div {:class [class-name (:root classes)]
                   :style style}
             children])))

(def header ((with-styles (fn [{:keys [spacing]}]
                            {:root {:margin-top    (spacing 1)
                                    :margin-bottom (spacing 1)}}))
             ui/typography))
