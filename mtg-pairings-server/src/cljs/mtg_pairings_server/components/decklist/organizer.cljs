(ns mtg-pairings-server.components.decklist.organizer
  (:require [reagent.core :as reagent :refer [atom]]
            [re-frame.core :refer [subscribe dispatch]]
            [reagent-material-ui.components :as ui]
            [reagent-material-ui.styles :refer [with-styles]]
            [mtg-pairings-server.components.decklist.print :refer [render-decklist]]
            [mtg-pairings-server.subscriptions.decklist :as subs]))

(defn view-decklist []
  (let [decklist (subscribe [::subs/decklist-by-type])
        tournament (subscribe [::subs/organizer-tournament])
        translate (subscribe [::subs/translate])]
    (fn []
      [render-decklist {:decklist   @decklist
                        :tournament @tournament
                        :translate  @translate}])))

(defn view-decklists []
  (let [decklists (subscribe [::subs/decklists-by-type])
        tournament (subscribe [::subs/organizer-tournament])
        translate (subscribe [::subs/translate])
        printed? (clojure.core/atom false)
        print-page #(when (and (seq @decklists)
                               @tournament
                               (not @printed?))
                      (reset! printed? true)
                      (.print js/window))]
    (reagent/create-class
     {:component-did-mount  print-page
      :component-did-update print-page
      :reagent-render       (fn view-decklists-render []
                              [:div
                               (doall (for [decklist @decklists]
                                        ^{:key (:id decklist)}
                                        [render-decklist {:decklist   decklist
                                                          :tournament @tournament
                                                          :translate  @translate}]))])})))

(defn login-styles [{:keys [spacing]}]
  {:root-container  {:padding (spacing 2)}
   :field-container {:display     :flex
                     :align-items :flex-end}
   :field           {:margin         (spacing 0 1)
                     "&:first-child" {:margin-left 0}}})

(defn login* [props]
  (let [translate (subscribe [::subs/translate])]
    (fn login-render [{:keys [classes]}]
      (let [translate @translate]
        [:div {:class (:root-container classes)}
         [:p (translate :organizer.log-in.text)]
         [:form {:action (str "/login?next=" (.. js/window -location -pathname))
                 :method :post}
          [:input {:type  :hidden
                   :name  :__anti-forgery-token
                   :value js/csrfToken}]
          [:div {:class (:field-container classes)}
           [ui/text-field {:class (:field classes)
                           :name  :username
                           :label (translate :organizer.log-in.username)}]
           [ui/text-field {:class (:field classes)
                           :name  :password
                           :type  :password
                           :label (translate :organizer.log-in.password)}]
           [ui/button {:class   (:field classes)
                       :type    :submit
                       :variant :contained
                       :color   :primary}
            (translate :organizer.log-in.button)]]]]))))

(def login ((with-styles login-styles) login*))

