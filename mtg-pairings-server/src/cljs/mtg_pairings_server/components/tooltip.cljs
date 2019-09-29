(ns mtg-pairings-server.components.tooltip
  (:require [reagent.core :as reagent]))

(defn tooltip [props & children]
  (let [translate (reagent/atom nil)
        move-into-view (fn move-into-view [this]
                         (let [node (-> (reagent/dom-node this)
                                        (.getElementsByClassName "tooltip")
                                        (.item 0))
                               viewport-width (.. js/document -body -clientWidth)
                               bounding-rect (.getBoundingClientRect node)
                               right-edge (+ (.-x bounding-rect) (.-width bounding-rect))
                               overflow (- right-edge viewport-width)]
                           (when (pos? overflow)
                             (reset! translate (- overflow)))))]

    (reagent/create-class
     {:component-did-mount  move-into-view
      :component-did-update move-into-view
      :reagent-render       (fn tooltip-render [props & children]
                              (into [:div.tooltip-container
                                     (dissoc props :label)
                                     [:div.tooltip
                                      {:style {:transform (str "translate(-50%, 6px)"
                                                               (when @translate
                                                                 (str " translateX(" @translate "px)")))}}
                                      (:label props)]]
                                    children))})))
