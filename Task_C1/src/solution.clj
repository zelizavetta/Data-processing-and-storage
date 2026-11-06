   (ns solution)

(defn generate-sub-alphabets [alphabet n]
  (reduce
   (fn [words _]
     (reduce
      (fn [acc word]
        (doall
         (concat acc
                 (map #(str word %)
                      (filter #(not= % (str (last word))) alphabet)))))
      '()
      words))
   '("")
   (range n)))

(println (generate-sub-alphabets '("a" "b" "c") 2))
(println (generate-sub-alphabets '("a" "b" "c") 3))