(ns mtg-pairings-server.pages.decklist
  (:require [reagent.core :as reagent :refer [atom]]
            [re-frame.core :refer [subscribe dispatch]]
            [mtg-pairings-server.components.decklist.submit :as submit]
            [mtg-pairings-server.subscriptions :as subs]))

(defn decklist-submit []
  [submit/decklist-submit])

