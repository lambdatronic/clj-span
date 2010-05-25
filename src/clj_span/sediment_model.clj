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
;;;
;;;-------------------------------------------------------------------
;;;
;;; This namespace defines the sediment model.
;;;
;;; Outstanding tasks:
;;; 7) Rebuild analyzer functions.
;;; 8) Attempt integration with ARIES.
;;; 9) Email Ferd with hangups.

(ns clj-span.sediment-model
  (:use [clj-span.params        :only (*trans-threshold*)]
	[clj-span.model-api     :only (distribute-flow service-carrier)]
	[clj-misc.utils         :only (seq2map seq2redundant-map euclidean-distance square-distance)]
	[clj-misc.matrix-ops    :only (get-rows get-cols make-matrix filter-matrix-for-coords in-bounds? find-line-between)]
	[clj-misc.randvars      :only (rv-zero
				       rv-zero-below-scalar
				       rv-min
				       rv-mean
				       rv-add
				       rv-subtract
				       rv-multiply
				       rv-divide
				       rv-scalar-add
				       rv-scalar-multiply
				       rv-scalar-divide)]))

(def #^{:private false} hydrosheds-delta-codes {{  1 1} [ 0  1]	 ; e
						{  2 1} [-1  1]	 ; se
						{  4 1} [-1  0]	 ; s
						{  8 1} [-1 -1]	 ; sw
						{ 16 1} [ 0 -1]	 ; w
						{ 32 1} [ 1 -1]	 ; nw
						{ 64 1} [ 1  0]	 ; n
						{128 1} [ 1  1]}) ; ne

;; try out (vecfoo (floor (/ (atan2 x y) 45))) or approximate by checking quadrant of vector-direction
(defn aggregate-flow-dirs
  [hydrocodes]
  (if-let [exit-code (some #{{-1 1} {0 1}} hydrocodes)]
    exit-code
    (let [vector-direction (reduce (partial map +) (map hydrosheds-delta-codes hydrocodes))]
      (if (= vector-direction [0 0])
	{-1 1} ; since we don't know where to go, we'll just terminate this path by saying we hit an inland sink
	(let [vector-magnitude  (euclidean-distance [0 0] vector-direction)
	      unit-vector       (map #(/ % vector-magnitude) vector-direction)
	      distances-to-dirs (seq2map hydrosheds-delta-codes (fn [[code v]] [(square-distance unit-vector v) code]))]
	  (distances-to-dirs (apply min (keys distances-to-dirs))))))))

(def *absorption-threshold* 0.1) ; this is just a hack - make it meaningful

(defn- step-downstream!
  [route-layer hydrosheds-layer sink-map use-map scaled-sinks
   rows cols [current-id source-ids incoming-utilities sink-effects]]
  (let [flow-delta (hydrosheds-delta-codes (get-in hydrosheds-layer current-id))]
    (when flow-delta ; if nil, we've hit 0 (ocean) or -1 (inland sink)
      (let [new-id (map + current-id flow-delta)]
	(when (in-bounds? rows cols new-id) ; otherwise, we've gone off the map
	  (let [affected-sinks     (filter #(> (rv-mean @(scaled-sinks %)) *absorption-threshold*) (sink-map new-id))
		affected-users     (use-map  new-id)
		total-incoming     (reduce rv-add incoming-utilities)
		sink-effects       (if (seq affected-sinks) ; sinks encountered
				     (merge sink-effects
					    (let [total-sink-cap (reduce rv-add (map (comp deref scaled-sinks) affected-sinks))
						  sink-fraction  (rv-divide total-incoming total-sink-cap)]
					      (seq2map affected-sinks
						       #(let [sink-cap @(scaled-sinks %)]
							  [% (rv-scalar-multiply
							      (rv-min sink-cap (rv-multiply sink-cap sink-fraction))
							      -1)]))))
				     sink-effects)
		outgoing-utilities (if (seq affected-sinks) ; sinks encountered
				     (do
				       (doseq [sink-id affected-sinks]
					 (swap! (scaled-sinks sink-id) rv-add (sink-effects sink-id)))
				       (let [total-sunk   (reduce rv-add (map sink-effects affected-sinks))
					     out-fraction (rv-scalar-add (rv-divide total-sunk total-incoming) 1)]
					 (map #(rv-multiply % out-fraction) incoming-utilities)))
				     incoming-utilities)]
	    (when (seq affected-users)	; users encountered
	      (let [carrier-caches  (map #(get-in route-layer %) affected-users)
		    source-carriers (map #(struct-map service-carrier :weight %1 :route %2)
					 incoming-utilities ; might need to be the original source-value
					 source-ids)
		    sink-carriers   (map (fn [[id w]] (struct-map service-carrier :weight w :route id))
					 sink-effects)]
		(doseq [cache carrier-caches]
		  (swap! cache concat source-carriers sink-carriers))))
	    (when (> (reduce + (map rv-mean outgoing-utilities)) *trans-threshold*)
	      [new-id source-ids outgoing-utilities sink-effects])))))))

(defn- move-points-into-stream-channel
  "Returns a map of in-stream-ids to lists of the out-of-stream-ids
   that were shifted into this position."
  [hydrosheds-layer stream-layer data-points]
  (seq2redundant-map data-points
		     #(loop [id %]
			(if (not= rv-zero (get-in stream-layer id))
			  ;; in-stream
			  [(vec id) %]
			  ;; not in-stream
			  (let [flow-delta (hydrosheds-delta-codes (get-in hydrosheds-layer id))]
			    (recur (map + id flow-delta)))))
		     conj))

(defn- scale-by-stream-proximity
  [floodplain-layer elevation-layer in-stream-map data-layer]
  (into {}
	(remove nil?
		(for [in-stream-id (keys in-stream-map) data-id (in-stream-map in-stream-id)]
		  (if (= in-stream-id data-id)
		    ;; location is already in-stream, activation is 100%
		    [data-id (atom (get-in data-layer data-id))]
		    ;; location is out-of-stream, activation is scaled by the
		    ;; relative elevation difference between this location,
		    ;; the in-stream proxy location, and the nearest
		    ;; floodplain boundary
		    (let [loc-delta       (map - data-id in-stream-id)
			  outside-id      (first (drop-while #(not= rv-zero (get-in floodplain-layer %))
							     (rest (iterate #(map + loc-delta %) data-id))))
			  inside-id       (map - outside-id loc-delta)
			  boundary-id     (first (drop-while #(not= rv-zero (get-in floodplain-layer %))
							     (find-line-between inside-id outside-id)))
			  rise            (rv-subtract (get-in elevation-layer boundary-id)
						       (get-in elevation-layer in-stream-id))
			  run-to-boundary (Math/sqrt (#(+ (* %1 %1) (* %2 %2)) (map - boundary-id in-stream-id)))
			  run-to-data     (Math/sqrt (#(+ (* %1 %1) (* %2 %2)) (map - data-id in-stream-id)))
			  slope           (rv-scalar-divide rise run-to-boundary)
			  elev-limit      (rv-add (rv-scalar-multiply slope run-to-data) (get-in elevation-layer in-stream-id))]
		      (if (< (rv-mean (get-in elevation-layer data-id)) (rv-mean elev-limit))
			(let [activation-factor (- 1 (/ run-to-data run-to-boundary))]
			  [data-id (atom (rv-scalar-multiply (get-in data-layer data-id) activation-factor))]))))))))

(defmethod distribute-flow "Sediment"
  [_ source-layer sink-layer use-layer
   {hydrosheds-layer "Hydrosheds", stream-layer "RiverStream",
    floodplain-layer "FloodPlainPresence", elevation-layer "Altitude"}]
  (let [rows           (get-rows source-layer)
	cols           (get-cols source-layer)
	route-layer    (make-matrix rows cols (constantly (atom ())))
	[source-map sink-map use-map] (map (comp
					    (partial move-points-into-stream-channel hydrosheds-layer stream-layer)
					    (partial filter-matrix-for-coords #(not= rv-zero %)))
					   [source-layer sink-layer use-layer])
	[scaled-sources scaled-sinks] (map (partial scale-by-stream-proximity floodplain-layer elevation-layer)
					   [source-map   sink-map]
					   [source-layer sink-layer])]
    (println "Source points:" (count (concat (vals source-map))))
    (println "Sink points:  " (count (concat (vals sink-map))))
    (println "Use points:   " (count (concat (vals use-map))))
    (loop [sediment-carriers (for [in-stream-id (keys source-map)]
			       (let [source-ids (source-map in-stream-id)]
				 [in-stream-id source-ids (map (comp deref scaled-sources) source-ids) {}]))]
      (if (empty? sediment-carriers)
	route-layer
	(recur (remove nil?
		       (pmap (partial step-downstream!
				      route-layer
				      hydrosheds-layer
				      sink-map
				      use-map
				      scaled-sinks
				      rows cols)
			     sediment-carriers)))))))
