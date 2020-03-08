(ns mtg-pairings-server.components.pairings.header
  (:require [reagent.core :as reagent :refer [atom with-let]]
            [re-frame.core :refer [subscribe dispatch]]
            [reagent-material-ui.core.app-bar :refer [app-bar]]
            [reagent-material-ui.core.button :refer [button]]
            [reagent-material-ui.core.icon-button :refer [icon-button]]
            [reagent-material-ui.core.input-base :refer [input-base]]
            [reagent-material-ui.core.link :refer [link]]
            [reagent-material-ui.core.menu :refer [menu]]
            [reagent-material-ui.core.menu-item :refer [menu-item]]
            [reagent-material-ui.core.toolbar :refer [toolbar]]
            [reagent-material-ui.core.typography :refer [typography]]
            [reagent-material-ui.icons.menu :refer [menu] :rename {menu menu-icon}]
            [reagent-material-ui.styles :refer [with-styles]]
            [accountant.core :as accountant]
            [mtg-pairings-server.components.language-selector :refer [language-selector]]
            [mtg-pairings-server.events.pairings :as events]
            [mtg-pairings-server.routes.pairings :refer [tournaments-path]]
            [mtg-pairings-server.subscriptions.pairings :as subs]
            [mtg-pairings-server.util.material-ui :refer [wrap-on-change]]
            [mtg-pairings-server.util.styles :refer [on-desktop on-mobile]]
            [clojure.string :as str]))

(defn ^:private invalid-dci? [value]
  (not (re-matches #"\d*" value)))

(defn ^:private login-form [classes]
  (with-let [value (atom "")
             on-change (wrap-on-change #(reset! value %))]
    (let [translate @(subscribe [::subs/translate])
          invalid? (invalid-dci? @value)]
      [:form {:action (str "/dci-login?next=" (.. js/window -location -pathname))
              :method :post
              :class  (:form classes)}
       [:input {:type  :hidden
                :name  :__anti-forgery-token
                :value js/csrfToken}]
       [:div {:class [(:dci-container classes)
                      (when invalid? (:error classes))]}
        [input-base {:placeholder (translate :header.dci)
                     :name        :dci
                     :full-width  true
                     :value       @value
                     :on-change   on-change
                     :error       invalid?
                     :classes     {:input (:input classes)}}]]
       [button {:type     :submit
                :color    :inherit
                :class    (:login-button classes)
                :disabled (or (str/blank? @value) invalid?)}
        (translate :header.login)]])))

(defn styles [{:keys [spacing shape]}]
  {:dci-container     {:background-color "rgba(255, 255, 255, 0.2)"
                       "&:hover"         {:background-color "rgba(255, 255, 255, 0.3)"}
                       "&$error"         {:background-color "rgb(245, 147, 147)"
                                          "&:hover"         {:background-color "rgb(245, 122, 122)"}}
                       :width            120
                       :margin-right     (spacing 2)
                       :border-radius    (:border-radius shape)}
   :error             {}
   :input             {:height  "20px"
                       :color   :white
                       :padding (spacing 1)}
   :form              {:display :flex}
   :separator         {:flex "1 0 0"}
   :language-selector {on-mobile     {:display :none}
                       :margin-right (spacing 2)}
   :mobile            {on-desktop {:display :none}}
   :login-button      {:width 90}})

(defn ^:private header* [{:keys [classes]}]
  (with-let [user (subscribe [::subs/logged-in-user])
             ^js/React.Ref menu-anchor-ref (.createRef js/React)
             menu-open? (atom false)
             on-menu-click #(reset! menu-open? true)
             on-menu-close #(reset! menu-open? false)]
    (let [translate @(subscribe [::subs/translate])]
      [app-bar {:id       :header
                :position :static}
       [toolbar
        [icon-button {:on-click on-menu-click
                      :color    :inherit
                      :ref      menu-anchor-ref}
         [menu-icon]]
        [menu {:open      @menu-open?
               :anchor-el (.-current menu-anchor-ref)
               :on-close  on-menu-close}
         [menu-item {:class (:mobile classes)}
          [language-selector {:on-click on-menu-close}]]
         [menu-item {:on-click #(do
                                  (on-menu-close)
                                  (accountant/navigate! "/"))}
          (translate :header.front-page)]
         [menu-item {:on-click #(do
                                  (on-menu-close)
                                  (accountant/navigate! (tournaments-path)))}
          (translate :header.archive)]
         (when @user
           [menu-item {}
            [link {:color     :textPrimary
                   :href      "/dci-logout"
                   :underline :none}
             (translate :header.logout)]])]
        (when @user
          [typography {:variant :h6}
           (:name @user)])
        [:div {:class (:separator classes)}]
        [language-selector {:invert     true
                            :class-name (:language-selector classes)}]
        (when-not @user
          [login-form classes])]])))

(def header ((with-styles styles) header*))
