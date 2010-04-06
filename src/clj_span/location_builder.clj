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

(ns clj-span.location-builder
  (:use [clj-misc.utils      :only (maphash seq2map)]
	[clj-span.randvars   :only (unpack-datasource)]
	[clj-misc.matrix-ops :only (get-neighbors)]))
(refer 'corescience :only '(find-state
			    find-observation
			    get-state-map))

(defstruct location :id :neighbors :source :sink :use :flow-features :carrier-cache)

(defn- extract-values-by-concept-clean
  "Returns a seq of the concept's values in the observation,
   which are doubles or probability distributions."
  [obs conc n]
  (unpack-datasource (find-state obs conc) n))

(defn- extract-values-by-concept
  "Returns a seq of the concept's values in the observation,
   which are doubles or probability distributions."
  [obs conc n]
  (println "EXTRACT-VALUES-BY-CONCEPT...")
  (println "OBS:  " obs)
  (println "CONC: " conc)
  (println "STATE:" (find-state obs conc))
  (unpack-datasource (find-state obs conc) n))

(defn- extract-all-values-clean
  "Returns a map of concept-names to vectors of doubles or probability
   distributions."
  [obs conc n]
  (when conc
    (maphash (memfn getLocalName) #(vec (unpack-datasource % n)) (get-state-map (find-observation obs conc)))))

(defn- extract-all-values
  "Returns a map of concept-names to vectors of doubles or probability
   distributions."
  [obs conc n]
  (let [subobs (find-observation obs conc)
	states (get-state-map subobs)]
    (println "EXTRACT-ALL-VALUES...")
    (println "OBS:   " obs)
    (println "CONC:  " conc)
    (println "SUBOBS:" subobs)
    (println "STATES:" states)
    (when conc
      (maphash (memfn getLocalName) #(vec (unpack-datasource % n)) (get-state-map (find-observation obs conc))))))

(defn make-location-map
  "Returns a map of ids to location objects, one per location in the
   observation set."
  [observation source-conc sink-conc use-conc flow-conc rows cols]
  (let [n             (* rows cols)
	flow-vals-map (extract-all-values observation flow-conc n)]
    (println "Rows x Cols:" n)
    (println "Flow-vals-map length:" (count flow-vals-map))
    (seq2map
     (map (fn [[i j] source sink use]
	    (struct-map location
	      :id            [i j]
	      :neighbors     (vec (get-neighbors [i j] rows cols))
	      :source        source
	      :sink          sink
	      :use           use
	      :flow-features (maphash identity #(% (+ (* i cols) j)) flow-vals-map)))
	      ;;:carrier-cache (atom ()))) ;; FIXME make carrier-cache into a [] to save memory (do vecs save memory?)
	  (for [i (range rows) j (range cols)] [i j])
	  (extract-values-by-concept observation source-conc n)
	  (extract-values-by-concept observation sink-conc   n)
	  (extract-values-by-concept observation use-conc    n))
     (fn [loc] [(:id loc) loc]))))

(defn make-location-map-special
  "Returns a map of ids to location objects, one per location in the
   observation set."
  [source-layer sink-layer use-layer flow-layers rows cols]
  (seq2map
   (for [i (range rows) j (range cols) :let [id [i j]]]
     (struct-map location
       :id            id
       :neighbors     (get-neighbors id rows cols)
       :source        (get-in source-layer id)
       :sink          (get-in sink-layer   id)
       :use           (get-in use-layer    id)
       :flow-features (maphash identity #(get-in % id) flow-layers)
       :carrier-cache (atom ())))
   (fn [loc] [(:id loc) loc])))
