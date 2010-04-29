(refer 'corescience :only '(find-state
			    find-observation
			    get-state-map))
(refer 'tl          :only '(get-session))
(refer 'corescience :only '(find-state
			    find-observation
			    get-state-map
			    get-observable-class))
(refer 'geospace    :only '(build-coverage
			    get-spatial-extent
			    grid-extent?
			    grid-rows
			    grid-columns))
(refer 'modelling   :only '(probabilistic?
			    binary?
			    encodes-continuous-distribution?
			    get-dist-breakpoints
			    get-possible-states
			    get-probabilities
			    get-data))

(defn- observation-spaces-match?
  "Verifies that all observations have a grid extent and the same rows
   and cols."
  [& observations]
  (and (every? grid-extent? observations)
       (let [rows (map grid-rows observations)
	     cols (map grid-columns observations)]
         (not (or (some #(not= % (first rows)) (rest rows))
		  (some #(not= % (first cols)) (rest cols)))))))

(defmulti provide-results (fn [result-type flow-concept-name locations rows cols] result-type))

(defmethod provide-results :closure-map
  [_ flow-concept-name locations _ _]
  {:theoretical-source  #(theoretical-source  locations)
   :theoretical-sink    #(theoretical-sink    locations)
   :theoretical-use     #(theoretical-use     locations)
   :inaccessible-source #(inaccessible-source locations)
   :inaccessible-sink   #(inaccessible-sink   locations)
   :inaccessible-use    #(inaccessible-use    locations)
   :possible-flow       #(possible-flow       locations flow-concept-name)
   :possible-source     #(possible-source     locations)
   :possible-sink       #(possible-sink       locations)
   :possible-use        #(possible-use        locations)
   :blocked-flow        #(blocked-flow        locations flow-concept-name)
   :blocked-source      #(blocked-source      locations flow-concept-name)
   :blocked-sink        #(blocked-sink        locations flow-concept-name)
   :blocked-use         #(blocked-use         locations flow-concept-name)
   :actual-flow         #(actual-flow         locations flow-concept-name)
   :actual-source       #(actual-source       locations flow-concept-name)
   :actual-sink         #(actual-sink         locations flow-concept-name)
   :actual-use          #(actual-use          locations flow-concept-name)})

(defmethod provide-results :matrix-list
  [_ flow-concept-name locations rows cols]
  (map (partial coord-map-to-matrix rows cols)
       (list
	(theoretical-source  locations)
	(theoretical-sink    locations)
	(theoretical-use     locations)
	(inaccessible-source locations)
	(inaccessible-sink   locations)
	(inaccessible-use    locations)
	(possible-flow       locations flow-concept-name)
	(possible-source     locations)
	(possible-sink       locations)
	(possible-use        locations)
	(blocked-flow        locations flow-concept-name)
	(blocked-source      locations flow-concept-name)
	(blocked-sink        locations flow-concept-name)
	(blocked-use         locations flow-concept-name)
	(actual-flow         locations flow-concept-name)
	(actual-source       locations flow-concept-name)
	(actual-sink         locations flow-concept-name)
	(actual-use          locations flow-concept-name))))

(defmethod provide-results :raw-locations
  [_ _ locations _ _]
  locations)

(defn span-driver
  "Takes the source, sink, use, and flow concepts along with the
   flow-params map and an observation containing the concepts'
   dependent features, calculates the SPAN flows, and returns the
   results using one of the following result-types:
   :closure-map :matrix-list :raw-locations"
  ([observation source-concept use-concept sink-concept flow-concept flow-params]
     (span-driver observation source-concept use-concept sink-concept flow-concept flow-params :closure-map))
  ([observation source-concept use-concept sink-concept flow-concept flow-params result-type]
     ;; construct the data layers
     (let [source-layer (layer-from-observation observation source-concept)
	   sink-layer   (layer-from-observation observation sink-concept)
	   use-layer    (layer-from-observation observation use-concept)
	   flow-layer   (layer-from-observation observation flow-concept)]
       (run-span ...)
       ;; make the flow-params into global variables
       (set-global-params! flow-params)
     ;;(store-params flow-params)
     (let [rows              (grid-rows    observation)
	   cols              (grid-columns observation)
	   flow-concept-name (.getLocalName (get-observable-class observation))
	   locations         (simulate-service-flows observation
						     source-concept
						     sink-concept
						     use-concept
						     flow-concept
						     flow-concept-name
						     rows cols)]
       (provide-results result-type flow-concept-name locations rows cols))))

(defn span-driver
  "Takes the source, sink, use, and flow concepts along with the
   flow-params map and an observation containing the concepts'
   dependent features, calculates the SPAN flows, and returns the
   results using one of the following result-types:
   :closure-map :matrix-list :raw-locations"
  ([observation source-concept use-concept sink-concept flow-concept flow-params]
     (span-driver observation source-concept use-concept sink-concept flow-concept flow-params :closure-map))
  ([observation source-concept use-concept sink-concept flow-concept flow-params result-type]
     (set-global-params! flow-params)
     ;;(store-params flow-params)
     (let [rows              (grid-rows    observation)
	   cols              (grid-columns observation)
	   flow-concept-name (.getLocalName (get-observable-class observation))
	   locations         (simulate-service-flows observation
						     source-concept
						     sink-concept
						     use-concept
						     flow-concept
						     flow-concept-name
						     rows cols)]
       (provide-results result-type flow-concept-name locations rows cols))))



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

(defn- extract-values-by-concept-clean
  "Returns a seq of the concept's values in the observation,
   which are doubles or probability distributions."
  [obs conc n]
  (unpack-datasource (find-state obs conc) n))

(defn- extract-values-by-concept
  "Returns a seq of the concept's values in the observation,
   which are doubles or probability distributions."
  [obs conc n]
  (println "EXTRACT-VALUES-BY-CONCEPT...")
  (println "OBS:  " obs)
  (println "CONC: " conc)
  (println "STATE:" (find-state obs conc))
  (unpack-datasource (find-state obs conc) n))

(defn- extract-all-values-clean
  "Returns a map of concept-names to vectors of doubles or probability
   distributions."
  [obs conc n]
  (when conc
    (maphash (memfn getLocalName) #(vec (unpack-datasource % n)) (get-state-map (find-observation obs conc)))))

(defn- extract-all-values
  "Returns a map of concept-names to vectors of doubles or probability
   distributions."
  [obs conc n]
  (let [subobs (find-observation obs conc)
	states (get-state-map subobs)]
    (println "EXTRACT-ALL-VALUES...")
    (println "OBS:   " obs)
    (println "CONC:  " conc)
    (println "SUBOBS:" subobs)
    (println "STATES:" states)
    (when conc
      (maphash (memfn getLocalName) #(vec (unpack-datasource % n)) (get-state-map (find-observation obs conc))))))

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

