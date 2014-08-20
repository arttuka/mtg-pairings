(ns mtg-pairings.uploader
  (:require [org.httpkit.client :as http]
            [cheshire.core :as json]
            [mtg-pairings.watcher :as watcher]
            [mtg-pairings.util :refer [->edn]]))

(defmacro response [callback]
  `(fn [response#]
     (if (#{200 204} (:status response#))
       (let [callback# (or ~callback identity)
             parsed-response# (update-in response# [:body] #(json/parse-string % true))]
         (doto parsed-response# callback#))
       (throw (ex-info "HTTP Error" response#)))))

(defmacro GET [url callback]
  `(http/get ~url {} (response ~callback)))

(defmacro POST [url options callback]
  `(http/post ~url ~options (response ~callback)))

(defmacro PUT [url options callback]
  `(http/put ~url ~options (response ~callback)))

(defmacro DELETE [url options callback]
  `(http/delete ~url ~options (response ~callback)))

(declare upload-teams! upload-pairings! upload-results! upload-seatings! upload-tournament!)

(defn check-dependency! [settings tournament-id type round callback]
  (case type
    :teams (when-not (watcher/uploaded? tournament-id :tournament)
             @(upload-tournament! settings tournament-id))
    :seatings (when-not (watcher/uploaded? tournament-id :teams)
                @(upload-teams! settings tournament-id))
    :pairings (do 
                (when-not (watcher/uploaded? tournament-id :teams)
                  @(upload-teams! settings tournament-id))
                (let [prev (dec round)]
                  (when (and (pos? prev) (not (watcher/uploaded? tournament-id :results prev)))
                    @(upload-results! settings tournament-id prev))))
    :results (when-not (watcher/uploaded? tournament-id :pairings round)
                @(upload-pairings! settings tournament-id round))
    :publish (when-not (watcher/uploaded? tournament-id :results round)
               @(upload-results! settings tournament-id round))
    nil)
  (callback))

(defn options [api-key body]
  {:timeout 1000
   :query-params {:key api-key}
   :body (->edn body)
   :headers {"Content-Type" "application/edn"}
   :as :text})

(defn upload-tournament! [{:keys [url api-key] :as settings} tournament-id & [callback]]
  (check-dependency! settings tournament-id :tournament nil 
                    #(POST (str url "/tournament/") 
                           (options api-key (watcher/get-tournament tournament-id)) 
                           (fn [response]
                             (watcher/set-uploaded! tournament-id :tournament)
                             (when callback (callback))))))

(defn upload-teams! [{:keys [url api-key sanction-id] :as settings} tournament-id & [callback]]
  (check-dependency! settings tournament-id :teams nil 
                    #(PUT (str url "/tournament/" sanction-id "/teams")
                           (options api-key (watcher/get-teams tournament-id))
                           (fn [response]
                             (watcher/set-uploaded! tournament-id :teams)
                             (when callback (callback))))))

(defn upload-seatings! [{:keys [url api-key sanction-id] :as settings} tournament-id & [callback]]
  (check-dependency! settings tournament-id :seatings nil 
  #(PUT (str url "/tournament/" sanction-id "/seatings")
        (options api-key (watcher/get-seatings tournament-id))
        (fn [response]
          (watcher/set-uploaded! tournament-id :seatings)
          (when callback (callback))))))

(defn upload-pairings! [{:keys [url api-key sanction-id] :as settings} tournament-id round & [callback]]
  (check-dependency! settings tournament-id :pairings round 
                    #(PUT (str url "/tournament/" sanction-id "/round-" round "/pairings") 
                           (options api-key (watcher/get-pairings tournament-id round))
                           (fn [response]
                             (watcher/set-uploaded! tournament-id :pairings round)
                             (when callback (callback))))))

(defn upload-results! [{:keys [url api-key sanction-id] :as settings} tournament-id round & [callback]]
  (check-dependency! settings tournament-id :results round 
                    #(PUT (str url "/tournament/" sanction-id "/round-" round "/results") 
                           (options api-key (watcher/get-results tournament-id round))
                           (fn [response]
                             (watcher/set-uploaded! tournament-id :results round)
                             (when callback (callback))))))

(defn publish-results! [{:keys [url api-key sanction-id] :as settings} tournament-id round & [callback]]
  (check-dependency! settings tournament-id :publish round
                     #(PUT (str url "/tournament/" sanction-id "/round-" round "/results/publish")
                            (options api-key nil)
                            (fn [response]
                              (when callback (callback))))))

(defn reset-tournament! [{:keys [url api-key sanction-id]} tournament-id & [callback]]
  (DELETE (str url "/tournament/" sanction-id)
          (options api-key nil)
          (fn [response]
            (watcher/reset-tournament! tournament-id)
            (when callback (callback)))))
