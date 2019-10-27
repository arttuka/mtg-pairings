(ns mtg-pairings-server.components.decklist.decklist-table
  (:require [reagent.core :as reagent :refer [atom]]
            [re-frame.core :refer [subscribe dispatch]]
            [reagent-material-ui.components :as ui]
            [reagent-material-ui.icons.delete-icon :refer [delete]]
            [reagent-material-ui.styles :refer [with-styles]]
            [mtg-pairings-server.components.decklist.icons :refer [error-icon]]
            [mtg-pairings-server.events.decklist :as events]
            [mtg-pairings-server.subscriptions.decklist :as subs]
            [mtg-pairings-server.util.decklist :refer [->text card-types decklist-errors basic?]]
            [mtg-pairings-server.util.mtg :refer [valid-dci?]]
            [mtg-pairings-server.util.material-ui :as mui-util :refer [wrap-on-change]]))

(defn styles [{:keys [spacing] :as theme}]
  (let [on-desktop (mui-util/on-desktop theme)
        table-cell-style #(apply merge {:padding   0
                                        :font-size 16}
                                 %&)]
    {:root            {on-desktop {:width          "50%"
                                   :display        :inline-block
                                   :vertical-align :top}}
     :header-cell     {:font-size 16
                       :padding   (spacing 1 0)}
     :type-header     (table-cell-style {:font-weight :bold
                                         :padding     (spacing 1 0)})
     :quantity-column (table-cell-style {:width      60
                                         :text-align :center})
     :quantity-header {:text-align :center}
     :card-column     (table-cell-style {on-desktop {:width "calc(50% - 156px)"}})
     :error-column    (table-cell-style {:width   48
                                         :height  49
                                         :padding 12})
     :action-column   (table-cell-style {:width  48
                                         :height 49})
     :quantity-select {:width 48}}))

(defn cards-with-error [decklist]
  (into {}
        (comp (filter #(= :card-over-4 (:type %)))
              (map (juxt :card :text)))
        (decklist-errors decklist)))

(defn decklist-table-row [board card error classes]
  (let [translate (subscribe [::subs/translate])
        on-change (wrap-on-change #(dispatch [::events/set-quantity board (:id card) %]))
        on-delete #(dispatch [::events/remove-card board (:id card)])]
    (fn decklist-table-row-render [_ card error classes]
      (let [translate @translate]
        [ui/table-row
         [ui/table-cell {:class (:quantity-column classes)}
          (when (:quantity card)
            (into [ui/select {:value     (:quantity card)
                              :on-change on-change
                              :class     (:quantity-select classes)}]
                  (for [i (range 1 (if (basic? (:name card))
                                     31
                                     5))]
                    [ui/menu-item {:value i}
                     (str i)])))]
         [ui/table-cell {:class (:card-column classes)}
          (:name card)]
         [ui/table-cell {:class (:error-column classes)}
          (when error
            [error-icon {:error (translate (str "submit.error." (name error)))}])]
         [ui/table-cell {:class (:action-column classes)}
          [ui/icon-button {:on-click on-delete}
           [delete]]]]))))

(defn table-body-by-type [decklist board error-cards translate classes]
  (mapcat (fn [type]
            (when-let [cards (get-in decklist [board type])]
              (list*
               ^{:key (str (name type) "--header")}
               [ui/table-row
                [ui/table-cell {:class (:quantity-column classes)}]
                [ui/table-cell {:class (:type-header classes)}
                 (translate (str "card-type." (name type)))]
                [ui/table-cell {:class (:error-column classes)}]
                [ui/table-cell {:class (:action-column classes)}]]
               (for [{:keys [id name error] :as card} cards]
                 ^{:key (str id "--tr")}
                 [decklist-table-row board card (or error (get error-cards name)) classes]))))
          card-types))

(defn decklist-table* [props]
  (let [translate (subscribe [::subs/translate])
        decklist (subscribe [::subs/decklist-by-type])]
    (fn decklist-table-render [{:keys [classes board]}]
      (let [translate @translate
            error-cards (cards-with-error @decklist)
            board (keyword board)]
        [:div {:class (:root classes)}
         [ui/typography {:variant :h6}
          (if (= :main board)
            (str "Main deck (" (get-in @decklist [:count :main]) ")")
            (str "Sideboard (" (get-in @decklist [:count :side]) ")"))]
         [ui/table
          [ui/table-head
           [ui/table-row
            [ui/table-cell {:class [(:header-cell classes) (:quantity-header classes)]}
             (translate :submit.quantity)]
            [ui/table-cell {:class (:header-cell classes)}
             (translate :submit.card)]
            [ui/table-cell {:class (:error-column classes)}]
            [ui/table-cell {:class (:action-column classes)}]]]
          [ui/table-body
           (case board
             :main (table-body-by-type @decklist :main error-cards translate classes)
             :side (for [{:keys [id name error] :as card} (:side @decklist)]
                     ^{:key (str id "--tr")}
                     [decklist-table-row :side card (or error (get error-cards name)) classes]))]]]))))

(def decklist-table ((with-styles styles) decklist-table*))
