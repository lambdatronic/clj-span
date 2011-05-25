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
;;;& _0_ rv-zero
;;;& _+_ rv-add
;;;& _-_ rv-subtract
;;; _*_ rv-multiply !!!
;;; _d_ rv-divide  !!!
;;;& _<_ rv-lt?
;;;& _>_ rv-gt?
;;;&  +_ scalar-rv-add
;;;&  -_ scalar-rv-subtract
;;;&  *_ scalar-rv-multiply
;;;  d_ scalar-rv-divide  !!!
;;;& _+  rv-scalar-add
;;;& _-  rv-scalar-subtract
;;;& _*  rv-scalar-multiply
;;;& _d  rv-scalar-divide
;;; rv-fn !!!
;;;& rv-min
;;;& rv-max
;;;& draw
;;;& rv-above?
;;;& rv-below?
;;;& rv-intensive-sampler
;;;& rv-mean

(ns clj-misc.varprop
  (:use [clj-misc.utils :only [my-partition-all]]))

(defrecord FuzzyNumber [mean var])

(defn fuzzy-number
  "Constructs a FuzzyNumber."
  [mean var]
  (FuzzyNumber. mean var))

(def rv-zero (fuzzy-number 0.0 0.0))
(def _0_ rv-zero)

(defn rv-add
  "Returns the sum of two FuzzyNumbers."
  [{mx :mean, vx :var} {my :mean, vy :var}]
  (fuzzy-number (+ mx my) (+ vx vy)))
(def _+_ rv-add)

(defn rv-subtract
  "Returns the difference of two FuzzyNumbers."
  [{mx :mean, vx :var} {my :mean, vy :var}]
  (fuzzy-number (- mx my) (+ vx vy)))
(def _-_ rv-subtract)

;;(defn rv-multiply
;;  "Returns the product of two FuzzyNumbers."
;;  [{mx :mean, vx :var} {my :mean, vy :var}]
;;  (fuzzy-number ? ?))
;;(def _*_ rv-multiply)

;;(defn rv-divide
;;  "Returns the quotient of two FuzzyNumbers."
;;  [{mx :mean, vx :var} {my :mean, vy :var}]
;;  (fuzzy-number ? ?))
;;(def _d_ rv-divide)

(defn scalar-rv-add
  "Returns the sum of a constant and a FuzzyNumber."
  [x Y]
  (fuzzy-number (+ x (:mean Y)) (:var Y)))
(def +_ scalar-rv-add)

(defn scalar-rv-subtract
  "Returns the difference of a constant and a FuzzyNumber."
  [x Y]
  (fuzzy-number (- x (:mean Y)) (:var Y)))
(def -_ scalar-rv-subtract)

(defn scalar-rv-multiply
  "Returns the product of a constant and a FuzzyNumber."
  [x Y]
  (fuzzy-number (* x (:mean Y)) (* x x (:var Y))))
(def *_ scalar-rv-multiply)

;;(defn scalar-rv-divide
;;  "Returns the quotient of a constant and a FuzzyNumber."
;;  [x Y]
;;  (fuzzy-number ? ?))
;;(def d_ scalar-rv-divide)

(defn rv-scalar-add
  "Returns the sum of a FuzzyNumber and a constant."
  [X y]
  (fuzzy-number (+ (:mean X) y) (:var X)))
(def _+ rv-scalar-add)

(defn rv-scalar-subtract
  "Returns the difference of a FuzzyNumber and a constant."
  [X y]
  (fuzzy-number (- (:mean X) y) (:var X)))
(def _- rv-scalar-subtract)

(defn rv-scalar-multiply
  "Returns the product of a FuzzyNumber and a constant."
  [X y]
  (fuzzy-number (* (:mean X) y) (* (:var X) y y)))
(def _* rv-scalar-multiply)

(defn rv-scalar-divide
  "Returns the quotient of a FuzzyNumber and a constant."
  [X y]
  (fuzzy-number (/ (:mean X) y) (/ (:var X) y y)))
(def _d rv-scalar-divide)

;;(defn rv-fn
;;  [f X Y]
;;  ???)

(defn rv-lt?
  "Compares two FuzzyNumbers and returns true if P(X < Y) > 0.5."
  [X Y]
  (< (:mean X) (:mean Y)))
(def _<_ rv-lt?)

(defn rv-gt?
  "Compares two FuzzyNumbers and returns true if P(X > Y) > 0.5."
  [X Y]
  (> (:mean X) (:mean Y)))
(def _>_ rv-gt?)

(defn rv-below?
  "Compares a FuzzyNumber and a scalar and returns true if P(X < y) > 0.5."
  [X y]
  (< (:mean X) y))
(def _< rv-below?)

(defn rv-above?
  "Compares a FuzzyNumber and a scalar and returns true if P(X > y) > 0.5."
  [X y]
  (> (:mean X) y))
(def _> rv-below?)

(defn rv-min
  "Returns the smaller of two FuzzyNumbers using _<_."
  [X Y]
  (if (_<_ X Y) X Y))

(defn rv-max
  "Returns the greater of two FuzzyNumbers using _>_."
  [X Y]
  (if (_>_ X Y) X Y))

(defn rv-mean
  "Returns the mean of a FuzzyNumber."
  [X]
  (:mean X))

(defn rv-sum
  "Returns the sum of a sequence of FuzzyNumbers using _+_."
  [Xs]
  (cond (== (count Xs) 1)
        (first Xs)

        (<= (count Xs) 20)
        (reduce _+_ Xs)

        :otherwise
        (recur (pmap rv-sum
                     (my-partition-all 20 Xs)))))

(defn rv-extensive-sampler
  "Returns the extensive weighted sum of a coverage (i.e. a sequence
   of pairs of [value fraction-covered]). For use with
   clj-misc.matrix-ops/resample-matrix."
  [coverage]
  (rv-sum (map (fn [[val frac]] (_* val frac)) coverage)))

(defn rv-intensive-sampler
  "Returns the intensive weighted sum of a coverage (i.e. a sequence
   of pairs of [value fraction-covered]). For use with
   clj-misc.matrix-ops/resample-matrix."
  [coverage]
  (let [frac-sum (reduce + (map second coverage))]
    (rv-sum (map (fn [[val frac]] (_* val (/ frac frac-sum))) coverage))))

(let [stored-val (atom nil)]
  (defn marsaglia-normal
    "Returns a value from X~N(0,1). Uses the Marsaglia polar
     method. Memoizes extra computed values for quicker lookups on
     even calls."
    []
    (when-let [normal-val @stored-val]
      (reset! stored-val nil)
      normal-val)
    (let [v1 (dec (* 2.0 (rand)))
          v2 (dec (* 2.0 (rand)))
          s  (+ (* v1 v1) (* v2 v2))]
      (if (and (not= s 0.0) (< s 1.0))
        (let [theta (Math/sqrt (/ (* -2.0 (Math/log s)) s))]
          (reset! stored-val (* v1 theta))
          (* v2 theta))
        (recur)))))

(let [stored-val (atom nil)]
  (defn box-muller-normal
    "Returns a value from X~N(0,1). Uses the Box-Muller
     transform. Memoizes extra computed values for quicker lookups on
     even calls."
    []
    (when-let [normal-val @stored-val]
      (reset! stored-val nil)
      normal-val)
    (let [u1    (+ (rand) 1e-6) ;; adding delta=1e-6 to prevent computing log(0) below
          u2    (rand)
          r     (Math/sqrt (* -2.0 (Math/log u1)))
          theta (* 2.0 Math/PI u2)
          n1    (* r (Math/cos theta))
          n2    (* r (Math/sin theta))]
      (reset! stored-val n1)
      n2)))

(defn draw
  "Extracts a deterministic value from a FuzzyNumber by modelling it
   as a normal distribution."
  [X]
  (+ (* (marsaglia-normal) (:var X)) (:mean X)))
