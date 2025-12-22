(ns C3.core-test
  (:require [C3.core :refer :all]
            [clojure.test :refer :all]))

(deftest test-par-filter
  (let [n 100000
        result-par (take n (par-filter odd? (range) 100 1000))
        result-seq (take n (filter odd? (range)))]
    (is (= result-par result-seq) "Parallel filter should produce same result as sequential filter")))
