;;; Copyright 2010 Gary Johnson
;;;
;;; This file is part of clj-misc.
;;;
;;; clj-misc is free software: you can redistribute it and/or modify
;;; it under the terms of the GNU General Public License as published
;;; by the Free Software Foundation, either version 3 of the License,
;;; or (at your option) any later version.
;;;
;;; clj-misc is distributed in the hope that it will be useful, but
;;; WITHOUT ANY WARRANTY; without even the implied warranty of
;;; MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
;;; General Public License for more details.
;;;
;;; You should have received a copy of the GNU General Public License
;;; along with clj-misc.  If not, see <http://www.gnu.org/licenses/>.
;;;
;;;-------------------------------------------------------------------
;;;
;;; This namespace defines functions for creating, querying, and
;;; manipulating random variables, which are defined to be maps of
;;; {state -> probability}.  Both discrete and continuous random
;;; variables are supported.  Continuous RVs are represented using
;;; samples from their cumulative distribution functions (CDFs).
;;;
;;; This library also provides some shorthand abbreviations for many
;;; of the arithmetic functions:
;;;
;;; _0_ rv-zero
;;; _+_ rv-add
;;; _-_ rv-subtract
;;; _*_ rv-multiply !!!
;;; _d_ rv-divide  !!!
;;; _<_ rv-lt?
;;; _>_ rv-gt?
;;;  +_ scalar-rv-add
;;;  -_ scalar-rv-subtract
;;;  *_ scalar-rv-multiply
;;;  d_ scalar-rv-divide  !!!
;;; _+  rv-scalar-add
;;; _-  rv-scalar-subtract
;;; _*  rv-scalar-multiply
;;; _d  rv-scalar-divide
;;;
;;; rv-fn, rv-min, rv-max,
;;; cont-type, disc-type successive-sums, draw, make-randvar, rv-above?, rv-below?,
;;; rv-intensive-sampler, rv-mean, rv-pos
;;;
;;; Currently used, but probably not in the future:
;;; *rv-max-states*, reset-rv-max-states,
;;; rv-zero-ish?, rv-convolutions, rv-resample


(ns clj-misc.varprop
  (:use [clj-misc.utils :only (p my-partition-all constraints-1.0 mapmap seq2map dissoc-vec)]))

(defrecord FuzzyNumber [mean var])

(def rv-zero (FuzzyNumber. 0 0))
(def _0_ rv-zero)

(defn rv-add
  "Returns the sum of two FuzzyNumbers"
  [{mx :mean, vx :var} {my :mean, vy :var}]
  (FuzzyNumber. (+ mx my) (+ vx vy)))
(def _+_ rv-add)

(defn rv-subtract
  "Returns the difference of two FuzzyNumbers"
  [{mx :mean, vx :var} {my :mean, vy :var}]
  (FuzzyNumber. (- mx my) (+ vx vy)))
(def _-_ rv-subtract)

(defn rv-gt?
  "Compares two FuzzyNumbers and returns true if P(X > Y) > 0.5."
  [X Y]
  (> (:mean X) (:mean Y)))
(def _>_ rv-gt?)

(defn rv-lt?
  "Compares two FuzzyNumbers and returns true if P(X < Y) > 0.5."
  [X Y]
  (< (:mean X) (:mean Y)))
(def _<_ rv-lt?)

