(ns C1.core
  (:gen-class))

(defn permut [alphabet N]
  (let [alphabet (map str alphabet)] 
    (reduce
      (fn [a _]
        (mapcat
          (fn [s]
            (map (fn [b] (str s b))
                 (remove (set (map str s)) alphabet))) 
          a))
      alphabet
      (range (dec N)))))

(defn -main [& args]
  (if (< (count args) 2)
    (println "Usage: clj -M -m C1.core \"a b c\" N")
    (let [
          alphabet    (clojure.string/split (first args) #"\s+")
          N       (Integer/parseInt (second args))
          result      (permut alphabet N)]
        
    (if (< N 0) (println "N cannot be less than 0")
    (if (<= (count alphabet) N) (println (list (apply str alphabet))) (println result))))))

