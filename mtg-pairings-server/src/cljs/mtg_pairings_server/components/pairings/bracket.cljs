(ns mtg-pairings-server.components.pairings.bracket
  (:require [re-frame.core :refer [subscribe]]
            [reagent-material-ui.core.avatar :refer [avatar]]
            [reagent-material-ui.core.list :refer [list]]
            [reagent-material-ui.core.list-item :refer [list-item]]
            [reagent-material-ui.core.list-item-avatar :refer [list-item-avatar]]
            [reagent-material-ui.core.list-item-text :refer [list-item-text]]
            [reagent-material-ui.styles :refer [with-styles]]
            [mtg-pairings-server.subscriptions.pairings :as subs]
            [mtg-pairings-server.util.styles :refer [on-desktop on-mobile]]))

(defn bracket-player-styles [{:keys [palette spacing]}]
  (let [border [[1 :solid (get-in palette [:text :primary])]]]
    {:player         (fn [{:keys [matches]}]
                       {on-desktop {:border-bottom     border
                                    :padding-top       (spacing (case matches
                                                                  4 4
                                                                  2 12
                                                                  1 28))
                                    :padding-bottom    (dec (spacing 1))
                                    "&:nth-child(odd)" {:border-right border}
                                    "&:nth-child(2)"   {:margin-top (spacing (case matches
                                                                               4 0
                                                                               2 -4
                                                                               1 -12))}}
                        on-mobile  {:padding-left      0
                                    :padding-right     0
                                    "&:nth-child(odd)" {:margin-bottom (spacing 2)}}})
     :list-item-text {:margin        0
                      :overflow      :hidden
                      :text-overflow :ellipsis}
     :rank-avatar    {:min-width 40}
     :wins-avatar    {:min-width 24}
     :rank           {:width            24
                      :height           24
                      :font-size        16
                      :background-color (get-in palette [:primary :main])}
     :wins           {:background-color :transparent
                      :width            24
                      :height           24
                      :font-size        16
                      :color            (get-in palette [:text :primary])}
     :winner         {:font-weight :bold}}))

(defn bracket-player* [{:keys [rank name wins winner? classes]}]
  [list-item {:class (:player classes)}
   (when rank
     [list-item-avatar {:class (:rank-avatar classes)}
      [avatar {:class (:rank classes)}
       rank]])
   [list-item-text {:class                    (:list-item-text classes)
                    :classes                  {:primary (when winner? (:winner classes))}
                    :primary                  name
                    :primary-typography-props {:no-wrap true}}]
   [list-item-avatar {:class [(:wins-avatar classes)
                              (when winner? (:winner classes))]}
    [avatar {:class (:wins classes)}
     (or wins "")]]])

(def bracket-player ((with-styles bracket-player-styles) bracket-player*))

(def bracket-styles
  {:container    {:display  :flex
                  on-mobile {:flex-direction :column}}
   :round        {on-desktop {:width 300}}
   :round-header {on-desktop {:display :none}}})

(defn bracket* [{:keys [tournament-id]}]
  (let [data (subscribe [::subs/bracket tournament-id])]
    (fn [{:keys [classes]}]
      (into [:div {:class (:container classes)}]
            (for [round @data
                  :let [num-matches (count round)]]
              (into [list {:class (:round classes)}
                     [list-item {:class           (:round-header classes)
                                 :disable-gutters true}
                      [list-item-text {:primary                  (str "Top " (* 2 num-matches))
                                       :primary-typography-props {:variant :h6}}]]]
                    (for [{:keys [team1_name team1_rank team1_wins
                                  team2_name team2_rank team2_wins]} round]
                      [:<>
                       [bracket-player {:name    team1_name
                                        :rank    team1_rank
                                        :wins    team1_wins
                                        :winner? (> team1_wins team2_wins)
                                        :matches num-matches}]
                       [bracket-player {:name    team2_name
                                        :rank    team2_rank
                                        :wins    team2_wins
                                        :winner? (< team1_wins team2_wins)
                                        :matches num-matches}]])))))))

(def bracket ((with-styles bracket-styles) bracket*))
