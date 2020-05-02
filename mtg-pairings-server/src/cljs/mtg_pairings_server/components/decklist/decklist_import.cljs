(ns mtg-pairings-server.components.decklist.decklist-import
  (:require [reagent.core :as reagent :refer [atom with-let]]
            [reagent.ratom :refer [make-reaction]]
            [re-frame.core :refer [subscribe dispatch]]
            [clojure.string :as str]
            [reagent-material-ui.colors :as colors]
            [reagent-material-ui.core.app-bar :refer [app-bar]]
            [reagent-material-ui.core.button :refer [button]]
            [reagent-material-ui.core.collapse :refer [collapse]]
            [reagent-material-ui.core.paper :refer [paper]]
            [reagent-material-ui.core.tab :refer [tab]]
            [reagent-material-ui.core.tabs :refer [tabs]]
            [reagent-material-ui.core.typography :refer [typography]]
            [reagent-material-ui.styles :refer [with-styles]]
            [mtg-pairings-server.events.decklist :as events]
            [mtg-pairings-server.subscriptions.decklist :as subs]
            [mtg-pairings-server.util.decklist :refer [->text card-types]]
            [mtg-pairings-server.util.material-ui :refer [text-field wrap-on-change]]
            [mtg-pairings-server.util.styles :refer [on-desktop]]))

(defn valid-code [address]
  (when address
    (let [[_ code] (re-find #"/([A-z0-9_-]{22})$" address)]
      code)))

(defn styles [{:keys [palette spacing]}]
  {:tab-panel-half    {:padding   (spacing 1)
                       on-desktop {:width          "50%"
                                   :display        :inline-block
                                   :vertical-align :top}}
   :address-import    {:height 68
                       :margin (spacing 1 0)}
   :error             {:color (get-in palette [:error :main])}
   :text-import       {:margin-bottom (spacing 1)}
   :text-import-input {:background-color (get colors/grey 100)}})

(defn subheader [text]
  [typography {:variant :h6}
   text])

(defn tab-panel [props & children]
  (into [paper {:role   :tabpanel
                :hidden (:hidden? props)}]
        children))

(defn previous-panel [props]
  (let [address (atom "")
        code (make-reaction #(valid-code @address))
        address-on-change #(reset! address %)
        import-from-address #(dispatch [::events/import-address @code])
        address-on-key-press (fn [^js/KeyboardEvent e]
                               (when (= "Enter" (.-key e))
                                 (import-from-address)))
        import-error (subscribe [::subs/error :import-address])
        translate (subscribe [::subs/translate])]
    (fn [{:keys [classes hidden?]}]
      (let [translate @translate]
        [tab-panel {:hidden? hidden?}
         [:div {:class (:tab-panel-half classes)}
          [subheader (translate :submit.load-previous.label)]
          [:p
           (translate :submit.load-previous.text.0)
           "https://decklist.pairings.fi/abcd..."
           (translate :submit.load-previous.text.1)]]
         [:div {:class (:tab-panel-half classes)}
          [text-field {:classes      {:root (:address-import classes)}
                       :on-change    address-on-change
                       :on-key-press address-on-key-press
                       :label        (translate :submit.load-previous.address)
                       :full-width   true
                       :error-text   (when-not (or (str/blank? @address)
                                                   @code)
                                       (translate :submit.error.address))}]
          [button {:disabled (nil? @code)
                   :variant  :contained
                   :color    :primary
                   :on-click import-from-address}
           (translate :submit.load)]
          (when @import-error
            [:p {:class (:error classes)}
             (translate (case @import-error
                          :not-found :submit.error.not-found
                          :submit.error.decklist-import-error))])]]))))

(defn text-panel [{:keys [close-panel]}]
  (let [decklist (atom "")
        decklist-on-change #(reset! decklist %)
        import-decklist (fn []
                          (dispatch [::events/import-text @decklist])
                          (close-panel))
        translate (subscribe [::subs/translate])]
    (fn [{:keys [classes hidden?]}]
      (let [translate @translate]
        [tab-panel {:hidden? hidden?}
         [:div {:class (:tab-panel-half classes)}
          [subheader (translate :submit.load-text.header)]
          [:p (translate :submit.load-text.info.0)]
          [:pre "4 Lightning Bolt\n4 Chain Lightning\n..."]
          [:p (translate :submit.load-text.info.1)]]
         [:div {:class (:tab-panel-half classes)}
          [text-field {:class       (:text-import classes)
                       :input-props {:class (:text-import-input classes)}
                       :on-change   decklist-on-change
                       :multiline   true
                       :rows        8
                       :full-width  true}]
          [button {:disabled (str/blank? @decklist)
                   :variant  :outlined
                   :on-click import-decklist}
           (translate :submit.load)]]]))))

(defn decklist-import* [{:keys [classes]}]
  (with-let [loaded? (subscribe [::subs/loaded?])
             translate (subscribe [::subs/translate])
             selected (atom false)
             selected-panel (atom nil)
             close-panel #(reset! selected false)
             on-select (fn [_ value]
                         (swap! selected #(if (not= value %)
                                            value
                                            false))
                         (when value
                           (reset! selected-panel value)))
             _ (add-watch loaded? ::decklist-import
                          (fn [_ _ _ new]
                            (when new
                              (close-panel))))]
    (let [translate @translate]
      [:<>
       [app-bar {:position :static}
        [tabs {:value     @selected
               :on-change on-select
               :variant   :fullWidth}
         [tab {:label (translate :submit.load-previous.label)
               :value "load-previous"}]
         [tab {:label (translate :submit.load-text.label)
               :value "load-text"}]]]
       [collapse {:in (boolean @selected)}
        [previous-panel {:classes classes
                         :hidden? (not= "load-previous" @selected-panel)}]
        [text-panel {:classes     classes
                     :hidden?     (not= "load-text" @selected-panel)
                     :close-panel close-panel}]]])
    (finally
      (remove-watch loaded? ::decklist-import))))

(def decklist-import ((with-styles styles) decklist-import*))
