(ns mtg-pairings-server.styles.decklist
  (:require [garden.def :refer [defstyles]]
            [garden.selectors :as sel :refer [after & nth-child first-child last-child]]
            [garden.units :refer [mm px px- px+ px* px-div percent vh]]
            [mtg-pairings-server.styles.common :refer [calc color ellipsis-overflow when-desktop when-mobile when-screen when-print]]))

(defstyles submit
  [:#decklist-submit
   {:position :relative}
   (when-desktop
    [:&
     {:max-width (px 880)
      :margin    "0 auto"}])
   (when-print
    [:.no-print
     {:display :none}])
   [:.language-selector
    {:float :right}]
   [:.top-header
    {:line-height   (px 36)
     :margin-top    (px 15)
     :margin-bottom (px 15)}]
   [:.intro
    [:.tournament-date :.tournament-name :.tournament-format :.tournament-deadline
     {:font-weight :bold}]]
   [:h3
    {:margin-bottom 0}]])

(def grey-border {:style :solid
                  :width (px 1)
                  :color (color :grey)})

(def print-height (mm 297))
(def print-width (mm 210))
(def left-margin (px 64))
(def header-line-height (px 36))
(def top-margin (px+ (px 1) (px* header-line-height 2)))
(def print-body-height (calc (- print-height top-margin)))
(def player-height (px 48))
(def card-width (px 270))
(def card-height (px 30))
(def column-margin (px 10))
(def column-gap (px 20))
(def print-body-width (calc (- print-width player-height)))
(def print-card-width (calc (- (/ print-body-width 2) (px* column-margin 2))))
(def letters-width (px 90))
(def date-width (px 200))
(def tournament-name-width (calc (- print-body-width (px+ letters-width date-width))))
(def deck-name-width (calc (- print-body-width letters-width)))

(defstyles cardlists
  [:.decklists
   {:position :relative}]
  [:.maindeck :.sideboard
   (when-screen
    [:&
     {:display        :inline-block
      :vertical-align :top}
     [:.cards
      [:.card
       {:width card-width}
       [:.name
        {:width (px 220)}]]]])
   (when-print
    [:h3
     {:margin "16px 0"}]
    [:.cards
     [:.card
      {:width  print-card-width
       :margin {:left  column-margin
                :right column-margin}}
      [:.name
       {:width (calc (- print-card-width (px 50)))}]]])
   [:.cards
    [:.card
     {:height        (px 24)
      :line-height   (px 24)
      :margin-bottom (px 6)}
     [:.quantity :.name
      {:display       :inline-block
       :border-bottom grey-border}]
     [:.quantity
      {:width      (px 34)
       :text-align :center
       :margin     "0 8px"}]
     [:.name
      {:padding-left (px 6)}]]
    [:.type-header
     {:height      (px 24)
      :line-height (px 24)
      :margin      {:top    0
                    :bottom (px 6)}
      :break-after :avoid-column}]]]
  [:.maindeck
   (when-screen
    [:&
     {:margin-right column-gap}
     [:.cards
      {:width  (px+ (px* card-width 2) (px* column-margin 4))
       :column {:count 2
                :gap   column-gap}}]])
   (when-print
    [:&
     {:width       print-body-width
      :margin-left player-height
      :position    :relative}
     [:h3
      {:position :absolute
       :top      0
       :left     column-margin}]
     [:.type-header
      {:margin-left column-margin}]
     [:.cards
      {:display     :flex
       :flex        {:direction :column
                     :wrap      :wrap}
       :align       {:content :space-between}
       :max-height  print-body-height
       :padding-top (px 22)}
      [:.card:first-child :.type-header:first-child
       {:margin-top card-height}]]])]
  [:.sideboard
   (when-screen
    [:&
     {:width card-width}])
   (when-print
    [:&
     {:position :absolute
      :right    0
      :bottom   0}
     [:h3
      {:margin-left column-margin}]])])

(defstyles player-info
  [:.player-info
   {:position :absolute}
   [:.last-name :.first-name :.dci :.name
    {:display :inline-block}]
   (when-screen
    [:&
     {:height      header-line-height
      :line-height header-line-height
      :font-size   (px 16)
      :font-weight :bold
      :display     :inline-block
      :right       0
      :top         header-line-height
      :width       (px 300)
      :text-align  :right}]
    [:.name
     {:width (px 188)}
     [:.last-name
      [:.value
       {:margin-right (px 5)}
       [(& after)
        {:content "\", \""}]]]]
    [:.dci
     {:width       (px 100)
      :margin-left (px 12)}])
   (when-print
    [:&
     {:height           player-height
      :padding-top      (px 1)
      :line-height      (px- player-height (px 2))
      :width            print-body-height
      :border-bottom    grey-border
      :top              0
      :left             0
      :transform        "translateY(297mm) rotate(270deg)"
      :transform-origin "top left"}]
    [:.label
     {:vertical-align :top}]
    [:.value
     {:vertical-align :top
      :font-size      (px 20)}]
    [:.dci
     [:.label
      {:width (px 40)}]
     [:.value
      [:.digit
       {:display        :inline-block
        :vertical-align :top
        :text-align     :center
        :border-left    grey-border
        :width          (px 38)
        :height         (px 36)
        :line-height    (px 36)
        :margin         "5px 0"}]]]
    [:.name
     {:width (calc (- print-body-height (px 420)))}
     [:.first-name :.last-name
      {:width         (percent 50)
       :padding-right (px 16)}
      ellipsis-overflow
      [:.label
       {:margin-right (px 8)}]]])])

(defstyles deck-info
  [:.deck-info
   {:border-bottom grey-border}
   [:.tournament-date :.tournament-name :.deck-name
    {:display     :inline-block
     :height      header-line-height
     :line-height header-line-height}]
   (when-screen
    [:&
     {:font-size   (px 16)
      :font-weight :bold}]
    [:.deck-name
     (merge ellipsis-overflow
            {:width (px 580)})]
    [:.tournament-name
     {:width (px 768)}]
    [:.tournament-date
     {:width        (px 100)
      :margin-right (px 12)}])
   (when-print
    [:&
     {:padding-left player-height}
     [:.tournament-date :.tournament-name :.deck-name
      ellipsis-overflow
      [:.label
       {:text-align :right}]
      [:.value
       {:padding-left (px 6)
        :font-size    (px 20)}]]
     [:.tournament-date
      {:width date-width}
      [:.label
       {:width (px 60)}]]
     [:.tournament-name
      {:width tournament-name-width}
      [:a
       {:color :black}]
      [:.label
       {:width (px 96)}]]
     [:.deck-name
      {:width deck-name-width}
      [:.label
       {:width (px 60)}]]])])

(defstyles print-decklist
  [:.print-decklist
   {:position :relative
    :width    (percent 100)}
   [:.label :.value
    {:display :inline-block}]
   (when-screen
    [:&
     {:width  (px 880)
      :margin "12px auto"}]
    [:.label :.first-letters
     {:display :none}])
   (when-print
    [:&
     {:width       print-width
      :height      print-height
      :break-after :page}]
    [:.label
     {:color (color :dark-grey)}]
    [:.first-letters
     {:position :absolute
      :top      0
      :right    0
      :width    letters-width}
     [:.label
      {:font-size  (px 14)
       :text-align :center
       :width      (percent 100)}]
     [:.letter
      {:font-size      (px 32)
       :font-weight    :bold
       :display        :inline-block
       :width          (px 30)
       :text-align     :center
       :text-transform :uppercase}]])
   cardlists
   player-info
   deck-info])

(defstyles organizer
  [:#decklist-organizer
   (when-print
    [:.decklist-organizer-header
     {:display "none !important"}])])

(defstyles styles
  organizer
  print-decklist
  submit)
