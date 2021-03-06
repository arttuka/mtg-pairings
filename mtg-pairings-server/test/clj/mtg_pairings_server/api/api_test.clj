(ns mtg-pairings-server.api.api-test
  (:require [clojure.test :refer :all]
            [clojure.edn :as edn]
            [config.core :refer [env]]
            [clj-time.core :as time]
            [korma.core :as sql]
            [ring.mock.request :as mock]
            [mtg-pairings-server.sql-db :as db]
            [mtg-pairings-server.test-util :refer [db-fixture erase-fixture users]]
            [mtg-pairings-server.handler :refer [app]])
  (:import (java.io BufferedInputStream)))

(use-fixtures :once db-fixture)
(use-fixtures :each erase-fixture)

(defn add-apikey
  ([request]
   (add-apikey request (get-in users [:user1 :uuid])))
  ([request key]
   (mock/query-string request {:key key})))

(defn ->string [body]
  (condp instance? body
    String body
    BufferedInputStream (slurp body)))

(defn make-request
  ([request]
   (make-request request 200))
  ([request status]
   (let [response (app request)]
     (is (= status (:status response)) (str "Unexpected status code, response body: " (->string (:body response))))
     response)))

(def sanction-id "1-123456")

(deftest api-test
  (testing "POST /api/tournament"
    (testing "inserts tournament"
      (make-request (-> (mock/request :post "/api/tournament")
                        (mock/json-body {:sanctionid sanction-id
                                         :name       "Test tournament"
                                         :organizer  "Test organizer"
                                         :day        "2019-03-10"
                                         :rounds     3})
                        (add-apikey)))
      (is (= [{:sanctionid sanction-id
               :name       "Test tournament"
               :organizer  "Test organizer"
               :day        (time/local-date 2019 3 10)
               :rounds     3
               :owner      1}]
             (sql/select db/tournament
               (sql/fields :sanctionid :name :organizer :day :rounds :owner)))))
    (testing "doesn't allow tournament with invalid apikey"
      (make-request (-> (mock/request :post "/api/tournament")
                        (mock/json-body {:sanctionid "2-123456"
                                         :name       "Test tournament"
                                         :organizer  "Test organizer"
                                         :day        "2019-03-10"
                                         :rounds     3})
                        (add-apikey #uuid "68126748-01fd-4d2b-a35d-d0f062403058"))
                    400)))
  (testing "PUT /api/tournament/:sanctionid"
    (testing "updates tournament name"
      (make-request (-> (mock/request :put (str "/api/tournament/" sanction-id))
                        (mock/json-body {:name "Updated test tournament"})
                        (add-apikey))
                    204)
      (is (= [{:sanctionid sanction-id
               :name       "Updated test tournament"
               :organizer  "Test organizer"
               :day        (time/local-date 2019 3 10)
               :rounds     3
               :owner      1}]
             (sql/select db/tournament
               (sql/fields :sanctionid :name :organizer :day :rounds :owner))))))
  (testing "PUT /api/tournament/:sanctionid/teams"
    (testing "adds teams and players"
      (make-request (-> (mock/request :put (str "/api/tournament/" sanction-id "/teams"))
                        (mock/json-body {:teams [{:name    "Team 1"
                                                  :players [{:dci  "100000"
                                                             :name "Player 1"}]}
                                                 {:name    "Team 2"
                                                  :players [{:dci  "200000"
                                                             :name "Player 2"}]}
                                                 {:name    "Team 3"
                                                  :players [{:dci  "300000"
                                                             :name "Player 3"}]}
                                                 {:name    "Team 4"
                                                  :players [{:dci  "400000"
                                                             :name "Player 4"}]}]})
                        (add-apikey))
                    204)
      (is (= [{:dci "4050100000", :name "Player 1"}
              {:dci "1010200000", :name "Player 2"}
              {:dci "5060300000", :name "Player 3"}
              {:dci "7010400000", :name "Player 4"}]
             (sql/select db/player
               (sql/order :name))))
      (let [teams (sql/select db/team
                    (sql/with db/player)
                    (sql/order :team.name))]
        (is (= ["Team 1" "Team 2" "Team 3" "Team 4"]
               (map :name teams)))
        (is (= [{:dci "4050100000", :name "Player 1"}
                {:dci "1010200000", :name "Player 2"}
                {:dci "5060300000", :name "Player 3"}
                {:dci "7010400000", :name "Player 4"}]
               (mapcat :player teams))))))
  (testing "PUT /api/tournament/:sanctionid/pairings"
    (testing "adds pairings"
      (make-request (-> (mock/request :put (str "/api/tournament/" sanction-id "/round-1/pairings"))
                        (mock/json-body {:pairings [{:team1        ["4050100000"]
                                                     :team2        ["1010200000"]
                                                     :table_number 1}
                                                    {:team1        ["5060300000"]
                                                     :team2        ["7010400000"]
                                                     :table_number 2}]
                                         :playoff  false})
                        (add-apikey))
                    204)
      (is (= [{:team1        "Team 1"
               :team2        "Team 2"
               :table_number 1
               :team1_points 0
               :team2_points 0}
              {:team1        "Team 3"
               :team2        "Team 4"
               :table_number 2
               :team1_points 0
               :team2_points 0}]
             (sql/select db/pairing
               (sql/fields :table_number :team1_points :team2_points)
               (sql/with db/team1
                 (sql/fields [:name :team1]))
               (sql/with db/team2
                 (sql/fields [:name :team2]))
               (sql/order :table_number)))))
    (testing "can replace pairings"
      (make-request (-> (mock/request :put (str "/api/tournament/" sanction-id "/round-1/pairings"))
                        (mock/json-body {:pairings [{:team1        ["4050100000"]
                                                     :team2        ["5060300000"]
                                                     :table_number 1}
                                                    {:team1        ["1010200000"]
                                                     :team2        ["7010400000"]
                                                     :table_number 2}]
                                         :playoff  false})
                        (add-apikey))
                    204)
      (is (= [{:team1        "Team 1"
               :team2        "Team 3"
               :table_number 1
               :team1_points 0
               :team2_points 0}
              {:team1        "Team 2"
               :team2        "Team 4"
               :table_number 2
               :team1_points 0
               :team2_points 0}]
             (sql/select db/pairing
               (sql/fields :table_number :team1_points :team2_points)
               (sql/with db/team1
                 (sql/fields [:name :team1]))
               (sql/with db/team2
                 (sql/fields [:name :team2]))
               (sql/order :table_number))))))
  (testing "PUT /api/tournament/:sanctionid/results"
    (testing "adds partial results"
      (make-request (-> (mock/request :put (str "/api/tournament/" sanction-id "/round-1/results"))
                        (mock/json-body {:results [{:team1        ["4050100000"]
                                                    :team2        ["5060300000"]
                                                    :table_number 1
                                                    :team1_wins   2
                                                    :team2_wins   0
                                                    :draws        0}]})
                        (add-apikey))
                    204)
      (is (= [{:table_number 1
               :team1_wins   2
               :team2_wins   0
               :draws        0}
              {:table_number 2
               :team1_wins   nil
               :team2_wins   nil
               :draws        nil}]
             (sql/select db/pairing
               (sql/fields :table_number)
               (sql/with db/result
                 (sql/fields :team1_wins :team2_wins :draws))))))
    (testing "can replace results"
      (make-request (-> (mock/request :put (str "/api/tournament/" sanction-id "/round-1/results"))
                        (mock/json-body {:results [{:team1        ["4050100000"]
                                                    :team2        ["5060300000"]
                                                    :table_number 1
                                                    :team1_wins   2
                                                    :team2_wins   1
                                                    :draws        0}
                                                   {:team1        ["1010200000"]
                                                    :team2        ["7010400000"]
                                                    :table_number 2
                                                    :team1_wins   2
                                                    :team2_wins   0
                                                    :draws        0}]})
                        (add-apikey))
                    204)
      (is (= [{:table_number 1
               :team1_wins   2
               :team2_wins   1
               :draws        0}
              {:table_number 2
               :team1_wins   2
               :team2_wins   0
               :draws        0}]
             (sql/select db/pairing
               (sql/fields :table_number)
               (sql/with db/result
                 (sql/fields :team1_wins :team2_wins :draws))))))
    (testing "calculates standings"
      (is (= [{:rank      1
               :team_name "Team 2"
               :points    3
               :omw       33/100
               :pgw       1
               :ogw       33/100}
              {:rank      2
               :team_name "Team 1"
               :points    3
               :omw       33/100
               :pgw       2/3
               :ogw       1/3}
              {:rank      3
               :team_name "Team 3"
               :points    0
               :omw       1
               :pgw       1/3
               :ogw       2/3}
              {:rank      4
               :team_name "Team 4"
               :points    0
               :omw       1
               :pgw       33/100
               :ogw       1}]
             (->> (sql/select db/standings)
                  first
                  :standings
                  edn/read-string
                  (map #(select-keys % [:rank :team_name :points :omw :pgw :ogw])))))))
  (testing "another round"
    (testing "pairings"
      (make-request (-> (mock/request :put (str "/api/tournament/" sanction-id "/round-2/pairings"))
                        (mock/json-body {:pairings [{:team1        ["4050100000"]
                                                     :team2        ["1010200000"]
                                                     :table_number 1}
                                                    {:team1        ["5060300000"]
                                                     :team2        ["7010400000"]
                                                     :table_number 2}]
                                         :playoff  false})
                        (add-apikey))
                    204)
      (is (= [{:team1        "Team 1"
               :team2        "Team 2"
               :table_number 1
               :team1_points 3
               :team2_points 3}
              {:team1        "Team 3"
               :team2        "Team 4"
               :table_number 2
               :team1_points 0
               :team2_points 0}]
             (sql/select db/pairing
               (sql/fields :table_number :team1_points :team2_points)
               (sql/with db/round
                 (sql/fields)
                 (sql/where {:num 2}))
               (sql/with db/team1
                 (sql/fields [:name :team1]))
               (sql/with db/team2
                 (sql/fields [:name :team2]))
               (sql/order :table_number)))))
    (testing "results"
      (make-request (-> (mock/request :put (str "/api/tournament/" sanction-id "/round-2/results"))
                        (mock/json-body {:results [{:team1        ["4050100000"]
                                                    :team2        ["1010200000"]
                                                    :table_number 1
                                                    :team1_wins   2
                                                    :team2_wins   1
                                                    :draws        0}
                                                   {:team1        ["5060300000"]
                                                    :team2        ["7010400000"]
                                                    :table_number 2
                                                    :team1_wins   2
                                                    :team2_wins   1
                                                    :draws        0}]})
                        (add-apikey))
                    204))
    (testing "standings"
      (is (= [{:rank      1
               :team_name "Team 1"
               :points    6
               :omw       1/2
               :pgw       2/3
               :ogw       11/20}
              {:rank      2
               :team_name "Team 2"
               :points    3
               :omw       133/200
               :pgw       3/5
               :ogw       299/600}
              {:rank      3
               :team_name "Team 3"
               :points    3
               :omw       133/200
               :pgw       1/2
               :ogw       299/600}
              {:rank      4
               :team_name "Team 4"
               :points    0
               :omw       1/2
               :pgw       33/100
               :ogw       11/20}]
             (->> (sql/select db/standings
                    (sql/where {:round 2}))
                  first
                  :standings
                  edn/read-string
                  (map #(select-keys % [:rank :team_name :points :omw :pgw :ogw]))))))))
