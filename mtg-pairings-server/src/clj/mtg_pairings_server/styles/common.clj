(ns mtg-pairings-server.styles.common
  (:require [garden.color :refer [hex->rgb rgba]]))

(def ellipsis-overflow {:white-space    :nowrap
                        :text-overflow  :ellipsis
                        :overflow       :hidden
                        :vertical-align :bottom})

(def color {:turquoise        (hex->rgb "337ab7")
            :light-blue       (hex->rgb "d0e9fc")
            :light-grey       (hex->rgb "e6e6e6")
            :grey             (hex->rgb "999999")
            :dark-grey        (hex->rgb "333333")
            :light-green      (hex->rgb "ccffcc")
            :transparent-grey (rgba 0 0 0 0.7)})
