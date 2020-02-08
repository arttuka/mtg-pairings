(ns mtg-pairings-server.components.pairings.expandable
  (:require [reagent.core :as reagent :refer [atom]]
            [reagent-material-ui.components :as ui]
            [reagent-material-ui.icons.expand-more :refer [expand-more]]
            [reagent-material-ui.styles :refer [with-styles]]
            [mtg-pairings-server.util.styles :refer [on-desktop create-transition]]))

(defn expandable-styles [theme]
  {:expand      {:transform  "rotate(0deg)"
                 :transition (create-transition theme
                                                :transform
                                                {:duration (get-in theme [:transitions :duration :shortest])})}
   :expand-open {:transform "rotate(180deg)"}
   :expandable  {on-desktop {:cursor :pointer}}})

(defn expandable-header* [{:keys [classes expanded? on-expand header-classes] :as props}]
  (let [other-props (dissoc props :classes :expanded? :on-expand :header-classes)]
    [ui/card-header (merge other-props
                           {:class    (when on-expand (:expandable classes))
                            :classes  header-classes
                            :on-click on-expand
                            :action   (when on-expand
                                        (reagent/as-element
                                         [ui/icon-button {:class    [(:expand classes)
                                                                     (when expanded?
                                                                       (:expand-open classes))]
                                                          :on-click (fn [e]
                                                                      (.stopPropagation e)
                                                                      (on-expand))}
                                          [expand-more]]))})]))

(def expandable-header ((with-styles expandable-styles) expandable-header*))
