(ns mtg-pairings-server.components.date-picker
  (:require [reagent.core :as reagent]
            [re-frame.core :refer [subscribe]]
            [material-ui-pickers]
            [date-io-moment]
            [cljsjs.moment]
            [cljsjs.moment.locale.fi]
            [cljs-time.coerce :as coerce]
            [oops.core :refer [oget]]
            [mtg-pairings-server.subscriptions.common :as subs]
            [mtg-pairings-server.util :as util]))

(def mui-date-picker (reagent/adapt-react-class (oget js/MaterialUIPickers "DatePicker")))
(def mui-date-time-picker (reagent/adapt-react-class (oget js/MaterialUIPickers "DateTimePicker")))
(def mui-pickers-utils-provider (reagent/adapt-react-class (oget js/MaterialUIPickers "MuiPickersUtilsProvider")))
(def moment-utils js/DateIOMomentUtils)

(defn ^:private local-date->moment [d]
  (when d
    (js/moment (util/format-iso-date d) "YYYY-MM-DD")))

(defn ^:private date-time->moment [d]
  (when d
    (js/moment (coerce/to-long d))))

(defn ^:private moment->local-date [d]
  (util/parse-iso-date (.format d "YYYY-MM-DD")))

(defn ^:private moment->date-time [d]
  (coerce/from-long (.valueOf d)))

(defn ^:private wrap-component [component moment->value value->moment]
  (fn [props]
    (let [on-change (fn [value]
                      ((:on-change props) (moment->value value)))
          language (subscribe [::subs/language])]
      (fn [props]
        (let [new-props (merge (dissoc props :value :on-change)
                               {:value     (value->moment (:value props))
                                :on-change on-change})]
          [mui-pickers-utils-provider {:utils  moment-utils
                                       :locale (name @language)}
           [component new-props]])))))

(def date-picker (wrap-component mui-date-picker moment->local-date local-date->moment))

(def date-time-picker (wrap-component mui-date-time-picker moment->date-time date-time->moment))
