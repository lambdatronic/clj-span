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
;;; This namespace defines functions for analyzing the location
;;; sequence returned by clj-span.core/simulate-service-flows.  Each
;;; public function may be applied independently of the others and
;;; will generate a map of {[i j] -> value} pairs.

(ns clj-span.analyzer
  (:use [clj-span.model-api  :only (undecay)]
	[clj-span.params     :only (*source-type*
				    *sink-type*
				    *use-type*)]
	[clj-misc.randvars   :only (rv-zero
				    rv-zero-below-scalar
				    rv-add
				    rv-subtract
				    rv-min
				    rv-scalar-divide
				    scalar-rv-multiply)]
	[clj-misc.matrix-ops :only (get-rows
				    get-cols
				    matrix2seq
				    map-matrix
				    make-matrix
				    unbitpack-route)]))

;;(defstruct service-carrer
;;  :source-id      ; starting id of this flow path
;;  :route          ; byte array of directions from source-id to use-id or nil
;;  :possible-weight; amount of source-weight which reaches (and is used by) this use location disregarding sink-effects
;;  :actual-weight  ; amount of source-weight which reaches (and is used by) this use location including sink-effects
;;  :sink-effects   ; map of sink-ids to sink-effects on this flow path (decayed as necessary)
;;  :use-effects)   ; map of rival-use-ids to rival-use-effects on this flow path (decayed as necessary)

(defn theoretical-source
  "If *source-type* is finite, return source-layer. Else return
   source-layer * num-users."
  [source-layer use-layer]
  (if (= *source-type* :finite)
    source-layer
    (let [num-users (count (remove (partial = rv-zero) (matrix2seq use-layer)))]
      (map-matrix (partial scalar-rv-multiply num-users) source-layer))))
(def theoretical-source (memoize theoretical-source))

(defn possible-source
  "Returns a matrix of RVs, in which each cell contains the amount of
   its theoretical source that impacts a user along any flow path,
   disregarding the negative effects of sinks and rival users."
  [cache-layer]
  (let [coord-map (apply merge-with rv-add {}
			 (for [cache (remove nil? (matrix2seq cache-layer))
			       {:keys [source-id possible-weight]} cache]
			   {source-id possible-weight}))]
    (make-matrix (get-rows cache-layer) (get-cols cache-layer) #(get coord-map % rv-zero))))
(def possible-source (memoize possible-source))

(defn actual-source
  "Returns a matrix of RVs, in which each cell contains the amount of
   its theoretical source that impacts a user along any flow path,
   including the negative effects of sinks and rival users."
  [cache-layer]
  (let [coord-map (apply merge-with rv-add {}
			 (for [cache (remove nil? (matrix2seq cache-layer))
			       {:keys [source-id actual-weight]} cache]
			   {source-id actual-weight}))]
    (make-matrix (get-rows cache-layer) (get-cols cache-layer) #(get coord-map % rv-zero))))
(def actual-source (memoize actual-source))

(defn theoretical-sink
  "If *sink-type* is finite, return sink-layer. Else return sink-layer
   * max-flowpaths (limited by total-source if *source-type* is
   finite)."
  [source-layer sink-layer use-layer]
  (if (= *sink-type* :finite)
    sink-layer
    (let [num-sources   (count (remove (partial = rv-zero) (matrix2seq source-layer)))
	  num-users     (count (remove (partial = rv-zero) (matrix2seq use-layer)))
	  max-flowpaths (* num-sources num-users)
	  sink-amount   (if (*source-type* :finite)
			  (let [total-source (reduce rv-add rv-zero (remove (partial = rv-zero) (matrix2seq source-layer)))]
			    #(rv-min (scalar-rv-multiply max-flowpaths %) total-source))
			  (partial scalar-rv-multiply max-flowpaths))]
      (map-matrix #(if (not= rv-zero %) (sink-amount %) rv-zero) sink-layer))))

(defn actual-sink
  "Returns a matrix of RVs, in which each cell contains the fraction
   of its theoretical sink that impacts a user along any flow path."
  [cache-layer]
  (let [coord-map (apply merge-with rv-add {}
			 (for [cache (remove nil? (matrix2seq cache-layer))]
			   (:sink-effects cache)))]
    (make-matrix (get-rows cache-layer) (get-cols cache-layer) #(get coord-map % rv-zero))))

(defn theoretical-use
  "If *use-type* is finite, return use-layer. Else return a new layer
   in which all non-zero use values have been replaced with
   total-source (or with total-source divided by num-users if
   *source-type* is finite)."
  [source-layer use-layer]
  (if (= *use-type* :finite)
    use-layer
    (let [num-users  (count (remove (partial = rv-zero) (matrix2seq use-layer)))
	  use-amount (if (= *source-type* :finite)
		       (let [total-source (reduce rv-add rv-zero (remove (partial = rv-zero) (matrix2seq source-layer)))]
			 (rv-scalar-divide total-source num-users)
			 total-source))]
      (map-matrix #(if (not= rv-zero %) use-amount rv-zero) use-layer))))
(def theoretical-use (memoize theoretical-use))

(defn possible-use
  "Returns a matrix of RVs, in which each cell contains the amount of
   its theoretical source that impacts a user along any flow path,
   disregarding the negative effects of sinks and rival users."
  [cache-layer]
  (map-matrix #(reduce rv-add rv-zero (map :possible-weight %)) cache-layer))
(def possible-use (memoize possible-use))

(defn actual-use
  "Returns a matrix of RVs, in which each cell contains the amount of
   its theoretical source that impacts a user along any flow path,
   disregarding the negative effects of sinks and rival users."
  [cache-layer]
  (map-matrix #(reduce rv-add rv-zero (map :actual-weight %)) cache-layer))
(def actual-use (memoize actual-use))

(defn- rerun-possible-route
  [flow-model {:keys [source-id route possible-weight use-effects]}]
  (let [route-ids (rseq (unbitpack-route source-id route))]
    (zipmap route-ids
	    (map #(undecay flow-model %1 %2)
		 (if (empty? use-effects)
		   (repeat possible-weight)
;;		   (reductions
;;		    #(rv-add %1 (get use-effects %2 rv-zero))
;;		    possible-weight
		   (reduce 
		    #(conj %1 (rv-add (peek %1) (get use-effects %2 rv-zero)))
		    [possible-weight]
		    route-ids))
		 (iterate inc 0)))))

(defn- rerun-actual-route
  [flow-model {:keys [source-id route actual-weight sink-effects use-effects] :as carrier}]
  (if (empty? sink-effects)
    (rerun-possible-route flow-model carrier)
    (let [route-ids (rseq (unbitpack-route source-id route))]
      (zipmap route-ids
	      (map #(undecay flow-model %1 %2)
;;		   (reductions
;;		    #(reduce rv-add %1 (remove nil? ((juxt sink-effects use-effects) %2)))
;;		    actual-weight
		   (reduce 
		    #(conj %1 (reduce rv-add (peek %1) (remove nil? [(sink-effects %2) (use-effects %2)])))
;;		    #(conj %1 (reduce rv-add (peek %1) (remove nil? ((juxt sink-effects use-effects) %2))))
		    [actual-weight]
		    route-ids)
		   (iterate inc 0))))))

(defn possible-flow
  [cache-layer flow-model]
  (let [rows (get-rows cache-layer)
	cols (get-cols cache-layer)]
    (if-let [carriers-with-routes (seq (filter :route (apply concat (matrix2seq cache-layer))))]
      (let [coord-map (apply merge-with rv-add
			     (map (partial rerun-possible-route flow-model) carriers-with-routes))]
	(make-matrix rows cols #(get coord-map % rv-zero)))
      (possible-source cache-layer))))
(def possible-flow (memoize possible-flow))

(defn actual-flow
  [cache-layer flow-model]
  (let [rows (get-rows cache-layer)
	cols (get-cols cache-layer)]
    (if-let [carriers-with-routes (seq (filter :route (apply concat (matrix2seq cache-layer))))]
      (let [coord-map (apply merge-with rv-add
			     (map (partial rerun-actual-route flow-model) carriers-with-routes))]
	(make-matrix rows cols #(get coord-map % rv-zero)))
      (actual-source cache-layer))))
(def actual-flow (memoize actual-flow))

(defn inaccessible-source
  "Returns a map of {location-id -> inaccessible-source}.
   Inaccessible-source is the amount of the theoretical-source which
   cannot be used by any location either due to propagation decay,
   lack of use capacity, or lack of flow pathways to use locations."
  [source-layer use-layer cache-layer]
  (map-matrix rv-subtract
	      (theoretical-source source-layer use-layer)
	      (possible-source    cache-layer)))

(defn inaccessible-use
  "Returns a map of {location-id -> inaccessible-use}.
   Inaccessible-use is the amount of the theoretical-use which cannot
   be utilized by each location either due to propagation decay of the
   asset or lack of flow pathways to use locations."
  [source-layer use-layer cache-layer]
  (map-matrix rv-subtract
	      (theoretical-use source-layer use-layer)
	      (possible-use    cache-layer)))

(defn blocked-source
  "Returns a map of {location-id -> blocked-source}.
   Blocked-source is the amount of the possible-source which cannot be
   used by any location due to upstream sinks or uses."
  [cache-layer]
  (map-matrix rv-subtract
	      (possible-source cache-layer)
	      (actual-source   cache-layer)))

(defn blocked-use
  "Returns a map of {location-id -> blocked-use}.
   Blocked-use is the amount of the possible-use which cannot be
   realized due to upstream sinks or uses."
  [cache-layer]
  (map-matrix rv-subtract
	      (possible-use cache-layer)
	      (actual-use   cache-layer)))

(defn blocked-flow
  "Returns a map of {location-id -> blocked-flow}.
   Blocked-flow is the amount of the possible-flow which cannot be
   realized due to upstream sinks or uses."
  [cache-layer flow-model]
  (map-matrix rv-subtract
	      (possible-flow cache-layer flow-model)
	      (actual-flow   cache-layer flow-model)))
