(ns mtg-pairings-server.components.decklist.icons
  (:require [reagent-material-ui.icons.delete-icon :refer [delete]]
            [reagent-material-ui.icons.warning :refer [warning]]
            [reagent-material-ui.styles :refer [styled]]
            [mtg-pairings-server.components.tooltip :refer [tooltip]]))

(def warning-icon (styled warning (fn [{:keys [theme]}]
                                    {:color          (get-in theme [:palette :error :main])
                                     :vertical-align :top})))

(defn error-icon [error]
  (let [icon [warning-icon {:title error}]]
    (if error
      [tooltip {:label error}
       icon]
      icon)))
