(ns mtg-pairings-server.util
  (:require #?(:clj  [clojure.core.async :refer [<! alts! chan go-loop put! timeout sliding-buffer]]
               :cljs [cljs.core.async :refer [<! alts! chan put! timeout sliding-buffer] :refer-macros [go-loop]])
            [#?(:clj  clj-time.format
                :cljs cljs-time.format)
             :as format]
            [#?(:clj  clj-time.core
                :cljs cljs-time.core)
             :as time]
            [#?(:clj  clj-time.coerce
                :cljs cljs-time.coerce)
             :as coerce]
            #?(:clj
               [ring.util.response :as ring])
            #?@(:cljs
                [[goog.string :as gstring]
                 [goog.string.format]
                 [oops.core :refer [oget]]])
            [clojure.string :as str]))

(defn map-values
  [m f]
  (persistent!
   (reduce-kv (fn [acc k v]
                (assoc! acc k (f v)))
              (transient {})
              m)))

(defn select-and-rename-keys
  [map keys]
  (persistent!
   (reduce (fn [acc k]
             (let [[from to] (if (coll? k)
                               k
                               [k k])]
               (if-let [entry (find map from)]
                 (assoc! acc to (val entry))
                 acc)))
           (transient {})
           keys)))

(defn parse-iso-date [date]
  (some->> date (format/parse-local-date (format/formatters :year-month-day))))

(defn format-iso-date [date]
  (some->> date (format/unparse-local-date (format/formatters :year-month-day))))

(defn format-date [date]
  (some->> date (format/unparse-local-date (format/formatter "dd.MM.yyyy"))))

(defn parse-iso-date-time [datetime]
  (some->> datetime (format/parse (format/formatters :date-time))))

(defn format-iso-date-time [datetime]
  (some->> datetime (format/unparse (format/formatters :date-time))))

(defn format-date-time [datetime]
  (some->> datetime (format/unparse (format/formatter "dd.MM.yyyy HH:mm"))))

(defn today-or-yesterday? [date]
  (let [yesterday (time/minus (time/today) (time/days 1))
        tomorrow (time/plus (time/today) (time/days 1))]
    (time/within? (time/interval yesterday tomorrow) date)))

(defn to-local-date [d]
  #?(:clj  (coerce/to-local-date d)
     :cljs (-> d
               coerce/to-date-time
               time/to-default-time-zone
               time/from-utc-time-zone
               coerce/to-local-date)))

(defn group-kv [keyfn valfn coll]
  (persistent!
   (reduce (fn [acc x]
             (let [k (keyfn x)
                   v (valfn x)]
               (assoc! acc k (conj (get acc k []) v))))
           (transient {})
           coll)))

(defn extract-list [k coll]
  (for [[m v] (group-kv #(dissoc % k) #(get % k) coll)]
    (assoc m k v)))

(defn map-by [f coll]
  (into {} (map (juxt f identity)) coll))

(defn indexed [coll]
  (map-indexed vector coll))

(defn assoc-in-many [m & kvs]
  (reduce (fn [m [ks v]] (assoc-in m ks v)) m (partition 2 kvs)))

(defn dissoc-in [m [k & ks]]
  (if ks
    (update m k dissoc-in ks)
    (dissoc m k)))

(defn round-up [n m]
  (* m (quot (+ n m -1) m)))

(defn deep-merge
  ([m1 m2]
   (merge-with #(if (map? %1)
                  (deep-merge %1 %2)
                  %2)
               m1 m2)))

(defn some-value [pred coll]
  (first (filter pred coll)))

(defn index-where [pred coll]
  (loop [i 0
         [x & xs] coll]
    (cond
      (pred x) i
      (empty? xs) nil
      :else (recur (inc i) xs))))

(defn dissoc-index [v index]
  (vec (concat (subvec v 0 index) (subvec v (inc index)))))

#?(:clj
   (defn response [body]
     (if body
       (ring/response body)
       (ring/not-found body))))

(defn cls [class-defs]
  (str/join " " (for [[c used?] class-defs
                      :when used?]
                  (name c))))

#?(:cljs
   (defn round [num]
     (if (neg? num)
       (Math/ceil num)
       (Math/floor num))))

#?(:cljs
   (defn format-time [seconds]
     (let [sign (if (neg? seconds) "-" "")
           seconds (Math/floor seconds)
           minutes (Math/abs (round (/ seconds 60)))
           seconds (mod (Math/abs seconds) 60)]
       (gstring/format "%s%02d:%02d" sign minutes seconds))))

(defn debounce [f ms]
  (let [c (chan (sliding-buffer 1))]
    (go-loop [args (<! c)]
      (let [[val port] (alts! [c (timeout ms)])]
        (if (= port c)
          (recur val)
          (do
            (apply f args)
            (recur (<! c))))))
    (fn [& args]
      (put! c (or args [])))))

#?(:cljs
   (defn get-host []
     (str (oget js/window "location" "protocol")
          "//"
          (oget js/window "location" "host"))))

(defn valid-email? [email]
  (some->> email
           (re-matches #".+@.+")))
