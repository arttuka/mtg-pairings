(ns mtg-pairings-server.components.decklist.organizer
  (:require [reagent.core :as reagent :refer [atom]]
            [re-frame.core :refer [subscribe dispatch]]
            [reagent-material-ui.components :as ui]
            [reagent-material-ui.styles :refer [styled with-styles]]
            [oops.core :refer [oget]]
            [mtg-pairings-server.components.decklist.print :refer [render-decklist]]
            [mtg-pairings-server.subscriptions.decklist :as subs]
            [mtg-pairings-server.util.material-ui :refer [text-field wrap-on-change]]))

(defn view-decklist []
  (let [decklist (subscribe [::subs/decklist-by-type])
        tournament (subscribe [::subs/organizer-tournament])
        translate (subscribe [::subs/translate])]
    (fn view-decklist-render []
      [render-decklist @decklist @tournament @translate])))

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
                                        [render-decklist decklist @tournament @translate]))])})))

(defn ^:private no-op [])

(defn login []
  (let [translate (subscribe [::subs/translate])]
    (fn login-render []
      (let [translate @translate]
        [:div#decklist-organizer-login
         [:p (translate :organizer.log-in.text)]
         [:form {:action (str "/login?next=" (oget js/window "location" "pathname"))
                 :method :post}
          [:input {:type  :hidden
                   :name  :__anti-forgery-token
                   :value (oget js/window "csrf_token")}]
          [text-field {:name      :username
                       :label     (translate :organizer.log-in.username)
                       :on-change no-op
                       :style     {:margin "0 8px"}}]
          [text-field {:name      :password
                       :type      :password
                       :label     (translate :organizer.log-in.password)
                       :on-change no-op
                       :style     {:margin "0 8px"}}]
          [ui/button {:type    :submit
                      :variant :outlined
                      :color   :primary
                      :style   {:margin         "0 8px"
                                :vertical-align :bottom}}
           (translate :organizer.log-in.button)]]]))))
