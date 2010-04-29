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
;;; This namespace defines the run-span, simulate-service-flows, and
;;; data-preprocessing functions.  run-span is the main entry point
;;; into the SPAN system and may be called with a number of different
;;; options specifying the form of its results.

(ns span.aries-span-bridge
  (:use [clj-span.core       :only (run-span)]
	[clj-misc.utils      :only (mapmap)]
	[clj-misc.matrix-ops :only (seq2matrix)]
	[clj-misc.randvars   :only (cont-type disc-type successive-sums)]))

(refer 'geospace :only '(grid-rows
			 grid-columns))

(refer 'corescience :only '(find-state
			    find-observation
			    get-state-map
			    get-observable-class))

(refer 'modelling   :only '(probabilistic?
			    binary?
			    encodes-continuous-distribution?
			    get-dist-breakpoints
			    get-possible-states
			    get-probabilities
			    get-data))

(defn- unpack-datasource
  "Returns a seq of length n of the values in ds,
   represented as probability distributions.  All values and
   probabilities are represented as rationals."
  [ds rows cols]
  (println "DS:           " ds)
  (println "PROBABILISTIC?" (probabilistic? ds))
  (println "ENCODES?      " (encodes-continuous-distribution? ds))
  (let [n            (* rows cols)
	to-rationals (partial map #(if (Double/isNaN %) 0 (rationalize %)))]
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

(defn- layer-from-observation
  "Builds a rows x cols matrix (vector of vectors) of the concept's
   state values in the observation."
  [observation concept rows cols]
  (let [states (when concept
		 (unpack-datasource (find-state observation concept) rows cols))]
    (seq2matrix rows cols states)))

(defn- layer-map-from-observation
  "Builds a map of {concept-names -> matrices}, where each concept's
   matrix is a rows x cols vector of vectors of the concept's state
   values in the observation."
  [observation concept rows cols]
  (let [state-map (when conc
		    (mapmap (memfn getLocalName) #(unpack-datasource % rows cols) (get-state-map (find-observation observation concept))))]
    (mapmap identity (partial seq2matrix rows cols) state-map)))

(defn span-driver
  "Takes the source, sink, use, and flow concepts along with the
   flow-params map and an observation containing the concepts'
   dependent features, calculates the SPAN flows, and returns the
   results using one of the following result-types:
   :closure-map :matrix-list :raw-locations"
  ([observation source-concept use-concept sink-concept flow-concept flow-params]
     (span-driver observation source-concept use-concept sink-concept flow-concept flow-params :closure-map))
  ([observation source-concept use-concept sink-concept flow-concept
    {:keys [source-threshold sink-threshold use-threshold trans-threshold
	    rv-max-states downscaling-factor sink-type use-type benefit-type]}
    result-type]
     ;; This version of SPAN only works for grid-based observations (i.e. raster maps).
     {:pre [(grid-extent? observation)]}
     (let [rows         (grid-rows    observation)
	   cols         (grid-columns observation)
	   flow-model   (.getLocalName (get-observable-class observation))
	   source-layer (layer-from-observation     observation source-concept rows cols)
	   sink-layer   (layer-from-observation     observation sink-concept   rows cols)
	   use-layer    (layer-from-observation     observation use-concept    rows cols)
	   flow-layers  (layer-map-from-observation observation flow-concept   rows cols)]
       (run-span {:source-layer       source-layer
		  :source-threshold   source-threshold
		  :sink-layer         sink-layer
		  :sink-threshold     sink-threshold
		  :use-layer          use-layer
		  :use-threshold      use-threshold
		  :flow-layers        flow-layers
		  :trans-threshold    trans-threshold
		  :rv-max-states      rv-max-states
		  :downscaling-factor downscaling-factor
		  :sink-type          sink-type
		  :use-type           use-type
		  :benefit-type       benefit-type
		  :flow-model         flow-model
		  :result-type        result-type}))))
