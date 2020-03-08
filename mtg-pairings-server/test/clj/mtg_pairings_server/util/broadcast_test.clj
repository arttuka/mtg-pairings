(ns mtg-pairings-server.util.broadcast-test
  (:require [clojure.test :refer :all]
            [mtg-pairings-server.util.broadcast :refer :all]))

(use-fixtures :each (fn [f]
                      (f)
                      (reset! mapping {:dci->uid {}
                                       :uid->dci {}
                                       :id->uid  {}
                                       :uid->id  {}})))

(def dci-1 "7040000001")
(def dci-2 "5080000002")
(def dci-3 "5030000003")

(def uid-1 "uid-1")
(def uid-2 "uid-2")
(def uid-3 "uid-3")

(deftest login-test
  (testing "login"
    (testing "adds new user to mapping"
      (login uid-1 dci-1)
      (is (= {dci-1 #{uid-1}} (:dci->uid @mapping)))
      (is (= {uid-1 dci-1} (:uid->dci @mapping))))
    (testing "adding second user doesn't change first user"
      (login uid-2 dci-2)
      (is (= {dci-1 #{uid-1}
              dci-2 #{uid-2}}
             (:dci->uid @mapping)))
      (is (= {uid-1 dci-1
              uid-2 dci-2}
             (:uid->dci @mapping))))
    (testing "adding same dci with another uid"
      (login uid-3 dci-1)
      (is (= {dci-1 #{uid-1 uid-3}
              dci-2 #{uid-2}}
             (:dci->uid @mapping)))
      (is (= {uid-1 dci-1
              uid-2 dci-2
              uid-3 dci-1}
             (:uid->dci @mapping))))))

(deftest logout-test
  (testing "logout"
    (login uid-1 dci-1)
    (login uid-2 dci-2)
    (login uid-3 dci-1)
    (testing "removes only the correct uid"
      (logout uid-3)
      (is (= {dci-1 #{uid-1}
              dci-2 #{uid-2}}
             (:dci->uid @mapping)))
      (is (= {uid-1 dci-1
              uid-2 dci-2}
             (:uid->dci @mapping))))
    (testing "removes dci->uid mapping completely if last uid logs out"
      (logout uid-2)
      (is (= {dci-1 #{uid-1}} (:dci->uid @mapping)))
      (is (= {uid-1 dci-1} (:uid->dci @mapping))))
    (testing "leaves empty mapping if last user logs out"
      (logout uid-1)
      (is (= {} (:dci->uid @mapping)))
      (is (= {} (:uid->dci @mapping))))))

(deftest logout-dci-test
  (testing "logout-dci"
    (login uid-1 dci-1)
    (login uid-2 dci-2)
    (login uid-3 dci-1)
    (testing "logs out all uids of the dci"
      (logout-dci dci-1)
      (is (= {dci-2 #{uid-2}} (:dci->uid @mapping)))
      (is (= {uid-2 dci-2} (:uid->dci @mapping))))))
