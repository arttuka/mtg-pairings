(ns mtg-pairings-server.components.slider
  (:require [cljsjs.rc-slider]
            [oops.core :refer [oget]]))

(defn slider [{:keys [min max on-change value step color]}]
  (let [range ((.-createSliderWithTooltip js/Slider) (.-Range js/Slider))]
    (fn slider-render []
      (let [handler (comp on-change js->clj)]
        [:> range
         {:min         @min
          :max         @max
          :step        step
          :value       (clj->js @value)
          :on-change   handler
          :trackStyle  #js[#js{"backgroundColor" color}]
          :handleStyle #js[#js{"borderColor" color}]}]))))
