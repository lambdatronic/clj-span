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
  (:use [clj-span.params     :only (*trans-threshold*)]
	[clj-misc.matrix-ops :only (make-matrix
				    get-rows
				    get-cols
				    filter-matrix-for-coords
				    bitpack-route
				    find-line-between)]
	[clj-misc.randvars   :only (rv-zero
				    rv-add
				    rv-subtract
				    rv-multiply
				    rv-divide
				    rv-scalar-divide
				    rv-scalar-multiply
				    rv-mean
				    rv-max
				    scalar-rv-subtract
				    rv-zero-below-scalar)]
	[clj-span.model-api  :only (distribute-flow
				    decay
				    undecay
				    service-carrier)]))

;; FIXME convert step to distance metric based on map resolution and make this gaussian to 1/2 mile
(defmethod decay "LineOfSight"
  [_ weight step] (rv-scalar-divide weight (* step step)))

;; FIXME convert step to distance metric based on map resolution and make this gaussian to 1/2 mile
(defmethod undecay "LineOfSight"
  [_ weight step] (rv-scalar-multiply weight (* step step)))

;; sink decay = slow decay to 1/2 mile, fast decay to 1 mile, gone after 1 mile

(defn- raycast!
  "Finds a line of sight path between source and use points, checks
   for obstructions, and determines (using elevation info) how much of
   the source element can be seen from the use point.  A distance
   decay function is applied to the results to compute the visual
   utility originating from the source point."
  [flow-model source-type route-layer source-layer use-layer elev-layer [source-point use-point]]
  ;;(println "Projecting from" source-point "->" use-point)
  (if-let [carrier
	   (when (not= source-point use-point) ;; no in-situ use
	     (let [sight-line     (vec (find-line-between use-point source-point))
		   source-elev    (get-in elev-layer source-point)
		   use-elev       (get-in elev-layer use-point)
		   source-utility (if (== (count sight-line) 2)
				    ;; adjacent use
				    (decay flow-model
					   (rv-multiply (get-in source-layer source-point)
							(rv-zero-below-scalar
							 (scalar-rv-subtract 1 (rv-divide use-elev source-elev))
							 0))
					   1)
				    ;; distant use
				    (let [sight-slope (reduce rv-max
							      (map #(rv-scalar-divide 
								     (rv-subtract (get-in elev-layer (sight-line %)) use-elev)
								     %)
								   (range 1 (dec (count sight-line)))))
					  projected-source-elev (rv-add use-elev (rv-scalar-multiply sight-slope (dec (count sight-line))))
					  visible-source-fraction (rv-zero-below-scalar
								   (scalar-rv-subtract 1 (rv-divide projected-source-elev source-elev))
								   0)]
				      (decay flow-model
					     (rv-multiply (get-in source-layer source-point) visible-source-fraction)
					     (dec (count sight-line)))))]
	       (when (> (rv-mean source-utility) *trans-threshold*)
		 (struct-map service-carrier
		   :weight (if (= source-type :sinks) (rv-scalar-multiply source-utility -1) source-utility)
		   :route  (bitpack-route sight-line)))))]
    (swap! (get-in route-layer use-point) conj carrier)))

;; Detects all sources and sinks visible from the use-point and stores
;; their utility contributions in the route-layer."
(defmethod distribute-flow "LineOfSight"
  [flow-model source-layer sink-layer use-layer {elev-layer "Altitude"}]
  (let [route-layer   (make-matrix (get-rows source-layer) (get-cols source-layer) #(atom ()))
	source-points (filter-matrix-for-coords #(not= rv-zero %) source-layer)
	sink-points   (filter-matrix-for-coords #(not= rv-zero %) sink-layer)
	use-points    (filter-matrix-for-coords #(not= rv-zero %) use-layer)]
    (println "Source points:" (count source-points))
    (println "Sink points:  " (count sink-points))
    (println "Use points:   " (count use-points))
    (dorun (map (partial raycast! flow-model :sources route-layer source-layer use-layer elev-layer)
		(for [source-point source-points use-point use-points] [source-point use-point])))
    (println "All sources assessed.")
    (dorun (map (partial raycast! flow-model :sinks   route-layer sink-layer   use-layer elev-layer)
		(for [sink-point sink-points use-point use-points] [sink-point use-point])))
    (println "All sinks assessed.")
    route-layer))
