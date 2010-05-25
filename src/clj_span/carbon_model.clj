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
;;; This namespace defines the carbon model.
;;;
;;; * Routes run from Source to Use (no Sinks in this model)
;;; * Contain positive utility values only
;;; * Divides all CO2 sequestration among users by relative
;;;   consumption (i.e. Emissions)

(ns clj-span.carbon-model
  (:use [clj-span.model-api  :only (distribute-flow service-carrier)]
	[clj-misc.randvars   :only (rv-zero rv-add rv-divide rv-multiply rv-min)]
	[clj-misc.matrix-ops :only (filter-matrix-for-coords make-matrix get-rows get-cols)]))

(defmethod distribute-flow "Carbon"
  [_ source-layer _ use-layer _]
  "The amount of carbon sequestration produced is distributed among
   the consumers (carbon emitters) according to their relative :use
   values."
  (let [route-layer       (make-matrix (get-rows source-layer) (get-cols source-layer) (constantly (atom ())))
	source-points     (filter-matrix-for-coords #(not= rv-zero %) source-layer)
	use-points        (filter-matrix-for-coords #(not= rv-zero %) use-layer)]
    (println "Source points:" (count source-points))
    (println "Use points:   " (count use-points))
    (if (and (seq source-points) (seq use-points))
      (let [source-values     (map #(get-in source-layer %) source-points)
	    use-values        (map #(get-in use-layer    %) use-points)
	    total-production  (reduce rv-add rv-zero source-values)
	    total-consumption (reduce rv-add rv-zero use-values)
	    percent-produced  (map #(rv-divide % total-production) source-values)
	    source-use-ratio  (rv-divide total-production total-consumption)]
	(dorun (pmap
		(fn [uid use-cap]
		  (let [amount-usable (rv-min use-cap (rv-multiply use-cap source-use-ratio))]
		    (reset! (get-in route-layer uid)
			    (map (fn [sid source-percent]
				   (struct-map service-carrier
				     :weight (rv-multiply source-percent amount-usable)
				     :route  sid))
				 source-points
				 percent-produced))))
		use-points
		use-values))))
    route-layer))
