(ns mtg-pairings-server.components.pairings.expandable
  (:require [reagent.core :as reagent :refer [atom]]
            [reagent-material-ui.components :as ui]
            [reagent-material-ui.icons.expand-more :refer [expand-more]]
            [reagent-material-ui.styles :refer [with-styles]]
            [mtg-pairings-server.util.material-ui :as mui-util]))

(defn expandable-styles [theme]
  {:expand      {:transform  "rotate(0deg)"
                 :transition (mui-util/create-transition theme
                                                         :transform
                                                         {:duration (get-in theme [:transitions :duration :shortest])})}
   :expand-open {:transform "rotate(180deg)"}
   :expandable  {(mui-util/on-desktop theme) {:cursor :pointer}}})

(defn expandable-header* [{:keys [classes expanded? on-expand] :as props}]
  (let [other-props (dissoc props :classes :expanded? :on-expand)]
    [ui/card-header (merge other-props
                           {:class    (when on-expand (:expandable classes))
                            :on-click on-expand
                            :action   (when on-expand
                                        (reagent/as-element
                                         [ui/icon-button {:class    [(:expand classes)
                                                                     (when expanded?
                                                                       (:expand-open classes))]
                                                          :on-click on-expand}
                                          [expand-more]]))})]))

(def expandable-header ((with-styles expandable-styles) expandable-header*))
