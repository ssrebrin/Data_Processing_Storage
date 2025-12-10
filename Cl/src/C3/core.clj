(ns C3.core
  (:gen-class)
  (:require [clojure.core.async :as async]))


(defn batch [n coll]
  (lazy-seq
    (when-let [s (seq coll)]
      (cons (doall (take n s))
            (batch n (drop n s))))))


(defn par-filter
  [pred coll block-size parallelism]

  (let [blocks (batch block-size coll)
        f      #(doall (filter pred %))]

    (letfn [(step [[b & bs :as all-blocks] futs]
              (lazy-seq
                (when (seq futs)
                  (lazy-cat @(first futs)
                    (let [rest-futs (vec (rest futs))]
                      (step (rest all-blocks) (if (seq all-blocks)
                              (conj rest-futs
                                    (future (f (first all-blocks))))
                              rest-futs      
                                    )))))))]
      (step
        (drop parallelism blocks)
        (vec (map #(future (f %))
                  (take parallelism blocks)))))))



(defn tst [a] (do
                 (Thread/sleep 10)
                 (= (mod a 2) 0)))

(defn -main []
  (do
    (time (doall (take 1000 (par-filter tst (range) 200 100))))
    (time (doall (take 1000 (filter tst (range)))))
    )
  )