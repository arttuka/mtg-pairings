(ns mtg-pairings-server.components.organizer.common
  (:require ["react" :as react]
            [reagent.core :as reagent :refer [atom]]
            [reagent.ratom :refer-macros [reaction]]
            [re-frame.core :refer [subscribe]]
            [reagent-material-ui.core.button :refer [button]]
            [reagent-material-ui.core.button-group :refer [button-group]]
            [reagent-material-ui.core.click-away-listener :refer [click-away-listener]]
            [reagent-material-ui.core.grow :refer [grow]]
            [reagent-material-ui.core.menu-item :refer [menu-item] :rename {menu-item mui-menu-item}]
            [reagent-material-ui.core.menu-list :refer [menu-list]]
            [reagent-material-ui.core.paper :refer [paper]]
            [reagent-material-ui.core.popper :refer [popper]]
            [reagent-material-ui.core.typography :refer [typography]]
            [reagent-material-ui.icons.arrow-drop-down :refer [arrow-drop-down]]
            [reagent-material-ui.styles :refer [with-styles]]
            [goog.object :as obj]
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
             typography))

(def flex-button ((with-styles {:root {:flex 1}})
                  button))

(defn select-button [{:keys [on-change]}]
  (let [open? (atom false)
        toggle-open #(swap! open? not)
        anchor-ref (react/createRef)
        on-close (fn [^js/Event event]
                   (when-not (some-> (.-current anchor-ref)
                                     (.contains (.-target event)))
                     (reset! open? false)))
        on-menu-item-click (fn [new-value]
                             (reset! open? false)
                             (on-change new-value))]
    (fn [{:keys [classes value items item-to-label on-click variant render-menu-item]}]
      (let [disabled? (empty? items)
            menu-item (or render-menu-item
                          (fn menu-item [props]
                            [mui-menu-item (dissoc props :item)
                             (item-to-label (:item props))]))]
        [:<>
         [button-group {:variant  variant
                        :color    :primary
                        :ref      anchor-ref
                        :classes  {:root (:button-group classes)}
                        :disabled disabled?}
          [flex-button {:on-click (or on-click toggle-open)
                        :variant  variant
                        :color    :primary}
           [typography {:no-wrap true
                        :variant :inherit}
            (or (item-to-label value) "")]]
          [button {:variant  variant
                   :color    :primary
                   :size     :small
                   :on-click toggle-open}
           [arrow-drop-down]]]
         [popper {:open       @open?
                  :anchor-el  (.-current anchor-ref)
                  :transition true}
          (fn [props]
            (reagent/as-element
             [grow (assoc (js->clj (obj/get props "TransitionProps"))
                          :style {:transform-origin "center top"})
              [paper {:class (:menu classes)}
               [click-away-listener {:on-click-away on-close}
                [menu-list
                 (for [item items]
                   ^{:key item}
                   [menu-item {:selected (= value item)
                               :on-click #(on-menu-item-click item)
                               :item     item}])]]]]))]]))))
