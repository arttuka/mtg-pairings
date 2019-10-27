(ns mtg-pairings-server.components.decklist.icons
  (:require [reagent-material-ui.components :as ui]
            [reagent-material-ui.icons.delete-icon :refer [delete]]
            [reagent-material-ui.icons.warning :refer [warning]]
            [reagent-material-ui.styles :refer [with-styles]]))

(defn styles [theme]
  (let [px->rem (get-in theme [:typography :px-to-rem])]
    {:warning-icon {:color          (get-in theme [:palette :error :main])
                    :vertical-align :top}
     :tooltip      {:font-size (px->rem 14)}}))

(defn error-icon* [{:keys [classes error]}]
  (let [icon [warning {:class (:warning-icon classes)}]]
    (if error
      [ui/tooltip {:classes {:tooltip (:tooltip classes)}
                   :title   error}
       icon]
      icon)))

(def error-icon ((with-styles styles) error-icon*))
