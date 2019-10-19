(ns mtg-pairings-server.components.tournament
  (:require [reagent.core :as reagent]
            [re-frame.core :refer [subscribe dispatch]]
            [reagent-material-ui.components :as ui]
            [reagent-material-ui.icons.expand-more :refer [expand-more]]
            [reagent-material-ui.icons.keyboard-arrow-down :refer [keyboard-arrow-down]]
            [reagent-material-ui.styles :as styles]
            [goog.string :as gstring]
            [goog.string.format]
            [mtg-pairings-server.events.pairings :as events]
            [mtg-pairings-server.subscriptions.pairings :as subs]
            [mtg-pairings-server.subscriptions.common :as common-subs]
            [mtg-pairings-server.routes.pairings :refer [tournaments-path tournament-path pairings-path standings-path pods-path seatings-path bracket-path]]
            [mtg-pairings-server.components.paging :refer [with-paging]]
            [mtg-pairings-server.components.pairings.filter :refer [filters]]
            [mtg-pairings-server.components.pairings.tournament :refer [tournament]]
            [mtg-pairings-server.util :refer [format-date indexed]]
            [mtg-pairings-server.util.mtg :refer [bye?]]))

(defn newest-tournaments-list []
  (let [tournaments (subscribe [::subs/newest-tournaments])]
    (fn newest-tournaments-list-render []
      [:div#tournaments
       [:h2.newest-header
        "Aktiiviset turnaukset | "
        [:a {:href (tournaments-path)}
         "Turnausarkistoon"]]
       (if-let [ts (seq @tournaments)]
         (for [t ts]
           ^{:key (:id t)}
           [tournament {:data       t
                        :list-item? true}])
         [:h3.no-active "Ei aktiivisia turnauksia."])])))

(defn tournament-list []
  [:div
   [filters]
   [with-paging ::events/tournaments-page ::subs/tournaments-page ::subs/filtered-tournaments
    (fn tournament-list-render [tournaments]
      [:div#tournaments
       (for [t tournaments]
         ^{:key (:id t)}
         [tournament {:data       t
                      :list-item? true}])])]])

(def sortable-header-cell (styles/styled :th (fn [{:keys [theme selected]}]
                                               {:color  (get-in theme (if selected
                                                                        [:palette :secondary :main]
                                                                        [:palette :text :primary]))
                                                :cursor :pointer})))

(def arrow-down (styles/styled keyboard-arrow-down {:vertical-align :baseline
                                                    :position       :absolute
                                                    :left           0}))
(defn sortable-header [{:keys [class column sort-key dispatch-key]} & children]
  [sortable-header-cell {:class    class
                         :selected (= column sort-key)
                         :on-click #(dispatch [dispatch-key column])}
   [arrow-down]
   children])

(defn seating-row [seat]
  [:tr
   [:td.table (:table_number seat)]
   [:td.player (:name seat)]])

(defn seatings [id round]
  (let [data (subscribe [::subs/sorted-seatings id round])
        sort-key (subscribe [::subs/seatings-sort])]
    (fn seatings-render [id round]
      (when (seq @data)
        [:table.seatings-table
         [:thead
          [:tr
           [sortable-header {:class        :table
                             :column       :table_number
                             :sort-key     @sort-key
                             :dispatch-key ::events/sort-seatings}
            "Pöytä"]
           [sortable-header {:class        :player
                             :column       :name
                             :sort-key     @sort-key
                             :dispatch-key ::events/sort-seatings}
            "Pelaaja"]]]
         [:tbody
          (for [seat @data]
            ^{:key [(:name seat)]}
            [seating-row seat])]]))))

(defn bracket-match [match]
  [:div.bracket-match
   [:div.team.team1
    {:class (when (> (:team1_wins match) (:team2_wins match))
              :winner)}
    (when (:team1_rank match)
      [:span.rank (str \( (:team1_rank match) \))])
    [:span.name (:team1_name match)]
    [:span.wins (:team1_wins match)]]
   [:div.team.team2
    {:class (when (< (:team1_wins match) (:team2_wins match))
              :winner)}
    (when (:team2_rank match)
      [:span.rank (str \( (:team2_rank match) \))])
    [:span.name (:team2_name match)]
    [:span.wins (:team2_wins match)]]])

(defn bracket [id]
  (let [data (subscribe [::subs/bracket id])]
    (fn bracket-render [id]
      [:div.bracket
       (for [round @data
             :let [num-matches (count round)
                   k (str id "-bracket-" num-matches)]]
         ^{:key k}
         [:div.bracket-round
          {:class (str "matches-" num-matches)}
          [:h3.hidden-desktop
           (str "Top " (* 2 num-matches))]
          (for [match round]
            ^{:key (str k "-table-" (:table_number match))}
            [bracket-match match])])])))
