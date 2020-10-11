(ns mtg-pairings-server.components.organizer.clock-controls
  (:require [reagent-material-ui.colors :as colors]
            [reagent-material-ui.core.button :refer [button]]
            [reagent-material-ui.core.button-group :refer [button-group]]
            [reagent-material-ui.core.text-field :refer [text-field]]
            [reagent-material-ui.core.tooltip :refer [tooltip]]
            [reagent-material-ui.icons.add :refer [add]]
            [reagent-material-ui.icons.arrow-drop-down :refer [arrow-drop-down]]
            [reagent-material-ui.icons.arrow-drop-up :refer [arrow-drop-up]]
            [reagent-material-ui.icons.clear :refer [clear]]
            [reagent-material-ui.icons.pause :refer [pause]]
            [reagent-material-ui.icons.play-arrow :refer [play-arrow]]
            [reagent-material-ui.icons.rotate-left :refer [rotate-left]]
            [reagent-material-ui.styles :refer [with-styles]]
            [clojure.string :as str]
            [mtg-pairings-server.util.material-ui :refer [wrap-on-change]]
            [mtg-pairings-server.util.styles :refer [fade]]))

(def with-icon-styles (with-styles {:root {:font-size "24px"
                                           :margin    "-3px"}}))

(def up-icon (with-icon-styles arrow-drop-up))
(def down-icon (with-icon-styles arrow-drop-down))

(defn time-field-button [props icon]
  [button (merge {:size    :small
                  :variant :outlined
                  :color   :primary}
                 props)
   [icon]])

(defn button-group-styles [{:keys [palette shape]}]
  {:root    {:flex-direction :column
             :width          18}
   :grouped {"&&"       {:border-radius       0
                         :margin              0
                         :border-color        (fade (get-in palette [:primary :main]) 0.5)
                         "&:hover"            {:border-color (get-in palette [:primary :main])}
                         "&:first-child"      {:border-top-right-radius (:border-radius shape)}
                         "&:last-child"       {:border-bottom-right-radius (:border-radius shape)}
                         "&:not(:last-child)" {:border-bottom-color :transparent}}
             :padding   0
             :height    18
             :width     18
             :min-width 18}})

(def time-field-button-group
  ((with-styles button-group-styles)
   button-group))

(def time-text-field
  ((with-styles (fn [{:keys [palette]}]
                  {:text-field-root {:width 32}
                   :root            {"&:hover $notchedOutline" {:border-color (get-in palette [:primary :main])}}
                   :input           {:padding    [[8.5 0]]
                                     :text-align :center}
                   :notched-outline {:border-right               :none
                                     :border-color               (fade (get-in palette [:primary :main]) 0.5)
                                     :border-top-right-radius    0
                                     :border-bottom-right-radius 0}}))
   (fn time-text-field [{:keys [classes] :as props}]
     [text-field (assoc (dissoc props :classes)
                        :InputProps {:classes (dissoc classes :text-field-root)}
                        :class (:text-field-root classes))])))

(defn time-field [{:keys [on-change]}]
  (let [on-change* (wrap-on-change
                    (fn [v]
                      (if (str/blank? v)
                        (on-change "")
                        (let [n (js/parseInt v)]
                          (when-not (js/isNaN n)
                            (on-change n))))))
        adjust (fn [value f]
                 (when-let [v (cond
                                (number? value) value
                                (str/blank? value) 0)]
                   (on-change (f v))))]
    (fn [{:keys [value]}]
      [:<>
       [time-text-field {:on-change on-change*
                         :value     value
                         :variant   :outlined}]
       [time-field-button-group {:variant :outlined
                                 :color   :primary}
        (time-field-button {:on-click #(adjust value inc)} up-icon)
        (time-field-button {:on-click #(adjust value dec)} down-icon)]])))

(def clock-icon-button-group ((with-styles (fn [{:keys [spacing]}]
                                             {:root    {:margin (spacing 0 1)
                                                        :height 36
                                                        :flex   "0 0 auto"}
                                              :grouped {:padding 0}}))
                              button-group))

(def button-tooltip ((with-styles (fn [{:keys [shadows palette]}]
                                    {:tooltip {:background-color :white
                                               :color            (get-in palette [:text :primary])
                                               :box-shadow       (shadows 1)
                                               :font-size        12}}))
                     tooltip))

(defn clock-icon-button [{:keys [icon title disabled] :as props}]
  (let [button-component [button (merge {:variant :contained}
                                        (dissoc props :icon :title))
                          [icon]]]
    (if disabled
      button-component
      [button-tooltip {:enter-delay 1000
                       :title       title}
       button-component])))

(def clock-button-styles {:green {:background-color (colors/green :A700)
                                  :color            :white
                                  "&:hover"         {:background-color "#00a152"}}
                          :red   {:background-color (colors/red :A700)
                                  :color            :white
                                  "&:hover"         {:background-color (colors/red 900)}}})

(defn clock-buttons* [{:keys [classes set-clock start-clock stop-clock
                              add-clock remove-clock clock-running selected-clock]}]
  [clock-icon-button-group {:variant :contained
                            :color   :inherit}
   (clock-icon-button {:on-click set-clock
                       :icon     rotate-left
                       :disabled (or clock-running (not selected-clock))
                       :color    :primary
                       :title    "Aseta aika"})
   (clock-icon-button {:on-click start-clock
                       :icon     play-arrow
                       :disabled (or clock-running (not selected-clock))
                       :color    :primary
                       :title    "Käynnistä kello"})
   (clock-icon-button {:on-click stop-clock
                       :icon     pause
                       :disabled (or (not clock-running) (not selected-clock))
                       :color    :secondary
                       :title    "Pysäytä kello"})
   (clock-icon-button {:on-click add-clock
                       :icon     add
                       :disabled false
                       :class    (:green classes)
                       :title    "Lisää uusi kello"})
   (clock-icon-button {:on-click remove-clock
                       :icon     clear
                       :disabled (not selected-clock)
                       :class    (:red classes)
                       :title    "Poista kello"})])

(def clock-buttons ((with-styles clock-button-styles) clock-buttons*))
