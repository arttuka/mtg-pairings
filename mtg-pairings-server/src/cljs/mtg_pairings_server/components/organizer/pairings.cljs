(ns mtg-pairings-server.components.organizer.pairings
  (:require [reagent.core :refer [atom]]
            [re-frame.core :refer [dispatch subscribe]]
            [reagent-material-ui.components :as ui]
            [reagent-material-ui.icons.zoom-out-map :refer [zoom-out-map]]
            [reagent-material-ui.styles :refer [styled with-styles]]
            [goog.string :as gstring]
            [mtg-pairings-server.components.organizer.menu :refer [round-select]]
            [mtg-pairings-server.events.pairings :as events]
            [mtg-pairings-server.subscriptions.pairings :as subs]
            [mtg-pairings-server.util :refer [cls indexed]]
            [mtg-pairings-server.util.material-ui :refer [wrap-on-change]]
            [mtg-pairings-server.util.mtg :refer [bye? duplicate-pairings]]))

(defn pairing-styles [{:keys [palette]}]
  (println "palette" palette)
  {:container    {:display :flex
                  :width   470
                  "&:not($byeContainer):nth-child(odd)" {:background-color (get-in palette [:grey :300])}}
   :bye-container {:background-color (get-in palette [:primary :100])}
   :table-number {:font-weight :bold
                  :font-size   20
                  :flex        "0 0 40px"}
   :player       {:flex 1}
   :points       {:flex "0 0 25px"}})

(defn pairing* [{:keys [classes data]}]
  (let [bye (bye? data)]
    [:div {:class [(:container classes)
                   (when bye (:bye-container classes))]}
     [:span {:class (:table-number classes)}
      (when-not bye (:table-number data))]
     [:span {:class (:player classes)}
      (:team-1-name data)]
     [:span {:class (:points classes)}
      (:team-1-points data)]
     [:span {:class (:player classes)}
      (:team-2-name data)]
     [:span {:class (:points classes)}
      (when-not bye (:team-2-points data))]]))

(def pairing ((with-styles pairing-styles) pairing*))

(def column (styled :div (fn [{:keys [menuhidden theme]}]
                           (let [{:keys [spacing]} theme
                                 top (+ (spacing 2)
                                        31
                                        (if menuhidden 0 56))]
                             {:display        :flex
                              :flex-direction :column
                              :flex-wrap      :wrap
                              :align-content  :space-around
                              :align-items    :center
                              :font-size      "16px"
                              :line-height    "24px"
                              :height         (str "calc(100vh - " top "px)")
                              :width          "100vw"
                              :overflow       :hidden}))))

(def header ((with-styles (fn [{:keys [spacing]}]
                            {:root {:margin-top    (spacing 1)
                                    :margin-bottom (spacing 1)}}))
             ui/typography))

(defn pairings [menu-hidden]
  (let [pairings (subscribe [::subs/organizer :pairings])
        pairings-round (subscribe [::subs/organizer :pairings-round])
        tournament (subscribe [::subs/organizer :tournament])]
    (fn pairings-render [menu-hidden]
      [:<>
       [header {:variant :h5
                :align   :center}
        (str (:name @tournament) " - kierros " @pairings-round)]
       [column {:menuhidden menu-hidden}
        (for [p (sort-by :team1_name (duplicate-pairings @pairings))]
          ^{:key (:team1_name p)}
          [pairing {:data p}])]])))
