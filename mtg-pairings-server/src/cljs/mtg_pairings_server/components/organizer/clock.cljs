(ns mtg-pairings-server.components.organizer.clock
  (:require [reagent.core :refer [atom with-let]]
            [re-frame.core :refer [dispatch subscribe]]
            [reagent-material-ui.colors :as colors]
            [reagent-material-ui.components :as ui]
            [reagent-material-ui.styles :refer [with-styles]]
            [mtg-pairings-server.events.pairings :as events]
            [mtg-pairings-server.subscriptions.pairings :as subs]
            [mtg-pairings-server.util :refer [indexed]]
            [mtg-pairings-server.util.material-ui :refer [text-field]]))

(defn font-size [num]
  (case num
    1 "32vw"
    2 "20vw"
    (3 4) "16vw"
    (5 6) "20vh"
    nil))

(def clock-styles
  {:root             {:text-align      :center
                      :flex            "1 0 50%"
                      :padding         5
                      :display         :flex
                      :flex-direction  :column
                      :justify-content :flex-start
                      :align-items     :center}
   :selected         {:border        [["5px" :solid (:A700 colors/green)]]
                      :padding       0
                      :border-radius 20}
   :text-field       {:height  66
                      :padding [[0 30]]}
   :text-field-input {:font-size "40px"}
   :title            {:height    66
                      :font-size "4rem"
                      :cursor    :pointer}
   :clock            {:cursor      :pointer
                      :line-height 1.1
                      :font-family "Lato, Helvetica, sans-serif"
                      :font-weight :bold
                      :color       (:A700 colors/green)}
   :timeout          {:color (:A700 colors/red)}})

(defn clock* [{:keys [clock]}]
  (let [name (atom (:name clock ""))
        on-change #(reset! name %)
        on-blur #(dispatch [::events/organizer-mode :rename-clock @name])]
    (fn [{:keys [classes clock select-clock selected num]}]
      [:div {:class [(:root classes)
                     (when selected (:selected classes))]}
       (if selected
         [text-field {:class       (:text-field classes)
                      :input-props {:class       (:text-field-input classes)
                                    :on-blur     on-blur
                                    :on-key-down (fn [^js/KeyboardEvent e]
                                                   (when (= "Enter" (.-key e))
                                                     (on-blur)
                                                     (select-clock)))}
                      :on-change   on-change
                      :value       @name
                      :full-width  true
                      :placeholder "Nimeä kello..."}]
         [ui/typography {:class    (:title classes)
                         :variant  :h3
                         :on-click select-clock}
          (:name clock)])
       [:div {:class    [(:clock classes)
                         (when (:timeout clock) (:timeout classes))]
              :on-click select-clock
              :style    {:font-size (font-size num)}}
        (:text clock)]])))

(def clock ((with-styles clock-styles) clock*))

(defn clocks-styles [{:keys []}]
  {:root        {:display        :flex
                 :flex-direction :row
                 :flex-wrap      :wrap
                 :align-content  :stretch}
   :menu-shown  {:height "calc(100vh - 56px)"}
   :menu-hidden {:height "100vh"}})

(defn clocks* [{:keys [classes]}]
  (with-let [cs (subscribe [::subs/organizer :clock])
             selected-clock (subscribe [::subs/organizer :selected-clock])
             hide-organizer-menu? (subscribe [::subs/organizer :menu])
             select-clock #(when-not @hide-organizer-menu?
                             (dispatch [::events/organizer-mode :select-clock %]))
             clock-interval (js/setInterval #(dispatch [::events/update-clocks]) 200)]
    (let [num (count @cs)]
      [:div {:class [(:root classes)
                     (if @hide-organizer-menu?
                       (:menu-hidden classes)
                       (:menu-shown classes))]}
       (doall (for [[i c] (indexed @cs)
                    :let [selected (and (= i @selected-clock)
                                        (not @hide-organizer-menu?))]]
                ^{:key (:id c)}
                [clock {:clock        c
                        :select-clock #(select-clock (when-not selected i))
                        :selected     selected
                        :num          num}]))])
    (finally
      (js/clearInterval clock-interval))))

(def clocks ((with-styles clocks-styles) clocks*))
