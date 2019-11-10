(ns mtg-pairings-server.util.styles
  (:require [goog.string :as gstring]
            [goog.string.format]
            [reagent-material-ui.util :refer [clj->js']]))

(def ellipsis-overflow {:white-space    :nowrap
                        :text-overflow  :ellipsis
                        :overflow       :hidden
                        :vertical-align :bottom})

(def mobile-max-width 600)

(defn mobile? []
  (< (.-innerWidth js/window) mobile-max-width))

(def on-mobile
  (str "@media (max-width:" (- mobile-max-width 0.05) "px)"))

(def on-desktop
  (str "@media (min-width:" mobile-max-width "px)"))

(def on-screen "@media screen")

(def on-print "@media print")

(defn create-transition [theme type styles]
  ((get-in theme [:transitions :create]) (name type) (clj->js' styles)))

(defn hex->rgb [color]
  (let [color (subs color 1)
        colors (re-seq (re-pattern (str ".{1," (/ (count color) 3) "}")) color)]
    (map #(js/parseInt % 16) (if (= 1 (count (first colors)))
                               (map #(str % %) colors)
                               colors))))

(defn fade [color a]
  (let [[r g b] (hex->rgb color)]
    (gstring/format "rgba(%d, %d, %d, %f)" r g b a)))
