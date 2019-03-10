(ns mtg-pairings-server.transit
  (:refer-clojure :exclude [read])
  (:require [cognitect.transit :as transit]
            [mtg-pairings-server.util :refer [parse-iso-date format-iso-date]])
  #?(:clj
     (:import (org.joda.time LocalDate)
              (clojure.lang Ratio)
              (java.io ByteArrayInputStream ByteArrayOutputStream))))

(def writers
  {#?(:clj LocalDate, :cljs goog.date.Date)
   (transit/write-handler (constantly "Date") format-iso-date)
   #?@(:clj [Ratio
             (transit/write-handler (constantly "d") double)])})

(def readers
  {"Date" (transit/read-handler #(parse-iso-date %))})

(defn write [x]
  #?(:clj (let [out (ByteArrayOutputStream.)
                writer (transit/writer out :json {:handlers writers})]
            (transit/write writer x)
            (.toString out "UTF-8"))
     :cljs (let [writer (transit/writer :json {:handlers writers})]
             (transit/write writer x))))

(defn read [^String s]
  #?(:clj (let [in (ByteArrayInputStream. (.getBytes s "UTF-8"))
                reader (transit/reader in :json {:handlers readers})]
            (transit/read reader))
     :cljs (let [reader (transit/reader :json {:handlers readers})]
             (transit/read reader s))))
