(ns solution-test
  (:require [clojure.test :refer [deftest is testing run-tests]]
            [solution :refer [primes]]))

(defn- prime? [n]
  (and (> n 1)
       (not-any? #(zero? (mod n %)) (range 2 (inc (int (Math/sqrt n)))))))

(deftest first-primes
  (is (= [2 3 5 7 11 13 17 19 23 29] (take 10 primes))))

(deftest nth-prime
  (testing "известные значения"
    (is (= 2 (first primes)))
    (is (= 541 (nth primes 99)))
    (is (= 7919 (nth primes 999)))
    (is (= 104729 (nth primes 9999)))))

(deftest only-primes
  (is (every? prime? (take 2000 primes))))

(deftest no-primes-skipped
  (is (= (filter prime? (range 2 5000))
         (take-while #(< % 5000) primes))))

(deftest increasing
  (is (apply < (take 1000 primes))))

(deftest lazy-and-infinite
  (is (instance? clojure.lang.LazySeq primes))
  (is (= 100000 (count (take 100000 primes)))))

(run-tests)
