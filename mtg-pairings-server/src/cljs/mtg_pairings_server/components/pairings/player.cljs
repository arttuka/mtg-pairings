(ns mtg-pairings-server.components.pairings.player
  (:require [reagent.core :as reagent :refer [atom with-let]]
            [re-frame.core :refer [subscribe]]
            [reagent-material-ui.components :as ui]
            [reagent-material-ui.icons.list :refer [list] :rename {list list-icon}]
            [reagent-material-ui.styles :refer [with-styles]]
            [accountant.core :as accountant]
            [mtg-pairings-server.components.pairings.tournament :refer [tournament-header]]
            [mtg-pairings-server.routes.pairings :refer [standings-path]]
            [mtg-pairings-server.subscriptions.pairings :as subs]
            [mtg-pairings-server.util :refer [format-time-only]]
            [mtg-pairings-server.util.mtg :refer [bye?]]
            [mtg-pairings-server.util.styles :refer [on-desktop on-mobile]]))

(defn styles [{:keys [palette]}]
  {:names-container {:display :flex}
   :names           {:flex "1"}
   :points          {:flex "0 0 auto"}
   :mobile-block    {on-desktop {:display :inline-block}
                     on-mobile  {:display :block}}
   :hidden-mobile   {on-mobile {:display :none}}
   :avatar          {:background-color (get-in palette [:primary :main])
                     :font-weight      :bold}
   :box             {:flex "1"}
   :card-content    {:padding-top    0
                     :padding-bottom 0}})

(defn pairing* [{:keys [data classes translate] :as props}]
  (let [bye (bye? data)
        type (cond
               (:team-2-name data) :pairing
               (:pod data) :pod
               :else :seating)
        {:keys [names-container names points mobile-block hidden-mobile avatar]} classes]
    [ui/list-item (dissoc props :data :classes :translate)
     [ui/list-item-avatar
      [ui/avatar {:class avatar}
       (when-not bye
         (or (:table-number data) (:pod data)))]]
     [ui/list-item-text {:classes   {:secondary names-container}
                         :primary   (case type
                                      :pairing (str (translate :player.round (:round-number data))
                                                    (when-let [start-time (:created data)]
                                                      (translate :common.started-short (format-time-only start-time))))
                                      :pod (translate :player.pod (:pod data))
                                      :seating (translate :player.seating))
                         :secondary (reagent/as-element
                                     (if (= :pairing type)
                                       [:<>
                                        [:span {:class names}
                                         [:span.MuiTypography-colorTextPrimary {:class mobile-block}
                                          (str (:team-1-name data) " (" (:team-1-points data) ")")]
                                         [:span {:class hidden-mobile} " - "]
                                         [:span {:class mobile-block}
                                          (if bye
                                            (:team-2-name data)
                                            (str (:team-2-name data) " (" (:team-2-points data) ")"))]]
                                        (when-not bye
                                          [:span {:class points}
                                           [:span {:class mobile-block} (:team-1-wins data)]
                                           [:span {:class hidden-mobile} " - "]
                                           [:span {:class mobile-block} (:team-2-wins data)]])]
                                       [:span {:class names}
                                        (or (:team-1-name data)
                                            (translate :common.seat-n (:seat data)))]))}]]))

(def pairing ((with-styles styles) pairing*))

(defn combine-pairings-and-pods [pairings pods]
  (->> (concat pairings pods)
       (sort-by (juxt :round-number :team1-name))
       (reverse)))

(defn own-tournament* [{:keys [classes tournament]}]
  (with-let [translate (subscribe [::subs/translate])
             expanded? (atom false)
             on-expand #(swap! expanded? not)]
    [ui/list-item {:disable-gutters true
                   :divider         true}
     [ui/box {:class (:box classes)}
      [tournament-header {:data      tournament
                          :on-expand on-expand
                          :expanded? @expanded?}]
      [ui/collapse {:in @expanded?}
       [ui/card-content {:class (:card-content classes)}
        [ui/list
         [ui/list-item {:disable-gutters true
                        :button          true
                        :on-click        #(accountant/navigate! (standings-path {:id    (:id tournament)
                                                                                 :round (:max-standings-round tournament)}))}
          [ui/list-item-avatar
           [ui/avatar {:class (:avatar classes)}
            [list-icon]]]
          [ui/list-item-text {:primary (@translate :player.standings (:max-standings-round tournament))}]]
         (doall (for [p (combine-pairings-and-pods (:pairings tournament) (:pod-seats tournament))]
                  ^{:key [(:id tournament) (:round-number p) (:id p)]}
                  [pairing {:data            p
                            :disable-gutters true
                            :translate       @translate}]))
         (when (:seating tournament)
           [pairing {:data            (:seating tournament)
                     :disable-gutters true
                     :translate       @translate}])]]]]]))

(def own-tournament ((with-styles styles)
                     own-tournament*))
