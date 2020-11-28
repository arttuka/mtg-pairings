(ns mtg-pairings-server.components.decklist.player-info
  (:require [reagent.core :as reagent :refer [atom]]
            [re-frame.core :refer [subscribe dispatch]]
            [clojure.string :as str]
            [reagent-material-ui.styles :refer [with-styles]]
            [mtg-pairings-server.events.decklist :as events]
            [mtg-pairings-server.subscriptions.decklist :as subs]
            [mtg-pairings-server.util :refer [valid-email?]]
            [mtg-pairings-server.util.mtg :refer [valid-dci?]]
            [mtg-pairings-server.util.material-ui :refer [text-field]]
            [mtg-pairings-server.util.styles :refer [on-desktop on-mobile]]))

(defn player-info-styles [{:keys [spacing]}]
  (let [text-field-style #(merge {:margin-bottom (spacing 2)
                                  :height        68}
                                 %)]
    {:full-width (text-field-style {:width "100%"})
     :half-width (text-field-style {on-desktop {:width (str "calc(50% - " (spacing 2) ")")}
                                    on-mobile  {:width "100%"}})
     :left       {on-desktop {:margin-right (spacing 4)}}}))

(defn player-info* [props]
  (let [set-first-name #(dispatch [::events/update-player-info :first-name %])
        set-last-name #(dispatch [::events/update-player-info :last-name %])
        set-deck-name #(dispatch [::events/update-player-info :deck-name %])
        set-email #(dispatch [::events/update-player-info :email %])
        set-dci #(dispatch [::events/update-player-info :dci %])
        player (subscribe [::subs/player-info])
        translate (subscribe [::subs/translate])]
    (fn player-info-render [{:keys [classes]}]
      (let [translate @translate]
        [:div
         [text-field {:class     (:full-width classes)
                      :on-change set-deck-name
                      :label     (translate :submit.deck-name)
                      :value     (:deck-name @player)
                      :style     {:vertical-align :top}
                      :variant   :standard}]
         (let [value (:first-name @player)]
           [text-field {:class      [(:half-width classes) (:left classes)]
                        :on-change  set-first-name
                        :label      (translate :submit.first-name)
                        :value      value
                        :error-text (when (str/blank? value)
                                      (translate :submit.error.first-name))
                        :style      {:vertical-align :top}
                        :variant    :standard}])
         (let [value (:last-name @player)]
           [text-field {:class      (:half-width classes)
                        :on-change  set-last-name
                        :label      (translate :submit.last-name)
                        :value      value
                        :error-text (when (str/blank? value)
                                      (translate :submit.error.last-name))
                        :style      {:vertical-align :top}
                        :variant    :standard}])
         (let [value (:dci @player)]
           [text-field {:class      [(:half-width classes) (:left classes)]
                        :on-change  set-dci
                        :label      (translate :submit.dci)
                        :value      value
                        :error-text (when-not (valid-dci? value)
                                      (translate :submit.error.dci))
                        :style      {:vertical-align :top}
                        :variant    :standard}])
         (let [value (:email @player)]
           [text-field {:class      (:half-width classes)
                        :on-change  set-email
                        :label      (translate :submit.email)
                        :value      value
                        :error-text (when-not (or (str/blank? value)
                                                  (valid-email? value))
                                      (translate :submit.error.email))
                        :style      {:vertical-align :top}
                        :disabled   (:email-disabled? @player)
                        :title      (when (:email-disabled? @player)
                                      (translate :submit.email-disabled))
                        :variant    :standard}])]))))

(def player-info ((with-styles player-info-styles) player-info*))
