(ns mtg-pairings.uploader
  (:require [org.httpkit.client :as http]
            [cheshire.core :as json]))

(defmacro response [callback]
  `(fn [response#]
     (let [callback# (or ~callback identity)] 
       (doto (update-in response# [:body] #(json/parse-string % true))
         callback#))))

(defmacro GET [url callback]
  `(http/get ~url {} (response ~callback)))

(defmacro POST [url options callback]
  `(http/post ~url ~options (response ~callback)))

(defmacro PUT [url options callback]
  `(http/put ~url ~options (response ~callback)))

(defn options [api-key body]
  {:timeout 1000
   :query-params {:key api-key}
   :body (json/generate-string body)})

(defn upload-tournament! [url api-key tournament & [callback]]
  (POST (str url "/tournament") 
        (options api-key tournament) 
        callback))

(defn upload-teams! [url tournament-id api-key teams & [callback]]
  (PUT (str url "/tournament/" tournament-id "/teams")
       (options api-key teams)
       callback))

(defn upload-seatings! [url tournament-id api-key seatings & [callback]]
  (PUT (str url "/tournament/" tournament-id "/seatings")
       (options api-key seatings)
       callback))

(defn upload-pairings! [url tournament-id round api-key pairings & [callback]]
  (PUT (str url "/tournament/" tournament-id "/round-" round "/pairings") 
       (options api-key pairings)
       callback))

(defn upload-results! [url tournament-id round api-key results & [callback]]
  (PUT (str url "/tournament/" tournament-id "/round-" round "/results") 
       (options api-key results)
       callback))