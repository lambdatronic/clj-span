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

(ns clj-span.flow-model
  (:use [clj-span.model-api     :only (distribute-flow!)]
	[clj-span.worldgen      :only (make-location-map)]
	[clj-span.route-caching :only (cache-all-actual-routes!)])
  (:require clj-span.water-model
	    clj-span.carbon-model
	    clj-span.proximity-model
	    clj-span.line-of-sight-model))

(defn simulate-service-flows
  "Creates a network of interconnected locations, and starts a
   service-carrier propagating in every location whose source value is
   greater than 0.  These carriers propagate child carriers through
   the network which collect information about the routes traveled and
   the service weight transmitted along the route.  When the
   simulation completes, a sequence of the locations in the network is
   returned."
  [flow-model source-layer sink-layer use-layer flow-layers]
  {:pre [(#{"LineOfSight" "Proximity" "Carbon" "Hydrosheds"} flow-model)]}
  (let [rows         (count source-layer)
	cols         (count (first source-layer))
	location-map (make-location-map source-layer sink-layer use-layer flow-layers)
	locations    (vals location-map)]
    (distribute-flow! flow-model location-map rows cols)
    (cache-all-actual-routes! locations flow-model)
    locations))
