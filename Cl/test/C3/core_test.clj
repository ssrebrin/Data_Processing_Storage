(ns C3.core-test
  (:require [C3.core :refer :all]
            [clojure.test :refer :all]))

(deftest test-par-filter []

  (time
    (let [result (take 10000 (par-filter odd? (range 1000000) 1000 100))]
      (println "Number of filtered elements:" (count result))))
    (time
    (let [result (take 10000 (filter odd? (range 1000000)))]
      (println "Number of filtered elements:" (count result)))
)
)

(defn expensive-pred [x]
  ;; имитация дорогой проверки
  (Thread/sleep 10)
  (even? x))

(deftest par-filter-performance-test []
  (println "=== Sequential filter ===")
  (time
    (let [result (take 50 (filter expensive-pred (range 100)))]
      (println "Result:" result)))

  (println "\n=== Parallel par-filter ===")
  (time
    (let [result (take 50 (par-filter expensive-pred (range 100) 5 10))]
      (println "Result:" result))))