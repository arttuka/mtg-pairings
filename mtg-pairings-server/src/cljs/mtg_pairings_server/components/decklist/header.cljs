(ns mtg-pairings-server.components.decklist.header
  (:require [reagent.core :as reagent :refer [atom]]
            [re-frame.core :refer [subscribe]]
            [reagent-material-ui.components :as ui]
            [reagent-material-ui.icons.add :refer [add]]
            [reagent-material-ui.styles :refer [styled]]
            [mtg-pairings-server.components.language-selector :refer [language-selector]]
            [mtg-pairings-server.routes.decklist :as routes]
            [mtg-pairings-server.subscriptions.decklist :as subs]))

(def header-button (styled ui/button (fn [{:keys [theme]}]
                                       (let [spacing (:spacing theme)]
                                         {:margin (spacing 0 1)
                                          :width  "200px"}))))

(defn header []
  (let [logged-in? (subscribe [::subs/user])
        translate (subscribe [::subs/translate])]
    (fn header-render []
      (let [disabled? (not @logged-in?)
            translate @translate]
        [ui/app-bar {:color      :default
                     :position   :static
                     :class-name :decklist-organizer-header}
         [ui/toolbar
          [header-button {:href     (routes/organizer-path)
                          :variant  :outlined
                          :disabled disabled?}
           (translate :organizer.all-tournaments)]
          [header-button {:href     (routes/organizer-new-tournament-path)
                          :variant  :outlined
                          :disabled disabled?}
           [add]
           (translate :organizer.new-tournament)]
          [:div {:style {:flex "1 0 0"}}]
          [language-selector]
          [header-button {:href     "/logout"
                          :variant  :outlined
                          :disabled disabled?}
           (translate :organizer.log-out)]]]))))
