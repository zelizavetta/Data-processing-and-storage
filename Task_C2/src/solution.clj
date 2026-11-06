(ns solution)

(defn- sieve [n composites]
  (lazy-seq
   (if-let [factors (get composites n)]
     (sieve (inc n)
            (reduce (fn [acc p] (update acc (+ n p) conj p))
                    (dissoc composites n)
                    factors))
     (cons n (sieve (inc n) (assoc composites (* n n) [n]))))))

(def primes (sieve 2 {}))

(println (take 20 primes))
