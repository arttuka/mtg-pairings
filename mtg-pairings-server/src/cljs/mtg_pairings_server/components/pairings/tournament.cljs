(ns mtg-pairings-server.components.pairings.tournament
  (:require [reagent.core :as reagent]
            [re-frame.core :refer [subscribe dispatch]]
            [reagent-material-ui.components :as ui]
            [reagent-material-ui.icons.expand-more :refer [expand-more]]
            [reagent-material-ui.styles :refer [with-styles]]
            [mtg-pairings-server.components.pairings.expandable :refer [expandable-header]]
            [mtg-pairings-server.routes.pairings :refer [tournament-path pairings-path standings-path pods-path seatings-path bracket-path]]
            [mtg-pairings-server.util :refer [format-date]]
            [mtg-pairings-server.util.material-ui :as mui-util]))

(defn tournament-styles [{:keys [spacing] :as theme}]
  {:card-content     {:padding-top 0}
   :button-container {(mui-util/on-desktop theme) {:width "300px"}
                      (mui-util/on-mobile theme)  {:width "100%"}}
   :button-group     {:margin-bottom (spacing 1)}
   :half-width       {:width "50%"}})

(defn tournament-header
  [{:keys [data expanded? on-expand]}]
  [expandable-header
   {:title     (reagent/as-element
                [ui/link {:href (tournament-path {:id (:id data)})}
                 (:name data)])
    :subheader (str (format-date (:day data)) " — " (:organizer data))
    :on-expand on-expand
    :expanded? expanded?}])


(defn tournament* [{:keys [classes data list-item?]}]
  (when data
    (let [pairings (set (:pairings data))
          standings (set (:standings data))
          button-group (fn [& children]
                         (into [ui/button-group {:class      (:button-group classes)
                                                 :variant    :outlined
                                                 :full-width true}]
                               children))
          rendered [:div
                    [tournament-header {:data data}]
                    [ui/card-content {:class (:card-content classes)}
                     [:div {:class (:button-container classes)}
                      (when (:playoff data)
                        [button-group
                         [ui/button {:href (bracket-path {:id (:id data)})}
                          "Playoff bracket"]])
                      (for [r (:round-nums data)]
                        ^{:key [(:id data) r]}
                        [button-group
                         (when (contains? pairings r)
                           [ui/button {:class (:half-width classes)
                                       :href  (pairings-path {:id (:id data), :round r})}
                            (str "Pairings " r)])
                         (when (contains? standings r)
                           [ui/button {:class (:half-width classes)
                                       :href  (standings-path {:id (:id data), :round r})}
                            (str "Standings " r)])])
                      (let [pod-buttons (cond->> (for [n (:pods data)]
                                                   [ui/button {:href (pods-path {:id (:id data), :round n})}
                                                    (str "Pods " n)])
                                          (:seatings data) (cons [ui/button {:href (seatings-path {:id (:id data)})}
                                                                  "Seatings"]))]
                        (into [:<>]
                              (for [group (partition-all 2 pod-buttons)]
                                (into [button-group] group))))]]]]
      (if list-item?
        [ui/list-item {:divider         true
                       :disable-gutters true}
         rendered]
        rendered))))

(def tournament ((with-styles tournament-styles)
                 tournament*))
