(ns mtg-pairings-server.components.providers
  (:require [re-frame.core :refer [subscribe]]
            [reagent-material-ui.cljs-time-utils :refer [cljs-time-utils]]
            [reagent-material-ui.colors :as colors]
            [reagent-material-ui.core.css-baseline :refer [css-baseline]]
            [reagent-material-ui.pickers.mui-pickers-utils-provider :refer [mui-pickers-utils-provider]]
            [reagent-material-ui.styles :as styles]
            [mtg-pairings-server.subscriptions.common :as subs]
            [mtg-pairings-server.theme :refer [theme-provider]])
  (:import (goog.i18n DateTimeSymbols_fi DateTimeSymbols_en)))

(def theme (styles/create-mui-theme
            {:palette {:primary   colors/blue
                       :secondary colors/pink}}))

(defn providers [app]
  (let [language (subscribe [::subs/language])]
    (fn [app]
      [styles/theme-provider theme
       [mui-pickers-utils-provider {:utils  cljs-time-utils
                                    :locale (case @language
                                              :fi DateTimeSymbols_fi
                                              :en DateTimeSymbols_en
                                              DateTimeSymbols_fi)}
        [css-baseline]
        app]])))