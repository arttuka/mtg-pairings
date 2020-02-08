(ns mtg-pairings-server.transit
  (:refer-clojure :exclude [read])
  (:require [cognitect.transit :as transit]
            [mtg-pairings-server.util :refer [parse-iso-date parse-iso-date-time
                                              format-iso-date format-iso-date-time]])
  #?(:clj
     (:import (org.joda.time LocalDate DateTime)
              (clojure.lang Ratio)
              (java.io ByteArrayInputStream ByteArrayOutputStream))
     :cljs
     (:import (goog.date Date UtcDateTime))))

(def writers
  (let [date-writer (transit/write-handler (constantly "Date") format-iso-date)
        datetime-writer (transit/write-handler (constantly "DateTime") format-iso-date-time)]
    #?(:clj  {LocalDate date-writer
              DateTime  datetime-writer
              Ratio     (transit/write-handler (constantly "d") double)}
       :cljs {Date        date-writer
              UtcDateTime datetime-writer})))

(def readers
  {"Date"     (transit/read-handler parse-iso-date)
   "DateTime" (transit/read-handler parse-iso-date-time)})

(defn write [x]
  #?(:clj  (let [out (ByteArrayOutputStream.)
                 writer (transit/writer out :json {:handlers writers})]
             (transit/write writer x)
             (.toString out "UTF-8"))
     :cljs (let [writer (transit/writer :json {:handlers writers})]
             (transit/write writer x))))

(defn read [^String s]
  #?(:clj  (let [in (ByteArrayInputStream. (.getBytes s "UTF-8"))
                 reader (transit/reader in :json {:handlers readers})]
             (transit/read reader))
     :cljs (let [reader (transit/reader :json {:handlers readers})]
             (transit/read reader s))))
