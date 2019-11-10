(ns mtg-pairings-server.components.decklist.header
  (:require [reagent.core :as reagent :refer [atom]]
            [re-frame.core :refer [subscribe]]
            [reagent-material-ui.components :as ui]
            [reagent-material-ui.icons.add :refer [add]]
            [reagent-material-ui.styles :refer [with-styles]]
            [mtg-pairings-server.components.language-selector :refer [language-selector]]
            [mtg-pairings-server.routes.decklist :as routes]
            [mtg-pairings-server.subscriptions.decklist :as subs]))

(defn styles [{:keys [spacing]}]
  {:button    {:margin (spacing 0 1)
               :width  200}
   :root      {"@media print" {:display :none}}
   :separator {:flex 1}})

(defn header* [props]
  (let [logged-in? (subscribe [::subs/user])
        translate (subscribe [::subs/translate])]
    (fn [{:keys [classes]}]
      (let [disabled? (not @logged-in?)
            translate @translate]
        [ui/app-bar {:color    :default
                     :position :static
                     :class    (:root classes)}
         [ui/toolbar
          [ui/button {:class    (:button classes)
                      :href     (routes/organizer-path)
                      :variant  :outlined
                      :disabled disabled?}
           (translate :organizer.all-tournaments)]
          [ui/button {:class    (:button classes)
                      :href     (routes/organizer-new-tournament-path)
                      :variant  :outlined
                      :disabled disabled?}
           [add]
           (translate :organizer.new-tournament)]
          [:div {:class (:separator classes)}]
          [language-selector]
          [ui/button {:class    (:button classes)
                      :href     "/logout"
                      :variant  :outlined
                      :disabled disabled?}
           (translate :organizer.log-out)]]]))))

(def header ((with-styles styles) header*))