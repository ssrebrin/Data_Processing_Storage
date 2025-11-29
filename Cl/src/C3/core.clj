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
    (letfn [(process [blks active-futs]
              (lazy-seq
                (when-let [b (seq blks)]
                  (let [
                        futs   (concat active-futs (doall (map #(future (filter pred %)) (take parallelism b))))]
                    (concat @(first futs)
                            (process (drop parallelism b) (rest futs)))))))]
      (process blocks '()))))


(defn -main [& args]
    (println (take 50 (par-filter even? (range) 5 10))))