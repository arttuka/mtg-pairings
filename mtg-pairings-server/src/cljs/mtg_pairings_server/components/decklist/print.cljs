(ns mtg-pairings-server.components.decklist.print
  (:require [reagent.core :as reagent :refer [atom]]
            [reagent-material-ui.core.link :refer [link]]
            [reagent-material-ui.styles :refer [with-styles]]
            [mtg-pairings-server.routes.decklist :as routes]
            [mtg-pairings-server.util :refer [format-date]]
            [mtg-pairings-server.util.decklist :refer [card-types]]
            [mtg-pairings-server.util.styles :refer [ellipsis-overflow on-screen on-print]]))

(defn div* [classes]
  (fn div [cls & children]
    (into [:div {:class (if (keyword? cls)
                          (get classes cls)
                          (mapv classes cls))}]
          children)))

(def grey-border "1px solid #999999")
(def print-height "297mm")
(def print-width "210mm")
(def header-line-height 36)
(def top-margin (inc (* header-line-height 2)))
(def print-body-height (str "calc(" print-height " - " top-margin "px)"))
(def player-height 48)
(def card-width 270)
(def column-margin 10)
(def column-gap 20)
(def print-body-width (str "calc(" print-width " - " player-height "px)"))
(def print-card-width (str "calc((" print-body-width " / 2) - " (* 2 column-margin) "px)"))
(def letters-width 90)
(def date-width 200)
(def tournament-name-width (str "calc(" print-body-width " - " letters-width "px - " date-width "px)"))
(def deck-name-width (str "calc(" print-body-width " - " letters-width "px)"))

(def first-letters
  ((with-styles {:root   {on-screen {:display :none}
                          :position :absolute
                          :top      0
                          :right    0
                          :width    letters-width}
                 :header {:font-size  14
                          :text-align :center}
                 :letter {:font-size      32
                          :font-weight    :bold
                          :display        :inline-block
                          :width          30
                          :text-align     :center
                          :text-transform :uppercase}})
   (fn first-letters [{:keys [classes letters translate]}]
     (let [div (div* classes)]
       (into [div :root
              [div :header
               (translate :decklist.first-letters)]]
             (for [letter letters]
               [div :letter
                letter]))))))

(def deck-info
  ((with-styles {:root  {:border-bottom grey-border
                         :height        (+ 1 (* 2 header-line-height))
                         on-print       {:padding-left player-height}}
                 :item  {:display     :inline-block
                         :height      header-line-height
                         :line-height (str header-line-height "px")
                         on-screen    {:font-weight :bold}}
                 :name  {on-screen {:width 768}
                         on-print  {:width     tournament-name-width
                                    "& $label" {:width 96}}}
                 :date  {on-screen {:width        100
                                    :margin-right 12}
                         on-print  {:width     date-width
                                    "& $label" {:width 60}}}
                 :deck  {on-screen {:width 580}
                         on-print  {:width     deck-name-width
                                    "& $label" {:width 60}}}
                 :label {on-screen   {:display :none}
                         :display    :inline-block
                         :text-align :right}
                 :value (merge ellipsis-overflow
                               {:display :inline-block
                                on-print {:padding-left 6
                                          :font-size    20}})
                 :link  {on-print {:color :black}}})
   (fn deck-info [{:keys [classes tournament deck-name translate]}]
     (let [div (div* classes)]
       [div :root
        [div [:item :date]
         [div :label
          (translate :decklist.date)]
         [div :value
          (format-date (:date tournament))]]
        [div [:item :name]
         [div :label
          (translate :decklist.tournament-name)]
         [div :value
          [link {:class (:link classes)
                 :href  (routes/organizer-tournament-path {:id (:id tournament)})}
           (:name tournament)]]]
        [div [:item :deck]
         [div :label
          (translate :decklist.deck-name)]
         [div :value
          deck-name]]]))))

(def player-info
  ((with-styles {:root       {:position :absolute
                              on-screen {:height      header-line-height
                                         :line-height (str header-line-height "px")
                                         :font-size   16
                                         :font-weight :bold
                                         :right       0
                                         :top         header-line-height
                                         :width       300
                                         :text-align  :right}
                              on-print  {:height           player-height
                                         :line-height      (str (- player-height 2) "px")
                                         :width            print-body-height
                                         :padding-top      1
                                         :border-bottom    grey-border
                                         :top              0
                                         :left             0
                                         :transform        "translateY(297mm) rotate(270deg)"
                                         :transform-origin "top left"}}
                 :label      {on-screen       {:display :none}
                              :display        :inline-block
                              :vertical-align :bottom}
                 :value      (merge ellipsis-overflow
                                    {:display        :inline-block
                                     :vertical-align :bottom
                                     on-print        {:font-size 20}})
                 :last-name  {:display  :inline-block
                              on-screen {"& $value::after" {:content "\", \""}
                                         :margin-right     4
                                         :text-align       :right}}
                 :first-name {:display  :inline-block
                              on-screen {:margin-right 12}}
                 :name       {on-print {:width     (str "calc((" print-body-height " - 420px) / 2)")
                                        "& $value" {:width "calc(100% - 90px)"}
                                        "& $label" {:width 90}}}
                 :dci        {:display :inline-block
                              on-print {:width     420
                                        "& $label" {:width 40}}}
                 :digit      {:display :inline-block
                              on-print {:text-align     :center
                                        :vertical-align :bottom
                                        :width          38
                                        :height         36
                                        :line-height    "36px"
                                        :margin         [[5 0]]
                                        :border-left    grey-border}}})
   (fn player-info [{:keys [classes player translate]}]
     (let [div (div* classes)
           dci (vec (:dci player))]
       [div :root
        [div [:name :last-name]
         [div :label
          (translate :decklist.last-name)]
         [div :value
          (:last-name player)]]
        [div [:name :first-name]
         [div :label
          (translate :decklist.first-name)]
         [div :value
          (:first-name player)]]
        [div :dci
         [div :label (translate :decklist.dci)]
         (into [div :value]
               (for [index (range 10)]
                 [div :digit
                  (get dci index)]))]]))))

(def decklist-card
  ((with-styles {:card     {:line-height   "23px"
                            :margin-bottom 6
                            on-screen      {:width card-width}
                            on-print       {:width  print-card-width
                                            :margin [[0 column-margin 6]]}}
                 :quantity {:display       :inline-block
                            :border-bottom grey-border
                            :width         34
                            :text-align    :center
                            :margin        [[0 8]]}
                 :name     {:display       :inline-block
                            :border-bottom grey-border
                            :padding-left  6
                            on-screen      {:width 220}
                            on-print       {:width (str "calc(" print-card-width " - 50px)")}}})
   (fn decklist-card [{:keys [classes card]}]
     (let [div (div* classes)]
       [div :card
        [div :quantity
         (:quantity card)]
        [div :name
         (:name card)]]))))

(def cardlists
  ((with-styles {:root         {:position :relative}
                 :list         {on-screen {:display        :inline-block
                                           :vertical-align :top}}
                 :cards        {on-print {:display        :flex
                                          :flex-direction :column
                                          :flex-wrap      :wrap
                                          :align-content  :space-between
                                          :max-height     print-body-height}}
                 :main         {on-screen {:margin-right column-gap
                                           :width        (+ (* card-width 2) (* column-margin 4))
                                           "& $cards"    {:column-count 2
                                                          :column-gap   column-gap}}
                                on-print  {:width       print-body-width
                                           :margin-left player-height
                                           :position    :relative
                                           "& $header"  {:position :absolute
                                                         :top      0
                                                         :left     column-margin}
                                           "& $cards"   {:padding-top 22}}}
                 :side         {on-print {:position   :absolute
                                          :right      0
                                          :bottom     0
                                          "& $header" {:margin-left column-margin}}}
                 :header       {on-print {:margin-top    16
                                          :margin-bottom 16}}
                 :type-header  {:line-height "24px"
                                :margin      [[0 0 6]]
                                on-print     {:margin [[0 column-margin 6]]}}
                 :main-padding {on-screen {:display :none}
                                :height   30}})
   (fn cardlists [{:keys [classes decklist translate]}]
     (let [div (div* classes)]
       [div :root
        [div [:list :main]
         [:h3 {:class (:header classes)}
          "Maindeck (" (get-in decklist [:count :main]) ")"]
         (into [div :cards
                [div :main-padding]]
               (mapcat (fn [type]
                         (when-let [cards (get-in decklist [:main type])]
                           (list* [:h4 {:class (:type-header classes)}
                                   (translate (str "card-type." (name type)))]
                                  (for [card cards]
                                    [decklist-card {:card card}])))))
               card-types)]
        [div [:list :side]
         [:h3 {:class (:header classes)}
          "Sideboard (" (get-in decklist [:count :side]) ")"]
         (into [div :cards]
               (for [card (:side decklist)]
                 [decklist-card {:card card}]))]]))))

(def render-decklist
  ((with-styles {:root {:position  :relative
                        :font-size 16
                        on-screen  {:width  880
                                    :margin [[12 "auto"]]}
                        on-print   {:width       print-width
                                    :height      print-height
                                    :break-after :page}}})
   (fn render-decklist [{:keys [classes decklist tournament translate]}]
     (let [{{:keys [last-name deck-name] :as player} :player} decklist]
       [:div {:class (:root classes)}
        [first-letters {:letters   (take 3 last-name)
                        :translate translate}]
        [deck-info {:tournament tournament
                    :deck-name  deck-name
                    :translate  translate}]
        [player-info {:player    player
                      :translate translate}]
        [cardlists {:decklist  decklist
                    :translate translate}]]))))
