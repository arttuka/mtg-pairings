(ns mtg-pairings-server.components.organizer.clock-controls
  (:require [reagent-material-ui.components :as ui]
            [reagent-material-ui.icons.arrow-drop-down :refer [arrow-drop-down]]
            [reagent-material-ui.icons.arrow-drop-up :refer [arrow-drop-up]]
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
  [ui/button (merge {:size    :small
                     :variant :contained
                     :color   :primary}
                    props)
   [icon]])

(defn button-group-styles [{:keys [palette shape]}]
  {:root    {:flex-direction :column
             :width          18}
   :grouped {"&&"       {:border-radius       0
                         :border              :none
                         "&:first-child"      {:border-top-right-radius (:border-radius shape)}
                         "&:last-child"       {:border-bottom-right-radius (:border-radius shape)}
                         "&:not(:last-child)" {:border-bottom [[1 :solid (get-in palette [:grey 400])]]}}
             :padding   0
             :height    18
             :width     18
             :min-width 18}})

(def time-field-button-group
  ((with-styles button-group-styles)
   ui/button-group))

(def text-field
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
     [ui/text-field (assoc (dissoc props :classes)
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
       [text-field {:on-change on-change*
                    :value     value
                    :variant   :outlined}]
       [time-field-button-group {:variant :contained
                                 :color   :primary}
        (time-field-button {:on-click #(adjust value inc)} up-icon)
        (time-field-button {:on-click #(adjust value dec)} down-icon)]])))

(def clock-icon-button-group ((with-styles (fn [{:keys [spacing]}]
                                             {:root    {:margin (spacing 0 1)
                                                        :height 36
                                                        :flex   "0 0 auto"}
                                              :grouped {:padding 0}}))
                              ui/button-group))

(defn clock-icon-button [{:keys [icon] :as props}]
  [ui/button (merge {:variant :contained
                     :color   :primary}
                    (dissoc props :icon))
   [icon]])

(defn clock-buttons [{:keys [set-clock start-clock stop-clock clock-running]}]
  [clock-icon-button-group {:variant :contained}
   (clock-icon-button {:on-click set-clock
                       :icon     rotate-left
                       :disabled clock-running})
   (clock-icon-button {:on-click start-clock
                       :icon     play-arrow
                       :disabled clock-running})
   (clock-icon-button {:on-click stop-clock
                       :icon     pause
                       :disabled (not clock-running)
                       :color    :secondary})])
