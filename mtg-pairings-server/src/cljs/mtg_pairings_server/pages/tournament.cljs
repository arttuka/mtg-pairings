(ns mtg-pairings-server.pages.tournament
  (:require [re-frame.core :refer [subscribe]]
            [cljsjs.material-ui]
            [cljs-react-material-ui.core]
            [cljs-react-material-ui.reagent :as ui]
            [mtg-pairings-server.components.tournament :refer [tournament-list tournament tournament-header pairings standings pods seatings bracket]]
            [mtg-pairings-server.subscriptions :as subs]))

(defn tournaments-page []
  [tournament-list])

(defn tournament-page [id]
  (let [data (subscribe [::subs/tournament id])]
    (fn tournament-page-render [id]
      [tournament @data])))

(defn tournament-subpage [id type round]
  [ui/paper
   {:style {:margin  "10px"
            :padding "10px"}}
   [tournament-header id]
   (case type
     :pairings [pairings id round]
     :standings [standings id round]
     :pods [pods id round]
     :seatings [seatings id]
     :bracket [bracket id])])
