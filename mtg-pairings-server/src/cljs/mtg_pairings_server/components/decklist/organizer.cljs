(ns mtg-pairings-server.components.decklist.organizer
  (:require [reagent.core :as reagent :refer [atom]]
            [re-frame.core :refer [subscribe dispatch]]
            [cljsjs.material-ui]
            [cljs-react-material-ui.reagent :as ui]
            [mtg-pairings-server.events :as events]
            [mtg-pairings-server.routes :as routes]
            [mtg-pairings-server.subscriptions :as subs]
            [mtg-pairings-server.util :refer [format-date format-date-time]]))

(defn tournament-row [tournament]
  (let [column-style {:font-size "14px"}
        edit-url (routes/decklist-organizer-tournament-path {:id (:id tournament)})
        submit-url (routes/new-decklist-submit-path {:id (:id tournament)})]
    [ui/table-row
     [ui/table-row-column {:class-name :date
                           :style      column-style}
      [:a.tournament-link {:href edit-url}
       (format-date (:date tournament))]]
     [ui/table-row-column {:class-name :deadline
                           :style      column-style}
      [:a.tournament-link {:href edit-url}
       (format-date-time (:deadline tournament))]]
     [ui/table-row-column {:class-name :name
                           :style      column-style}
      [:a.tournament-link {:href edit-url}
       (:name tournament)]]
     [ui/table-row-column {:class-name :decklists
                           :style      column-style}
      [:a.tournament-link {:href edit-url}
       (:decklist tournament)]]
     [ui/table-row-column {:class-name :submit-page
                           :style      column-style}
      [:a {:href submit-url}
       (str "https://pairings.fi" submit-url)]]]))

(defn all-tournaments []
  (let [tournaments (subscribe [::subs/decklist-organizer-tournaments])
        header-style {:color       :black
                      :font-weight :bold
                      :font-size   "16px"
                      :height      "36px"}]
    (fn all-tournaments-render []
      [:div#decklist-organizer-tournaments
       [ui/table {:selectable false
                  :class-name :tournaments}
        [ui/table-header {:display-select-all  false
                          :adjust-for-checkbox false}
         [ui/table-row {:style {:height "24px"}}
          [ui/table-header-column {:class-name :date
                                   :style      header-style}
           "Päivä"]
          [ui/table-header-column {:class-name :deadline
                                   :style      header-style}
           "Deadline"]
          [ui/table-header-column {:class-name :name
                                   :style      header-style}
           "Turnaus"]
          [ui/table-header-column {:class-name :decklists
                                   :style      header-style}
           "Dekkilistoja"]
          [ui/table-header-column {:class-name :submit-page
                                   :style      header-style}
           "Listojen lähetyssivu"]]]
        [ui/table-body
         (for [tournament @tournaments]
           ^{:key (str (:id tournament) "--row")}
           [tournament-row tournament])]]])))

(defn tournament [id])
