(ns mtg-pairings-server.components.providers
  (:require [re-frame.core :refer [subscribe]]
            [reagent-material-ui.cljs-time-utils :refer [cljs-time-utils]]
            [reagent-material-ui.colors :as colors]
            [reagent-material-ui.pickers :as pickers]
            [reagent-material-ui.styles :as styles]
            [goog.i18n.DateTimeSymbols_fi]
            [goog.i18n.DateTimeSymbols_en]
            [mtg-pairings-server.subscriptions.common :as subs]
            [mtg-pairings-server.theme :refer [theme-provider]]))

(def theme (styles/create-mui-theme
            {:palette {:primary   colors/blue
                       :secondary colors/pink}}))

(defn providers [app]
  (let [language (subscribe [::subs/language])]
    (fn [app]
      [styles/theme-provider theme
       [pickers/mui-pickers-utils-provider {:utils  cljs-time-utils
                                            :locale (case @language
                                                      :fi goog.i18n.DateTimeSymbols_fi
                                                      :en goog.i18n.DateTimeSymbols_en
                                                      goog.i18n.DateTimeSymbols_fi)}
        app]])))
