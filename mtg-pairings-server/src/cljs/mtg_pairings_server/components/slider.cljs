(ns mtg-pairings-server.components.slider
  (:require [reagent.core :refer [atom]]
            [cljsjs.rc-slider]
            [goog.functions]
            [oops.core :refer [oget]]))

(defonce ^:private keyseq (clojure.core/atom 0))

(defn slider [{:keys [min max on-change value step color]}]
  (let [range ((oget js/Slider "createSliderWithTooltip") (oget js/Slider "Range"))
        v (atom @value)
        handler (goog.functions.debounce on-change 100)]
    (add-watch value (str "slider-watch-" (swap! keyseq inc))
               (fn [_ _ _ new]
                 (reset! v new)))
    (fn slider-render [{:keys [min max on-change value step color]}]
      (let [on-change (fn [data]
                        (let [data (js->clj data)]
                          (reset! v data)
                          (handler data)))]
        [:> range
         {:min         @min
          :max         @max
          :step        step
          :value       (clj->js @v)
          :on-change   on-change
          :trackStyle  #js[#js{"backgroundColor" color}]
          :handleStyle #js[#js{"borderColor" color}]}]))))
