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
;;; This namespace defines the proximity model.
;;;
;;; * Routes run from Source to Use and Sink to Use
;;; * Contain positive and negative utility values (total source +
;;;   sink values)
;;; * Searches outward from Source points until decay and sink effects
;;;   block the frontier's progress

(ns clj-span.proximity-model
  (:use [clj-span.params     :only (*trans-threshold*)]
	[clj-span.model-api  :only (distribute-flow decay undecay service-carrier)]
	[clj-misc.randvars   :only (rv-zero rv-zero-below-scalar rv-subtract rv-scalar-multiply rv-scalar-divide rv-mean)]
	[clj-misc.matrix-ops :only (get-neighbors make-matrix get-rows get-cols filter-matrix-for-coords bitpack-route find-bounding-box)]))

;; FIXME convert step to distance metric based on map resolution and make this gaussian to 1/2 mile
(defmethod decay "Proximity"
  [_ weight step] (if (> step 1) (rv-scalar-divide weight (* step step)) weight))

;; FIXME convert step to distance metric based on map resolution and make this gaussian to 1/2 mile
(defmethod undecay "Proximity"
  [_ weight step] (if (> step 1) (rv-scalar-multiply weight (* step step)) weight))

(defn- make-frontier-element!
  "If the location is a sink, its id is saved on the sinks-encountered
   list and its sink-value is subtracted from the current utility
   along this path.  If the location is a use point, negative utility
   carriers are stored on it from each of the previously encountered
   sinks as well as a positive utility carrier from the original
   source point. This function returns a pair of [location-id
   [outgoing-utility route-including-location-id sinks-encountered]]."
  [location-id flow-model route-layer source-layer sink-layer
   use-layer [incoming-utility exclusive-route sinks-encountered]]
  (let [sink-value        (get-in sink-layer location-id)
	use-value         (get-in use-layer  location-id)
	inclusive-route   (conj exclusive-route location-id)
	outgoing-utility  (if (not= rv-zero sink-value) ; sink-location?
			    (rv-zero-below-scalar (rv-subtract incoming-utility sink-value) 0)
			    incoming-utility)
	sinks-encountered (if (not= rv-zero sink-value) ; sink location?
			    (conj sinks-encountered location-id)
			    sinks-encountered)]
    (when (not= rv-zero use-value)	; use location?
      (let [carrier-cache (get-in route-layer location-id)]
	(loop [remaining-sinks sinks-encountered
	       remaining-route inclusive-route]
	  (when (seq remaining-sinks)
	    (let [sink-id         (first remaining-sinks)
		  route-from-sink (drop-while #(not= sink-id %) remaining-route)
		  sink-utility    (decay flow-model (get-in sink-layer sink-id) (dec (count route-from-sink)))]
	      (swap! carrier-cache conj
		     (struct-map service-carrier
		       :weight (rv-scalar-multiply sink-utility -1)
		       :route  (bitpack-route route-from-sink)))
	      (recur (rest remaining-sinks) route-from-sink))))
	(swap! carrier-cache conj
	       (struct-map service-carrier
		 :weight (decay flow-model (get-in source-layer (first inclusive-route)) (dec (count inclusive-route)))
		 :route  (bitpack-route inclusive-route)))))
    [location-id [outgoing-utility inclusive-route sinks-encountered]]))

(defn- distribute-gaussian!
  [flow-model route-layer source-layer sink-layer use-layer rows cols source-id source-weight]
  (loop [frontier (into {} (list (make-frontier-element! source-id
							 flow-model
							 route-layer
							 source-layer
							 sink-layer
							 use-layer
							 [source-weight [] []])))]
    (when (seq frontier)
      (recur (into {}
		   (remove #(or (nil? %)
				(let [[_ [u r _]] %]
				  (< (rv-mean (decay flow-model u (dec (count r))))
				     *trans-threshold*)))
			   (for [boundary-id (find-bounding-box (keys frontier) rows cols)]
			     (when-let [frontier-options (seq (remove nil? (map frontier (get-neighbors boundary-id rows cols))))]
			       (make-frontier-element! boundary-id flow-model route-layer source-layer sink-layer use-layer
						     (apply max-key (fn [[u r s]] (rv-mean u)) frontier-options))))))))))

(defmethod distribute-flow "Proximity"
  [flow-model source-layer sink-layer use-layer _]
  (let [rows          (get-rows source-layer)
	cols          (get-cols source-layer)
	route-layer   (make-matrix rows cols #(atom ()))
	source-points (filter-matrix-for-coords #(not= rv-zero %) source-layer)]
    (println "Source points:" (count source-points))
    (dorun (map
	    (partial distribute-gaussian! flow-model route-layer source-layer sink-layer use-layer rows cols)
	    source-points
	    (map #(get-in source-layer %) source-points)))
    route-layer))
