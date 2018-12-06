(ns mtg-pairings-server.middleware.etag
  (:require [clojure.string :as str]
            [ring.middleware.not-modified :refer [wrap-not-modified]]
            [clj-time.format :as f]
            [clj-time.coerce :as c])
  (:import (java.security MessageDigest)
           (java.io File)))

(def rfc2616-formatter (f/formatter "EEE, dd MMM yyyy HH:mm:ss zzz"))

(defn to-hex-string [bytes]
  (str/join "" (map #(Integer/toHexString (bit-and % 0xff)) bytes)))

(defn sha1 [obj]
  (let [bytes (.getBytes (with-out-str (pr obj)))]
    (to-hex-string (.digest (MessageDigest/getInstance "SHA1") bytes))))

(defn calculate-response-etag [{:strs [Last-Modified Content-Length]}]
  (when (and Last-Modified Content-Length)
    (let [d (f/parse rfc2616-formatter Last-Modified)]
      (str (c/to-long d) \- Content-Length))))

(defmulti calculate-etag class)

(defmethod calculate-etag String [s]
  (sha1 s))

(defmethod calculate-etag File [f]
  (str (.lastModified f) \- (.length f)))

(defn wrap-etag [handler {paths :paths :or {paths [#".*"]}}]
  (fn [request]
    (let [{:keys [status body headers] :as response} (handler request)]
      (if (and (#{200 201 204} status) body (some #(re-matches % (:uri request)) paths))
        (-> response
            (assoc-in [:headers "cache-control"] "max-age=3600, must-revalidate")
            (assoc-in [:headers "etag"] (or (calculate-response-etag headers) (calculate-etag body))))
        response))))
