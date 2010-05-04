;;; Copyright 2010 Gary Johnson
;;;
;;; This file is part of clj-span.
;;;
;;; clj-span is free software: you can redistribute it and/or modify
;;; it under the terms of the GNU General Public License as published
;;; by the Free Software Foundation, either version 3 of the License,
;;; or (at your option) any later version.
;;;
;;; clj-span is distributed in the hope that it will be useful, but
;;; WITHOUT ANY WARRANTY; without even the implied warranty of
;;; MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
;;; General Public License for more details.
;;;
;;; You should have received a copy of the GNU General Public License
;;; along with clj-span.  If not, see <http://www.gnu.org/licenses/>.

(ns clj-span.line-of-sight-model
  (:use [clj-misc.randvars   :only (rv-zero-above-scalar
				    rv-add
				    rv-subtract
				    rv-scale
				    rv-scalar-divide
				    rv-scalar-multiply
				    rv-lt
				    rv-mean)]
	[clj-misc.matrix-ops :only (make-matrix
				    get-rows
				    get-cols
				    filter-matrix-for-coords)]
	[clj-span.model-api  :only (distribute-flow!
				    distribute-flow
				    decay
				    undecay
				    service-carrier)]
	[clj-span.analyzer   :only (source-loc? sink-loc? use-loc?)]
	[clj-span.params     :only (*trans-threshold*)]))

(def #^{:private true} elev-concept "Altitude")

(defn- find-viewpath
  "Returns the sequence of all points [i j] intersected by the line
   from provider to beneficiary.  Since this is calculated over a
   regular integer-indexed grid, diagonal lines will be approximated
   by lines bending at right angles along the p-to-b line.  This
   calculation imagines the indeces of each point to be located at the
   center of a square of side length 1.  Note that the first point in
   each path will be the provider id, and the last will be the
   beneficiary id.  If provider=beneficiary, the path will contain
   only this one point."
  [[pi pj] [bi bj]]
  (let [m (if (not= pj bj) (/ (- bi pi) (- bj pj)))
	b (if m (- pi (* m pj)))
	f (fn [x] (+ (* m x) b))]
    (cond (nil? m) (map (fn [i] [i pj])
			(if (< pi bi)
			  (range pi (inc bi))
			  (range pi (dec bi) -1)))

	  (== m 0) (map (fn [j] [pi j])
			(if (< pj bj)
			  (range pj (inc bj))
			  (range pj (dec bj) -1)))

	  :otherwise (let [get-i-range
			   (cond (and (< pi bi) (< pj bj))
				 (fn [j] (let [left-i  (int (Math/round (f (- j (if (== j pj) 0.0 0.5)))))
					       right-i (int (Math/round (f (+ j (if (== j bj) 0.0 0.5)))))]
					   (range left-i (inc right-i))))
					       
				 (and (< pi bi) (> pj bj))
				 (fn [j] (let [left-i  (int (Math/round (f (- j (if (== j bj) 0.0 0.5)))))
					       right-i (int (Math/round (f (+ j (if (== j pj) 0.0 0.5)))))]
					   (range right-i (inc left-i))))
					       
				 (and (> pi bi) (< pj bj))
				 (fn [j] (let [left-i  (int (Math/round (f (- j (if (== j pj) 0.0 0.5)))))
					       right-i (int (Math/round (f (+ j (if (== j bj) 0.0 0.5)))))]
					   (range left-i  (dec right-i) -1)))
					       
				 (and (> pi bi) (> pj bj))
				 (fn [j] (let [left-i  (int (Math/round (f (- j (if (== j bj) 0.0 0.5)))))
					       right-i (int (Math/round (f (+ j (if (== j pj) 0.0 0.5)))))]
					   (range right-i (dec left-i)  -1))))
			   j-range (if (< pj bj)
				     (range pj (inc bj))
				     (range pj (dec bj) -1))]
		       (for [j j-range i (get-i-range j)] [i j])))))

;; FIXME what is this NODATA value evilness doing in my code?!
(defn- get-valid-elevation
  [location]
  (rv-zero-above-scalar (get-in location [:flow-features elev-concept]) 55535.0))
(def get-valid-elevation (memoize get-valid-elevation))

;; FIXME convert step to distance metric based on map resolution and make this gaussian to 1/2 mile
(defmethod decay "LineOfSight"
  [_ weight step] (rv-scalar-divide weight (* step step)))

;; FIXME convert step to distance metric based on map resolution and make this gaussian to 1/2 mile
(defmethod undecay "LineOfSight"
  [_ weight step] (rv-scalar-multiply weight (* step step)))

(defn- distribute-raycast!
  [flow-model location-map [provider beneficiary]]
  (if (= provider beneficiary)
    (swap! (:carrier-cache provider) conj
	   (struct service-carrier (:source provider) [provider]))
    (let [path        (map location-map (find-viewpath (:id provider) (:id beneficiary)))
	  steps       (dec (count path))
	  source-val  (:source provider)]
      (when (or (== steps 1)
		(> (rv-mean (decay flow-model source-val (dec steps))) *trans-threshold*))
	(let [source-elev (get-valid-elevation provider)
	      rise        (rv-subtract (get-valid-elevation beneficiary) source-elev)
	      m           (rv-scalar-divide rise steps)]
	  (loop [step        1
		 current     (second path)
		 explored    (vec (take 2 path))
		 frontier    (drop 2 path)
		 elev-bounds (rest (iterate #(rv-add m %) source-elev))
		 weight      source-val
		 delayed-ops (if (sink-loc? provider)
			       [#(swap! (:carrier-cache provider) conj
					(struct service-carrier weight [provider]))]
			       [])]
	    (let [decayed-weight (decay flow-model weight step)]
	      (if (== step steps)
		(do
		  (doseq [op delayed-ops] (op))
		  (swap! (:carrier-cache current) conj
			 (struct service-carrier decayed-weight explored)))
		(when (> (rv-mean decayed-weight) *trans-threshold*)
		  (recur (inc step)
			 (first frontier)
			 (conj explored (first frontier))
			 (rest frontier)
			 (rest elev-bounds)
			 (rv-scale weight (rv-lt (get-valid-elevation current) (first elev-bounds)))
			 (if (sink-loc? current)
			   (conj delayed-ops
				 #(swap! (:carrier-cache current) conj
					 (struct service-carrier decayed-weight explored)))
			   delayed-ops)))))))))))

(defn- distribute-raycast-debug!
  [flow-model location-map [provider beneficiary]]
  (println "Distribute-raycast called!\nWaiting...")
  (Thread/sleep 1000)
  (println "Resuming")
  (if (= provider beneficiary)
    (do
      (println "In Situ Usage")
      (swap! (:carrier-cache provider) conj
	     (struct service-carrier (:source provider) [provider])))
    (let [viewpath    (find-viewpath (:id provider) (:id beneficiary))
	  path        (map location-map viewpath)
	  steps       (dec (count path))
	  source-val  (:source provider)]
      (newline)
      (println "ViewPath:        " viewpath)
      (println "First Path ID:   " (first viewpath))
      (println "First ID Type:   " (map type (first viewpath)))
      (println "Path Length:     " steps)
      (println "Source-val:      " source-val)
      (println "Trans-threshold: " *trans-threshold*)
      (println "Steps-1 Squared: " (Math/pow (dec steps) 2))
      (if (or (== steps 1)
	      (> (rv-mean (decay flow-model source-val (dec steps))) *trans-threshold*))
	(do
	  (let [source-elev (get-valid-elevation provider)
		use-elev    (get-valid-elevation beneficiary)]
	    (println "Source-elev:" source-elev)
	    (println "Use-elev:"    use-elev)
	    (let [rise (rv-subtract use-elev source-elev)]
	      (println "Rise:" rise)
	      (let [m (rv-scalar-divide rise steps)]
		(println "M:" m)
		(loop [step        1
		       current     (second path)
		       explored    (vec (take 2 path))
		       frontier    (drop 2 path)
		       elev-bounds (rest (iterate #(rv-add m %) source-elev))
		       weight      source-val
		       delayed-ops (if (sink-loc? provider)
				     [#(swap! (:carrier-cache provider) conj
					      (struct service-carrier weight [provider]))]
				     [])]
		  (println step ":" current (count explored) (count frontier) (first elev-bounds) weight (count delayed-ops))
		  (Thread/sleep 500)
		  (let [decayed-weight (decay flow-model weight step)]
		    (println "Decayed-weight:" decayed-weight)
		    (if (== step steps)
		      (do
			(println "Final Step!")
			(doseq [op delayed-ops] (op))
			(swap! (:carrier-cache current) conj
			       (struct service-carrier decayed-weight explored)))
		      (when (> (rv-mean decayed-weight) *trans-threshold*)
			(println "Recurring...")
			(println "Weight:      " weight)
			(println "Elev-bound:  " (first elev-bounds))
			(println "Elev-current:" (get-valid-elevation current))
			(println "RV-LT:       " (rv-lt (get-valid-elevation current) (first elev-bounds)))
			(println "RV-Scale:    " (rv-scale weight (rv-lt (get-valid-elevation current) (first elev-bounds))))
			(recur (inc step)
			       (first frontier)
			       (conj explored (first frontier))
			       (rest frontier)
			       (rest elev-bounds)
			       (rv-scale weight (rv-lt (get-valid-elevation current) (first elev-bounds)))
			       (if (sink-loc? current)
				 (conj delayed-ops
				       #(swap! (:carrier-cache current) conj
					       (struct service-carrier decayed-weight explored)))
				 delayed-ops))))))))))
	(println "Distribute raycast completed!")))))

(defmethod distribute-flow! "LineOfSight"
  [flow-model location-map _ _]
  (let [locations (vals location-map)]
    (dorun
     (pmap
      (partial distribute-raycast! flow-model location-map)
      (for [p (filter source-loc? locations) b (filter use-loc? locations)] [p b])))))

(defmethod distribute-flow! "LineOfSightDebug"
  [flow-model location-map _ _]
  (let [locations (vals location-map)
	sources   (filter source-loc? locations)
	sinks     (filter sink-loc?   locations)
	uses      (filter use-loc?    locations)
	pbs       (for [p sources b uses] [p b])]
    (println "Num Sources:" (count sources))
    (println "Num Sinks:"   (count sinks))
    (println "Num Uses:"    (count uses))
    (println "Num Pairs:"   (count pbs))
    (dorun (map (partial distribute-raycast-debug! flow-model location-map) pbs))))

(defn- raycast!
  [route-layer source-layer sink-layer use-layer elev-layer source-point use-point]
  (if (= source-point use-point)
    (swap! (route-layer use-point) conj
	   (struct service-carrier (source-layer source-point) (list source-point)))
    ...))

(defmethod distribute-flow "LineOfSight"
  [flow-model source-layer sink-layer use-layer {elev-layer "Altitude"}]
  (let [route-layer   (make-matrix (get-rows source-layer) (get-cols source-layer) #(atom ()))
	source-points (filter-matrix-for-coords source-loc? source-layer)
	use-points    (filter-matrix-for-coords use-loc?    use-layer)]
    (dorun (pmap (partial raycast! route-layer source-layer sink-layer use-layer elev-layer) source-points use-points))
    route-layer))
