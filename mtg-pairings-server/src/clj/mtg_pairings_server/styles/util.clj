(ns mtg-pairings-server.styles.util
  (:require [garden.stylesheet :refer [at-media]]
            [garden.units :refer [px percent]]
            [mtg-pairings-server.styles.variables :as variables]))

(defn when-mobile [& styles]
  (apply at-media {:max-width (px variables/mobile-max-width)} styles))

(defn when-desktop [& styles]
  (apply at-media {:min-width (px (inc variables/mobile-max-width))} styles))

(def ellipsis-overflow {:white-space    :nowrap
                        :text-overflow  :ellipsis
                        :overflow       :hidden
                        :vertical-align :bottom})
