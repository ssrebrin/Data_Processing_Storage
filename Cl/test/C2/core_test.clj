(ns C2.core-test
  (:require [clojure.test :refer :all]
            [C2.core :refer :all]))

(defn prime? [n]
  (and (> n 1)
       (not-any? #(zero? (mod n %))
                 (range 2 (inc (int (Math/sqrt n)))))))


(deftest test-erot1
  (is (= (erot 5) '(2 3 5 7 11)))
  (is (= (erot 10) '(2 3 5 7 11 13 17 19 23 29))))

(deftest test-erot2
  (is (= 20 (count (erot 20)))))

(deftest test3
  (is (every? prime? (erot 30))))
