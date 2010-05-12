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
	[clj-misc.randvars   :only (rv-zero rv-add rv-divide rv-multiply)]
	[clj-misc.matrix-ops :only (filter-matrix-for-coords make-matrix get-rows get-cols)]))

(defmethod distribute-flow "Carbon"
  [_ source-layer _ use-layer _]
  "The amount of carbon sequestration produced is distributed among
   the consumers (carbon emitters) according to their relative :use
   values."
  (let [source-points     (filter-matrix-for-coords #(not= rv-zero %) source-layer)
	use-points        (filter-matrix-for-coords #(not= rv-zero %) use-layer)
	use-values        (map #(get-in use-layer %) use-points)
	total-consumption (reduce rv-add rv-zero use-values)
	percent-consumed  (zipmap use-points (map #(rv-divide % total-consumption) use-values))
	route-layer       (make-matrix (get-rows source-layer) (get-cols source-layer) #(atom ()))]
    (println "Source points:" (count source-points))
    (println "Use points:   " (count use-points))
    (dorun (pmap
	    (fn [uid]
	      (reset! (get-in route-layer uid)
		      (for [sid source-points]
			(struct-map service-carrier
			  :weight (rv-multiply (get-in source-layer sid) (percent-consumed uid))
			  :route  sid))))
	    use-points))
    route-layer))
