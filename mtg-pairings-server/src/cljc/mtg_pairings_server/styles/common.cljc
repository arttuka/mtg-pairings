(ns mtg-pairings-server.styles.common
  (:require [garden.color :refer [hex->rgb rgba rgb->hex]]))

(def ellipsis-overflow {:white-space    :nowrap
                        :text-overflow  :ellipsis
                        :overflow       :hidden
                        :vertical-align :bottom})

(def color {:light-blue          (hex->rgb "d0e9fc")
            :light-grey          (hex->rgb "e6e6e6")
            :grey                (hex->rgb "999999")
            :dark-grey           (hex->rgb "333333")
            :light-green         (hex->rgb "ccffcc")
            :transparent-grey    (rgba 0 0 0 0.7)
            :primary1-color      (hex->rgb "90caf9")
            :primary2-color      (hex->rgb "5d99c6")
            :primary3-color      (hex->rgb "c3fdff")
            :accent1-color       (hex->rgb "ec407a")
            :accent2-color       (hex->rgb "b4004e")
            :accent3-color       (hex->rgb "ff77a9")
            :error-color         (hex->rgb "f44336")
            :picker-header-color (hex->rgb "4ba3c7")})

(def palette (into {} (for [key [:primary1-color :primary2-color :primary3-color :accent1-color
                                 :accent2-color :accent3-color :error-color :picker-header-color]]
                        [key (rgb->hex (color key))])))
