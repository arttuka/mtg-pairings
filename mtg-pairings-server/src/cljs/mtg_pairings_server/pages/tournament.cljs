(ns mtg-pairings-server.pages.tournament
  (:require [re-frame.core :refer [subscribe]]
            [cljsjs.material-ui]
            [cljs-react-material-ui.core]
            [cljs-react-material-ui.reagent :as ui]
            [mtg-pairings-server.components.tournament :refer [tournament-list tournament-card-header tournament pairings standings pods seatings bracket]]
            [mtg-pairings-server.routes :refer [tournament-path]]
            [mtg-pairings-server.subscriptions :as subs]
            [mtg-pairings-server.util.util :refer [format-date]]))

(defn tournaments-page []
  [tournament-list])

(defn tournament-page [id]
  (let [data (subscribe [::subs/tournament id])]
    (fn tournament-page-render [id]
      [tournament @data])))

(defn tournament-subpage [id type round]
  (let [tournament (subscribe [::subs/tournament id])]
    (fn tournament-subpage-render [id type round]
      [ui/card
       [tournament-card-header @tournament]
       [ui/card-text
        {:style {:padding-top 0}}
        (case type
          :pairings [pairings id round]
          :standings [standings id round]
          :pods [pods id round]
          :seatings [seatings id]
          :bracket [bracket id])]])))
