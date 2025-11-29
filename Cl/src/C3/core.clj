(ns C3.core
  (:gen-class)
  (:require [clojure.core.async :as async]))


(defn batch [n coll]
  (lazy-seq
    (when-let [s (seq coll)]
      (cons (take n s) (batch n (drop n s))))))

(defn par-filter
  [pred coll block-size parallelism]
  (let [blocks (batch block-size coll)]
    (letfn [(process [blks futs]
              (lazy-seq
                (when-let [b (seq blks)]
                  (let [futs (if (empty? futs) (vec (doall (map #(future (filter pred %)) (take parallelism b)))) futs)
                        rest-futs (subvec futs 1)
                        new-block (first (drop parallelism b))
                        new-fut   (when new-block (future (filter pred new-block)))]
                    (concat @(first futs)
                            (process (rest blks) (if new-fut (conj rest-futs new-fut) rest-futs)))))))]
      (process blocks []))))

(defn is-prime [n]
  (cond
    (< n 2) false
    (= n 2) true
    (even? n) false
    :else (let [sqrt-n (Math/sqrt n)]
            (not-any? #(zero? (mod n %))
                      (range 3 (inc (int sqrt-n)) 2)))))

(defn -main [& args]

  (time
    (let [result (take 10000 (par-filter is-prime (range) 100 1000000))]
      (println "Number of filtered elements:" (count result))))
    (time
    (let [result (take 10000 (filter is-prime (range)))]
      (println "Number of filtered elements:" (count result)))
)
)