(ns mtg-pairings-server.components.pairings.tournament
  (:require [reagent.core :as reagent]
            [re-frame.core :refer [subscribe dispatch]]
            [reagent-material-ui.core.button :refer [button]]
            [reagent-material-ui.core.button-group :refer [button-group] :rename {button-group mui-button-group}]
            [reagent-material-ui.core.card-content :refer [card-content]]
            [reagent-material-ui.core.link :refer [link]]
            [reagent-material-ui.core.list-item :refer [list-item]]
            [reagent-material-ui.icons.expand-more :refer [expand-more]]
            [reagent-material-ui.styles :refer [with-styles]]
            [mtg-pairings-server.components.pairings.expandable :refer [expandable-header]]
            [mtg-pairings-server.routes.pairings :refer [tournament-path pairings-path standings-path pods-path seatings-path bracket-path]]
            [mtg-pairings-server.util :refer [format-date]]
            [mtg-pairings-server.util.styles :refer [on-desktop on-mobile]]))

(defn tournament-styles [{:keys [spacing]}]
  {:card         {:width "100%"}
   :card-content {:padding   (spacing 0 2)
                  on-desktop {:width 340}
                  on-mobile  {:width "100%"}}
   :button-group {:margin-bottom (spacing 1)
                  :display       :flex}
   :half-width   {:width "50%"}})

(def tournament-header-styles
  {:title     {:display :flex}
   :subheader {:display :flex}
   :separator {:flex    1
               :display :inline-block}
   :desktop   {on-mobile {:display :none}}})

(defn tournament-header*
  [{:keys [data expanded? on-expand round-data classes]}]
  (let [title [link {:href (tournament-path {:id (:id data)})}
               (:name data)]
        subheader (str (format-date (:day data)) " — " (:organizer data))
        separator (fn [] [:div {:class (:separator classes)}])
        desktop (fn [v] [:span {:class (:desktop classes)} v])]
    [expandable-header
     {:title          (reagent/as-element
                       (if round-data
                         [:<>
                          title
                          [separator]
                          [desktop (:title round-data)]]
                         title))
      :subheader      (reagent/as-element
                       (if (:started round-data)
                         [:<>
                          subheader
                          [separator]
                          [desktop (:started round-data)]]
                         subheader))
      :on-expand      on-expand
      :expanded?      expanded?
      :header-classes (dissoc classes :separator :desktop :mobile)}]))

(def tournament-header ((with-styles tournament-header-styles) tournament-header*))

(defn tournament* [{:keys [classes data list-item?]}]
  (when data
    (let [pairings (set (:pairings data))
          standings (set (:standings data))
          button-group (fn [& children]
                         (into [mui-button-group {:class      (:button-group classes)
                                                  :variant    :outlined
                                                  :full-width true}]
                               children))
          rendered [:div {:class (:card classes)}
                    [tournament-header {:data data}]
                    [card-content {:class (:card-content classes)}
                     (when (:playoff data)
                       [button-group
                        [button {:href (bracket-path {:id (:id data)})}
                         "Playoff bracket"]])
                     (for [r (:round-nums data)]
                       ^{:key [(:id data) r]}
                       [button-group
                        (when (contains? pairings r)
                          [button {:class (:half-width classes)
                                   :href  (pairings-path {:id (:id data), :round r})}
                           (str "Pairings " r)])
                        (when (contains? standings r)
                          [button {:class (:half-width classes)
                                   :href  (standings-path {:id (:id data), :round r})}
                           (str "Standings " r)])])
                     (let [pod-buttons (cond->> (for [n (:pods data)]
                                                  [button {:href (pods-path {:id (:id data), :round n})}
                                                   (str "Pods " n)])
                                         (:seatings data) (cons [button {:href (seatings-path {:id (:id data)})}
                                                                 "Seatings"]))]
                       (into [:<>]
                             (for [group (partition-all 2 pod-buttons)]
                               (into [button-group] group))))]]]
      (if list-item?
        [list-item {:divider         true
                    :disable-gutters true}
         rendered]
        rendered))))

(def tournament ((with-styles tournament-styles)
                 tournament*))
