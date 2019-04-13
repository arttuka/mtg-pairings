(ns mtg-pairings-server.styles.decklist
  (:require [garden.def :refer [defstyles]]
            [garden.selectors :refer [after & first-child last-child]]
            [garden.units :refer [px px- px+ px* px-div percent vh]]
            [mtg-pairings-server.styles.common :refer [color ellipsis-overflow]]
            [mtg-pairings-server.util.mobile :refer [when-desktop when-mobile when-screen when-print]]))

(defstyles submit
  [:#decklist-submit
   {:position :relative}
   (when-desktop
    [:&
     {:max-width (px 880)
      :margin    "0 auto"}]
    [:.deck-table-container
     {:width          "50%"
      :display        :inline-block
      :vertical-align :top}]
    [:#player-info
     [:.full-width :.half-width
      {:height (px 100)}]
     [:.full-width
      {:width "100%"}]
     [:.half-width
      {:width   "calc(50% - 24px)"
       :display :inline-block}
      [:&.left
       {:margin-right (px 24)}]
      [:&.right
       {:margin-left (px 24)}]]]
    [:.decklist-import
     [:.info :.form
      {:display :inline-block
       :width (percent 50)}]])
   (when-mobile
    [:#player-info
     [:.full-width :.half-width
      {:width   "100%"
       :display :block}]])
   [:.intro
    [:.tournament-date :.tournament-name :.tournament-format
     {:font-weight :bold}]]
   [:h3
    {:margin-bottom 0}]
   [:.deck-table-container
    [:.deck-table
     [:th.quantity :td.quantity
      {:width (px 72)}]
     [:th.actions :td.actions
      {:width (px 48)}]
     [:th.error :td.error
      {:width (px 48)}]]]
   [:.decklist-import
    {:margin "12px 0"}
    [:.info :.form
     {:padding (px 12)}]]])

(def grey-border {:style :solid
                  :width (px 1)
                  :color (color :grey)})

(def body-height (px 952))
(def left-margin (px 64))
(def header-line-height (px 36))
(def top-margin (px+ (px 1) (px* header-line-height 2)))
(def player-height (px 48))
(def card-width (px 270))
(def card-height (px 30))
(def column-gap (px 20))

(defstyles cardlists
  [:.maindeck :.sideboard
   (when-screen
    [:&
     {:display        :inline-block
      :vertical-align :top}])
   (when-print
    [:h3
     {:margin "16px 0"}])
   [:.cards
    [:.card
     {:width         card-width
      :height        (px 24)
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
      {:width        (px 220)
       :padding-left (px 6)}]]]]
  [:.maindeck
   [:.cards
    {:width (px+ card-width column-gap card-width)}]
   (when-screen
    [:&
     {:margin-right column-gap}
     [:.cards
      {:column {:count 2
                :gap   column-gap}}]])
   (when-print
    [:&
     {:margin-left left-margin
      :position    :relative}
     [:h3
      {:position :absolute
       :top      0
       :left     0}]
     [:.cards
      {:display     :flex
       :flex        {:direction :column
                     :wrap      :wrap}
       :align       {:content :space-between}
       :height      body-height
       :padding-top (px 22)}
      [:.card
       [(& first-child)
        {:margin-top card-height}]]]])]
  [:.sideboard
   (when-screen
    [:&
     {:width card-width}])
   (when-print
    [:&
     {:position :absolute
      :left     (px+ left-margin card-width column-gap)}
     (for [i (range 16)
           :let [free-space (px* card-height (- 30 i))
                 top (px+ top-margin free-space)]]
       [(keyword (str "&.sideboard-" i))
        {:top top}])])])

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
     {:height        player-height
      :padding-top   (px 1)
      :line-height   (px- player-height (px 2))
      :width         body-height
      :border-bottom grey-border
      :top           (px+ (px-div (px- body-height player-height) 2) top-margin)
      :left          (px-div (px- body-height player-height) -2)
      :transform     "rotate(270deg)"}]
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
     {:width (px- body-height (px 420))}
     [:.first-name :.last-name
      {:width (percent 50)}
      [:.label
       {:width (px 80)}]]])])

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
     {:padding-left left-margin}
     [:.tournament-date :.tournament-name :.deck-name
      [:.label
       {:width      (px 72)
        :text-align :right}]
      [:.value
       {:padding-left (px 6)
        :font-size    (px 20)}]]
     [:.tournament-date
      {:width (px 200)}]
     [:.tournament-name
      [:a
       {:color :black}]]
     [:.deck-name
      {:width (percent 100)}]])])

(defstyles organizer-decklist
  [:.organizer-decklist
   {:position :relative}
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
     {:height      (px+ body-height top-margin)
      :break-after :page}]
    [:.label
     {:color (color :dark-grey)}]
    [:.first-letters
     {:position :absolute
      :top      0
      :right    0
      :width    (px 84)}
     [:.label
      {:font-size  (px 12)
       :text-align :center
       :width      (percent 100)}]
     [:.letter
      {:font-size      (px 32)
       :font-weight    :bold
       :display        :inline-block
       :width          (px 28)
       :text-align     :center
       :text-transform :uppercase}]])
   cardlists
   player-info
   deck-info])

(def table-row-link {:color       :black
                     :display     :block
                     :height      (px 48)
                     :line-height (px 48)
                     :padding     "0 24px"})

(defstyles organizer
  [:#decklist-organizer-tournaments
   {:margin "12px 24px"}
   [:.tournaments
    [:th.date :td.date
     {:width (px 130)}]
    [:th.deadline :td.deadline
     {:width (px 160)}]
    [:th.decklists :td.decklists
     {:width (px 135)}]
    [:.tournament-link
     table-row-link]]]
  [:#decklist-organizer-tournament
   {:margin "12px 24px"}
   [:.field
    {:display        :inline-block
     :vertical-align :top
     :margin-right   "24px"}]
   [:.decklists
    [:th.dci :td.dci
     {:width (px 150)}]
    [:th.name :td.name
     {:width (px 400)}]
    [:.decklist-link
     table-row-link]]]
  [:#decklist-organizer-login
   {:margin "12px 24px"}]
  organizer-decklist)

(defstyles styles
  organizer
  submit)
