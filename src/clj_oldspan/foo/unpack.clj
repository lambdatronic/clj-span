(comment

(defn generate-layer-from-ascii-grid
  [filename]
  (let [lines  (read-lines filename)
	rows (Integer/parseInt (second (re-find #"^NROWS\s+(\d+)" (first  lines))))
	cols (Integer/parseInt (second (re-find #"^NCOLS\s+(\d+)" (second lines))))
	data (drop-while #(re-find #"^[^\d].*" %) lines)]
    (println "Stub...process the data...")))

(defn- layer-from-observation
  [observation concept]
  (let [rows   (grid-rows observation)
	cols   (grid-columns observation)
	states (unpack-datasource (find-state observation concept) (* rows cols))]
    (vec (map vec (partition cols states)))))

)

(defn unpack-datasource
  "Returns a seq of length n of the values in ds,
   represented as probability distributions.  All values and
   probabilities are represented as rationals."
  [ds n]
  (println "DS:           " ds)
  (println "PROBABILISTIC?" (probabilistic? ds))
  (println "ENCODES?      " (encodes-continuous-distribution? ds))
  (let [to-rationals (partial map #(if (Double/isNaN %) 0 (rationalize %)))]
    (if (and (probabilistic? ds) (not (binary? ds)))
      (if (encodes-continuous-distribution? ds)
	;; sampled continuous distributions (FIXME: How is missing information represented?)
	(let [bounds                (get-dist-breakpoints ds)
	      unbounded-from-below? (== Double/NEGATIVE_INFINITY (first bounds))
	      unbounded-from-above? (== Double/POSITIVE_INFINITY (last bounds))]
	  (println "BREAKPOINTS:    " bounds)
	  (println "UNBOUNDED-BELOW?" unbounded-from-below?)
	  (println "UNBOUNDED-ABOVE?" unbounded-from-above?)
	  (let [prob-dist             (apply create-struct (to-rationals
							    (if unbounded-from-below?
							      (if unbounded-from-above?
								(rest (butlast bounds))
								(rest bounds))
							      (if unbounded-from-above?
								(butlast bounds)
								bounds))))
		get-cdf-vals          (if unbounded-from-below?
					(if unbounded-from-above?
					  #(successive-sums (to-rationals (butlast (get-probabilities ds %))))
					  #(successive-sums (to-rationals (get-probabilities ds %))))
					(if unbounded-from-above?
					  #(successive-sums 0 (to-rationals (butlast (get-probabilities ds %))))
					  #(successive-sums 0 (to-rationals (get-probabilities ds %)))))]
	    (for [idx (range n)]
	      (with-meta (apply struct prob-dist (get-cdf-vals idx)) cont-type))))
	;; discrete distributions (FIXME: How is missing information represented? Fns aren't setup for non-numeric values.)
	(let [prob-dist (apply create-struct (get-possible-states ds))]
	  (for [idx (range n)]
	    (with-meta (apply struct prob-dist (to-rationals (get-probabilities ds idx))) disc-type))))
      ;; binary distributions and deterministic values (FIXME: NaNs become 0s)
      (for [value (to-rationals (get-data ds))]
	(with-meta (array-map value 1) disc-type)))))

