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
;;; This namespace defines the clj-span.java-span-bridge/run-span
;;; function, which is the main entry point into the SPAN system from
;;; external Java programs.

(ns clj-span.java-span-bridge
  (:use [clj-misc.utils      :only (p & with-message mapmap mapmap-java)]
        [clj-misc.matrix-ops :only (make-matrix)])
  (:require [clj-span.core :as core]
            (clj-misc [numbers :as nb] [varprop :as vp] [randvars :as rv]))
  (:gen-class
   :main false
   :methods [^{:static true} [runSpan [java.util.HashMap] java.util.HashMap]]))

(defn NaN-to-zero
  [double]
  (if (Double/isNaN double) 0.0 double))

(defn offset-to-yx
  [rows cols offset]
  [(- rows (quot offset cols) 1) (mod offset cols)])

(defn yx-to-offset
  [rows cols y x]
  (+ (* (- rows y 1) cols) x))

(defn unpack-layer
  [value-type rows cols layer]
  (when layer
    (if (instance? java.util.HashMap layer)
      (with-message "Unpacking Bayesian datasource..." "done."
        (let [bounds                (get layer "bounds") ; double[]
              probs-layer           (get layer "probs")  ; double[][]
              unbounded-from-below? (== Double/NEGATIVE_INFINITY (first bounds))
              unbounded-from-above? (== Double/POSITIVE_INFINITY (last  bounds))
              unpack-fn             (case value-type
                                      :randvars #(if % (rv/create-from-ranges bounds %) rv/_0_)
                                      :varprop  #(if % (vp/create-from-ranges bounds %) vp/_0_)
                                      :numbers  #(if % (nb/create-from-ranges bounds %) nb/_0_))]
          (if (or unbounded-from-below? unbounded-from-above?)
            (throw (Exception. "All undiscretized bounds must be closed above and below."))
            (make-matrix rows cols
                         (fn [[y x]]
                           (->> (yx-to-offset rows cols y x)
                                (aget probs-layer)
                                unpack-fn))))))
      (with-message "Unpacking deterministic datasource..." "done."
        (let [unpack-fn (case value-type
                          :randvars #(rv/make-randvar :discrete 1 [%])
                          :varprop  #(vp/fuzzy-number % 0.0)
                          :numbers  identity)]
          (make-matrix rows cols
                       (fn [[y x]]
                         (->> (yx-to-offset rows cols y x)
                              (aget layer)
                              NaN-to-zero
                              unpack-fn))))))))

(defn unpack-layer-map
  [value-type rows cols layer-map]
  (mapmap identity (p unpack-layer value-type rows cols) layer-map))

(defn funky-matrix2seq
  [rows cols matrix]
  (map #(get-in matrix (offset-to-yx rows cols %))
       (range (* rows cols))))

(defn pack-layer
  [value-type rows cols closure]
  (let [result-seq (funky-matrix2seq rows cols (closure))]
    (case value-type
      "numbers"  (into-array result-seq)
      "varprop"  (into-array (map (p mapmap-java name double) result-seq))
      "randvars" (into-array (map (p mapmap-java double double) result-seq)))))

(defn postprocess-results
  [value-type rows cols result-layers result-map]
  (mapmap-java
   (fn [label] (println (str "Computing " label "...")) label)
   (p pack-layer value-type rows cols)
   (select-keys result-map result-layers)))

(defn -runSpan
  [{:strs [source-layer sink-layer use-layer flow-layers rows cols
           source-threshold sink-threshold use-threshold trans-threshold
           cell-width cell-height rv-max-states downscaling-factor
           source-type sink-type use-type benefit-type
           value-type flow-model animation? result-layers]}]
  (postprocess-results value-type rows cols result-layers
                       (core/run-span {:source-layer       (unpack-layer (keyword value-type) rows cols source-layer)
                                       :sink-layer         (unpack-layer (keyword value-type) rows cols sink-layer)
                                       :use-layer          (unpack-layer (keyword value-type) rows cols use-layer)
                                       :flow-layers        (unpack-layer-map (keyword value-type) rows cols flow-layers)
                                       :source-threshold   source-threshold
                                       :sink-threshold     sink-threshold
                                       :use-threshold      use-threshold
                                       :trans-threshold    trans-threshold
                                       :cell-width         cell-width
                                       :cell-height        cell-height
                                       :rv-max-states      rv-max-states
                                       :downscaling-factor downscaling-factor
                                       :source-type        (keyword source-type)
                                       :sink-type          (keyword sink-type)
                                       :use-type           (keyword use-type)
                                       :benefit-type       (keyword benefit-type)
                                       :value-type         (keyword value-type)
                                       :flow-model         flow-model
                                       :animation?         animation?
                                       :result-type        :java-hashmap})))
