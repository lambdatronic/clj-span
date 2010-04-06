;;; Copyright 2009 Gary Johnson
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
  (:use [clj-misc.utils            :only (maphash)]
	[clj-span.location-builder :only (make-location-map)]
	[clj-span.params           :only (set-global-params!)]
	[clj-span.model-api        :only (distribute-flow!)]
	[clj-span.actualizer       :only (cache-all-actual-routes!)])
  (:require clj-span.water-model
	    clj-span.carbon-model
	    clj-span.proximity-model
	    clj-span.line-of-sight-model))

(defn- load-params!
  []
  (with-open [foo (java.io.PushbackReader. (java.io.FileReader. "/tmp/span-flow-params.txt"))]
    (binding [*in* foo]
      (set-global-params! (read)))))

(defn- store-data
  [r c fcn lm]
  (with-open [foo (java.io.FileWriter. "/tmp/span-location-data.txt")]
    (binding [*out*        foo
	      *print-meta* true]
      (prn r c fcn lm))))

(defn- load-data
  []
  (with-open [foo (java.io.PushbackReader. (java.io.FileReader. "/tmp/span-location-data.txt"))]
    (binding [*in* foo]
      [(read) (read) (read) (read)])))

(defn- add-atoms-to
  [location-map]
  (maphash identity #(assoc % :carrier-cache (atom ())) location-map))

(defn simulate-service-flows
  "Creates a network of interconnected locations, and starts a
   service-carrier propagating in every location whose source value is
   greater than 0.  These carriers propagate child carriers through
   the network which collect information about the routes traveled and
   the service weight transmitted along the route.  When the
   simulation completes, a sequence of the locations in the network is
   returned."
  [observation source-conc sink-conc use-conc flow-conc flow-conc-name rows cols]
  (let [lm (make-location-map observation
			      source-conc
			      sink-conc
			      use-conc
			      flow-conc
			      rows cols)
	location-map (add-atoms-to lm)
	locations    (vals location-map)]
    ;;(store-data rows cols flow-conc-name lm)
    ;;(throw (Exception. "Exiting early."))
    (distribute-flow! flow-conc-name location-map rows cols)
    (cache-all-actual-routes! locations flow-conc-name)
    locations))

(defn ssf
  []
  (load-params!)
  (let [[rows cols flow-conc-name lm] (load-data)
	location-map (add-atoms-to lm)
	locations    (vals location-map)]
    (distribute-flow! flow-conc-name location-map rows cols)
    (cache-all-actual-routes! locations flow-conc-name)
    (println "Locs:         " (count locations))
    (println "Filled Caches:" (count (filter #(not (empty? @(:carrier-cache %))) locations)))))
