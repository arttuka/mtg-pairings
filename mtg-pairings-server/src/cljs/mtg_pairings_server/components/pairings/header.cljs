(ns mtg-pairings-server.components.pairings.header
  (:require [reagent.core :as reagent :refer [atom]]
            [re-frame.core :refer [subscribe dispatch]]
            [reagent-material-ui.components :as ui]
            [reagent-material-ui.icons.menu :refer [menu] :rename {menu menu-icon}]
            [reagent-material-ui.styles :refer [with-styles]]
            [accountant.core :as accountant]
            [mtg-pairings-server.events.pairings :as events]
            [mtg-pairings-server.routes.pairings :refer [tournaments-path]]
            [mtg-pairings-server.subscriptions.pairings :as subs]
            [mtg-pairings-server.util.material-ui :refer [wrap-on-change]]))

(defn styles [{:keys [spacing shape]}]
  {:dci-container {:background-color "rgba(255, 255, 255, 0.2)"
                   "&:hover"         {:background-color "rgba(255, 255, 255, 0.3)"}
                   :width            120
                   :margin-right     (spacing 2)
                   :border-radius    (:border-radius shape)}
   :input         {:height  "20px"
                   :color   :white
                   :padding (spacing 1)}
   :separator     {:flex "1 0 0"}})

(defn ^:private header* [props]
  (let [user (subscribe [::subs/logged-in-user])
        dci-number (atom "")
        menu-anchor-ref (.createRef js/React)
        menu-open? (atom false)
        on-menu-click #(reset! menu-open? true)
        on-menu-close #(reset! menu-open? false)
        login! (fn []
                 (dispatch [::events/login @dci-number])
                 (reset! dci-number ""))]
    (fn [{:keys [classes]}]
      [ui/app-bar {:id       :header
                   :position :static}
       [ui/toolbar
        [ui/icon-button {:on-click on-menu-click
                         :color    :inherit
                         :ref      menu-anchor-ref}
         [menu-icon]]
        [ui/menu {:open      @menu-open?
                  :anchor-el (.-current menu-anchor-ref)
                  :on-close  on-menu-close}
         [ui/menu-item {:on-click #(do
                                     (on-menu-close)
                                     (accountant/navigate! "/"))}
          "Etusivu"]
         [ui/menu-item {:on-click #(do
                                     (on-menu-close)
                                     (accountant/navigate! (tournaments-path)))}
          "Turnausarkisto"]
         (when @user
           [ui/menu-item {:on-click #(do
                                       (on-menu-close)
                                       (dispatch [::events/logout]))}
            "Kirjaudu ulos"])]
        (when @user
          [ui/typography {:variant :h6}
           (:name @user)])
        [:div {:class (:separator classes)}]
        (when-not @user
          [:<>
           [:div {:class (:dci-container classes)}
            [ui/input-base {:placeholder "DCI-numero"
                            :value       @dci-number
                            :on-change   (wrap-on-change #(reset! dci-number %))
                            :on-key-down (fn [e]
                                           (when (= "Enter" (.-key e))
                                             (login!)))
                            :full-width  true
                            :classes     {:input (:input classes)}}]]
           [ui/button {:on-click login!
                       :color    :inherit}
            "Kirjaudu"]])]])))

(def header ((with-styles styles) header*))
