(ns mtg-pairings-server.styles.bracket
  (:require [garden.def :refer [defstyles]]
            [garden.units :refer [px px-]]
            [mtg-pairings-server.styles.common :refer [color]]
            [mtg-pairings-server.util.mobile :refer [when-desktop when-mobile]]))

(def line-height 20)
(def bracket-height 50)

(defstyles styles
  [:.bracket
   [:.bracket-round
    {:display        :inline-block
     :vertical-align :top}
    (when-desktop
     (for [n [1 2 4]
           :let [d (/ 4 n)
                 height (* d bracket-height)]]
       [(keyword (str "&.matches-" n))
        {:margin-top (px (* bracket-height (dec d) -1/2))}
        [:.team
         {:height (px height)}
         [:&.team1
          {:padding-top (px- height line-height 1)}]
         [:&.team2
          {:padding-top (px- height line-height 2)}]]]))
    (when-mobile
     [:&
      {:display :block}
      [:h3
       {:margin {:bottom 0
                 :top    "24px"}}]
      [:.team
       {:height (px bracket-height)}
       [:&.team1
        {:padding-top (px- bracket-height line-height 1)}]
       [:&.team2
        {:padding-top (px- bracket-height line-height 2)}]]])
    [:.bracket-match
     [:.team
      {:padding     {:right  (px 20)
                     :bottom 0
                     :left   (px 10)}
       :line-height (px line-height)
       :min-width   (px 200)
       :font-size   (px 16)}
      [:span
       {:vertical-align :bottom}]
      [:&.team1
       {:padding {:bottom (px 1)}}]
      [:&.team2
       {:border {:color (color :dark-grey)
                 :style :solid
                 :width (px 1)
                 :left  0}}]
      [:&.winner
       {:font-weight 500}]
      [:.rank
       {:color        (color :grey)
        :margin-right (px 10)}]
      [:.wins
       {:float       :right
        :margin-left (px 10)}]]]]])
