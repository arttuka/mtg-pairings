(ns mtg-pairings-server.styles.variables
  (:require [garden.color :refer [hex->rgb]]))

(def mobile-max-width 767)

(def color {:turquoise   (hex->rgb "337ab7")
            :light-blue  (hex->rgb "d0e9fc")
            :light-grey  (hex->rgb "e6e6e6")
            :grey        (hex->rgb "999999")
            :dark-grey   (hex->rgb "333333")
            :light-green (hex->rgb "ccffcc")})
