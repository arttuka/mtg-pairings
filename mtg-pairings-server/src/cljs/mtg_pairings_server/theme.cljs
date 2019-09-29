(ns mtg-pairings-server.theme
  (:require [reagent-material-ui.colors :as colors]
            [reagent-material-ui.styles :as styles]))

(def theme (styles/create-mui-theme
            {:palette {:primary   colors/blue
                       :secondary colors/pink}}))

(defn theme-provider [app]
  [styles/theme-provider theme
   app])
