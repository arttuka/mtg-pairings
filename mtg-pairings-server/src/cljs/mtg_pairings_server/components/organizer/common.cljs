(ns mtg-pairings-server.components.organizer.common
  (:require [reagent-material-ui.components :as ui]
            [reagent-material-ui.styles :refer [with-styles]]
            [mtg-pairings-server.util.styles :refer [ellipsis-overflow]]))

(def number-style {:text-align  :center
                   :font-weight :bold
                   :font-size   20
                   :flex        "0 0 40px"})

(def player-style (merge {:flex 1}
                         ellipsis-overflow))

(def column ((with-styles {:root        {:display        :flex
                                         :flex-direction :column
                                         :flex-wrap      :wrap
                                         :align-content  :space-around
                                         :align-items    :center
                                         :font-size      "16px"
                                         :line-height    "24px"
                                         :overflow       :hidden}
                           :menu-shown  {:height "calc(100vh - 103px)"}
                           :menu-hidden {:height "calc(100vh - 47px)"}})
             (fn column [{:keys [classes menu-hidden? children]}]
               [:div {:class [(:root classes)
                              (if menu-hidden?
                                (:menu-hidden classes)
                                (:menu-shown classes))]}
                children])))

(def row ((with-styles (fn [{:keys [palette]}]
                         {:root {:display           :flex
                                 :width             470
                                 "&:nth-child(odd)" {:background-color (get-in palette [:grey 300])}}}))
          (fn row [{:keys [class-name classes children]}]
            [:div {:class [class-name (:root classes)]}
             children])))

(def header ((with-styles (fn [{:keys [spacing]}]
                            {:root {:margin-top    (spacing 1)
                                    :margin-bottom (spacing 1)}}))
             ui/typography))
