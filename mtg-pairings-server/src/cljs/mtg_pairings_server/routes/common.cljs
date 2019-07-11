(ns mtg-pairings-server.routes.common
  (:require [re-frame.core :refer [dispatch]]
            [mtg-pairings-server.events.common :as events]))

(defn dispatch-page
  ([page]
   (dispatch-page page nil nil))
  ([page id]
   (dispatch-page page id nil))
  ([page id round]
   (dispatch [::events/page {:page  page
                             :id    id
                             :round round}])))
