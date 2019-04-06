(ns mtg-pairings-server.util.mobile
  (:require #?(:clj [garden.stylesheet :refer [at-media]])
            #?(:clj [garden.units :refer [px percent]])
            #?(:cljs [oops.core :refer [oget]])))

(def mobile-max-width 767)

#?(:clj (defn when-mobile [& styles]
          (apply at-media {:max-width (px mobile-max-width)} styles)))

#?(:clj (defn when-desktop [& styles]
          (apply at-media {:min-width (px (inc mobile-max-width))} styles)))

#?(:clj (defn when-screen [& styles]
          (apply at-media {:screen true} styles)))

#?(:clj (defn when-print [& styles]
          (apply at-media {:print true} styles)))

#?(:cljs (defn mobile? []
           (<= (oget js/window "innerWidth") mobile-max-width)))
