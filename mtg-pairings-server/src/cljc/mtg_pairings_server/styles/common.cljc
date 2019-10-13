(ns mtg-pairings-server.styles.common
  (:require [clojure.string :as str]
            [clojure.walk :refer [postwalk]]
            [garden.color :refer [hex->rgb rgb? rgba rgb->hex]]
            [garden.compiler :refer [CSSRenderer]]
            #?(:clj [garden.stylesheet :refer [at-media]])
            #?(:clj [garden.units :refer [px percent]])
            #?(:cljs [oops.core :refer [oget]])))

(def ellipsis-overflow {:white-space    :nowrap
                        :text-overflow  :ellipsis
                        :overflow       :hidden
                        :vertical-align :bottom})

(defn rgba? [color]
  (every? color [:red :green :blue :alpha]))

(defn ->str [c]
  (cond
    (rgba? c) (str "rgba(" (:red c) ", " (:green c) ", " (:blue c) ", " (:alpha c) ")")
    (rgb? c) (rgb->hex c)))

(def color {:light-blue           (hex->rgb "d0e9fc")
            :light-grey           (hex->rgb "e0e0e0")
            :grey                 (hex->rgb "999999")
            :dark-grey            (hex->rgb "333333")
            :light-green          (hex->rgb "ccffcc")
            :transparent-grey     (rgba 0 0 0 0.7)
            :primary1-color       (hex->rgb "90caf9")
            :primary2-color       (hex->rgb "5d99c6")
            :primary3-color       (hex->rgb "c3fdff")
            :accent1-color        (hex->rgb "ec407a")
            :accent2-color        (hex->rgb "b4004e")
            :accent3-color        (hex->rgb "ff77a9")
            :error-color          (hex->rgb "f44336")
            :picker-header-color  (hex->rgb "4ba3c7")
            :text-color           (rgba 0 0 0 0.87)
            :secondary-text-color (rgba 0 0 0 0.54)
            :alternate-text-color (hex->rgb "ffffff")
            :white-0.15 (rgba 255 255 255 0.15)
            :white-0.25 (rgba 255 255 255 0.25)})

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

(declare calc*)

(defrecord Calc [expression s]
  Object
  (toString [_]
    (str "(calc" s ")"))
  CSSRenderer
  (render-css [_]
    (str "calc" (calc* expression))))

(defn ^:private calc* [token]
  (cond
    (vector? token)
    (let [[op & vals] token]
      (str \(
           (str/join (interpose (str " " op " ") (map calc* vals)))
           \)))

    (number? token)
    (str token)

    (string? token)
    token

    ;; CSSUnit
    (and (:magnitude token) (:unit token))
    (str (:magnitude token) (name (:unit token)))

    ;; Calc
    (:expression token)
    (calc* (:expression token))

    :else
    (throw (ex-info (str "Don't know how to calc token " token) {:token token}))))

(defmacro calc [expression]
  `(Calc. ~(postwalk (fn [token]
                       (if (and (sequential? token)
                                (contains? #{'+ '- '* '/} (first token)))
                         (into [(str (first token))] (rest token))
                         token))
                     expression)
          ~(str expression)))
