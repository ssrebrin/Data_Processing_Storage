(ns C2.core
  (:gen-class))

(defn sieve [s]
  (lazy-seq
  (cons (first s)
    (sieve (remove #(= 0 (mod % (first s))) (rest s)))
  )
  )
)

(defn erot [N]
    (take N (sieve (range 2 Integer/MAX_VALUE)))
)

(defn -main [& args]
  (if (< (count args) 1)
    (println "Usage: clj -M -m C2.core N")
    (let [
          N           (Integer/parseInt (first args))
          result      (erot N)]
        (println result))))