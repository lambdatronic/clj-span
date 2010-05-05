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

(ns clj-span.core
  (:use [clj-misc.utils         :only (mapmap)]
	[clj-span.model-api     :only (distribute-flow! distribute-flow)]
	[clj-span.params        :only (set-global-params!)]
	[clj-span.interface     :only (show-span-results-menu provide-results)]
	[clj-span.route-caching :only (cache-all-actual-routes!)]
	[clj-misc.randvars      :only (rv-mean rv-zero rv-average)]
	[clj-misc.matrix-ops    :only (map-matrix
				       downsample-matrix
				       print-matrix
				       get-rows
				       get-cols
				       get-neighbors
				       grids-align?
				       is-matrix?)])
  (:require clj-span.water-model
	    clj-span.carbon-model
	    clj-span.proximity-model
	    clj-span.line-of-sight-model))

(defstruct location :id :neighbors :source :sink :use :flow-features :carrier-cache)

(defn- make-location-map
  "Returns a map of ids to location objects, one per location in the
   data layers."
  [source-layer sink-layer use-layer flow-layers]
  (let [rows (get-rows source-layer)
	cols (get-cols source-layer)]
    (into {}
	  (for [i (range rows) j (range cols) :let [id [i j]]]
	    [id (struct-map location
		  :id            id
		  :neighbors     (get-neighbors id rows cols)
		  :source        (get-in source-layer id)
		  :sink          (get-in sink-layer   id)
		  :use           (get-in use-layer    id)
		  :flow-features (mapmap identity #(get-in % id) flow-layers)
		  :carrier-cache (atom ()))]))))

(defn- simulate-service-flows
  "Creates a network of interconnected locations, and starts a
   service-carrier propagating in every location whose source value is
   greater than 0.  These carriers propagate child carriers through
   the network which collect information about the routes traveled and
   the service weight transmitted along these routes.  When the
   simulation completes, a sequence of the locations in the network is
   returned."
  [flow-model source-layer sink-layer use-layer flow-layers]
  (let [location-map (make-location-map source-layer sink-layer use-layer flow-layers)
	locations    (vals location-map)]
    (distribute-flow! flow-model location-map (get-rows source-layer) (get-cols source-layer))
    (cache-all-actual-routes! locations flow-model)
    locations))

(def #^{:private true} nil-or-double>0? #(or (nil? %) (and (float? %) (pos? %))))
(def #^{:private true} nil-or-int>=1?   #(or (nil? %) (and (int %) (>= % 1))))
(def #^{:private true} nil-or-matrix?   #(or (nil? %) (is-matrix? %)))

(defn- zero-layer-below-threshold
  "Takes a two dimensional array of RVs and replaces all values whose
   means are less than the threshold with rv-zero."
  [threshold layer]
  (map-matrix #(if (< (rv-mean %) threshold) rv-zero %) layer))

(defn run-span
  [{:keys [source-layer  source-threshold
	   sink-layer    sink-threshold
	   use-layer     use-threshold
	   flow-layers   trans-threshold
	   rv-max-states downscaling-factor
	   sink-type     use-type
	   benefit-type  flow-model
	   result-type]
    :as input-params
    :or {source-threshold   0.0
	 sink-threshold     0.0
	 use-threshold      0.0
	 trans-threshold    0.01
	 rv-max-states      10
	 downscaling-factor 1}}]
  {:pre [(every? is-matrix?       [source-layer use-layer])
	 (every? nil-or-matrix?   (cons sink-layer (vals flow-layers)))
	 (apply grids-align?      (remove nil? (list* source-layer sink-layer use-layer (vals flow-layers))))
	 (every? nil-or-double>0? [source-threshold sink-threshold use-threshold trans-threshold])
	 (every? nil-or-int>=1?   [rv-max-states downscaling-factor])
	 (every? #{:absolute :relative} [sink-type use-type])
	 (#{:rival :non-rival} benefit-type)
	 (#{"LineOfSight" "Proximity" "Carbon" "Hydrosheds"} flow-model)
	 (#{:cli-menu :closure-map :matrix-list} result-type)]}
  ;; Initialize global parameters
  (set-global-params! {:rv-max-states      rv-max-states
		       :trans-threshold    trans-threshold
		       :sink-type          sink-type
		       :use-type           use-type
		       :benefit-type       benefit-type})
  ;; Preprocess data layers (downsampling and zeroing below their thresholds)
  (let [rows             (get-rows source-layer)
	cols             (get-cols source-layer)
	preprocess-layer (fn [[l t]] (if l (zero-layer-below-threshold t (downsample-matrix downscaling-factor rv-average l))))
	[source-layer sink-layer use-layer] (map preprocess-layer
						 {source-layer source-threshold,
						  sink-layer   sink-threshold,
						  use-layer    use-threshold})
	flow-layers (mapmap identity #(downsample-matrix downscaling-factor rv-average %) flow-layers)]
    ;; Display layers
    ;;(newline)
    ;;(apply print-matrix
    ;;(map #(map-matrix (fn [rv] (mapmap double identity rv)) %)
    ;;(remove nil? (list* source-layer sink-layer use-layer (vals flow-layers)))))
    ;;(newline)
    ;; Run flow model and return the results
    (let [route-layer (distribute-flow flow-model source-layer sink-layer use-layer flow-layers)]
      (newline)
      (print-matrix (map-matrix #(let [weights (map (comp key first :weight) (deref %))]
				   [(count (filter pos? weights))
				    (count (filter neg? weights))])
				route-layer))
      (newline)
      (println "That's all folks."))))

#_(let [locations (simulate-service-flows flow-model source-layer sink-layer use-layer flow-layers)]
    (condp = result-type
      :cli-menu      (show-span-results-menu flow-model
					     source-layer
					     sink-layer
					     use-layer
					     flow-layers
					     locations
					     downscaling-factor)
      :closure-map   (provide-results :closure-map
				      flow-model
				      locations
				      rows cols
				      downscaling-factor)
      :matrix-list   (provide-results :matrix-list
				      flow-model
				      locations
				      rows cols
				      downscaling-factor)))
