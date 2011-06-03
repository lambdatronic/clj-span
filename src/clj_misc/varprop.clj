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

(ns clj-misc.varprop
  (:use [clj-misc.utils :only [my-partition-all replace-all]]))

(defrecord FuzzyNumber [mean var])

(defn fuzzy-number
  "Constructs a FuzzyNumber."
  [mean var]
  (FuzzyNumber. mean var))

(def _0_ (fuzzy-number 0.0 0.0))

(defn _+_
  "Returns the sum of two FuzzyNumbers."
  [{mx :mean, vx :var} {my :mean, vy :var}]
  (fuzzy-number (+ mx my) (+ vx vy)))

(defn _-_
  "Returns the difference of two FuzzyNumbers."
  [{mx :mean, vx :var} {my :mean, vy :var}]
  (fuzzy-number (- mx my) (+ vx vy)))

(defn _*_
  "Returns the product of two FuzzyNumbers."
  [{mx :mean, vx :var} {my :mean, vy :var}]
  (fuzzy-number (* mx my) (+ (* vx vy) (* mx mx vy) (* my my vx))))

(declare d_)

(defn _d_
  "Returns the quotient of two FuzzyNumbers."
  [X Y]
  (_*_ X (d_ 1 Y)))

(defn _+
  "Returns the sum of a FuzzyNumber and a constant."
  [X y]
  (fuzzy-number (+ (:mean X) y) (:var X)))

(defn _-
  "Returns the difference of a FuzzyNumber and a constant."
  [X y]
  (fuzzy-number (- (:mean X) y) (:var X)))

(defn _*
  "Returns the product of a FuzzyNumber and a constant."
  [X y]
  (fuzzy-number (* (:mean X) y) (* (:var X) y y)))

(defn _d
  "Returns the quotient of a FuzzyNumber and a constant."
  [X y]
  (fuzzy-number (/ (:mean X) y) (/ (:var X) y y)))

(defn +_
  "Returns the sum of a constant and a FuzzyNumber."
  [x Y]
  (fuzzy-number (+ x (:mean Y)) (:var Y)))

(defn -_
  "Returns the difference of a constant and a FuzzyNumber."
  [x Y]
  (fuzzy-number (- x (:mean Y)) (:var Y)))

(defn *_
  "Returns the product of a constant and a FuzzyNumber."
  [x Y]
  (fuzzy-number (* x (:mean Y)) (* x x (:var Y))))

(defn d_
  "Returns the quotient of a constant and a FuzzyNumber."
  [x {:keys [mean var]}]
  (fuzzy-number (/ x mean) (/ (* x x var) (Math/pow mean 4))))

(defn _<_
  "Compares two FuzzyNumbers and returns true if P(X < Y) > 0.5."
  [X Y]
  (< (:mean X) (:mean Y)))

(defn _>_
  "Compares two FuzzyNumbers and returns true if P(X > Y) > 0.5."
  [X Y]
  (> (:mean X) (:mean Y)))

(defn _<
  "Compares a FuzzyNumber and a scalar and returns true if P(X < y) > 0.5."
  [X y]
  (< (:mean X) y))

(defn _>
  "Compares a FuzzyNumber and a scalar and returns true if P(X > y) > 0.5."
  [X y]
  (> (:mean X) y))

(defn <_
  "Compares a scalar and a FuzzyNumber and returns true if P(Y > x) > 0.5."
  [x Y]
  (< x (:mean Y)))

(defn >_
  "Compares a scalar and a FuzzyNumber and returns true if P(Y < x) > 0.5."
  [x Y]
  (> x (:mean Y)))

(defn _min_
  "Returns the smaller of two FuzzyNumbers using _<_."
  [X Y]
  (if (_<_ X Y) X Y))

(defn _max_
  "Returns the greater of two FuzzyNumbers using _>_."
  [X Y]
  (if (_>_ X Y) X Y))

(defn _min
  "Returns the smaller of a FuzzyNumber and a scalar using _<."
  [X y]
  (if (_< X y) X y))

(defn _max
  "Returns the greater of a FuzzyNumber and a scalar using _>."
  [X y]
  (if (_> X y) X y))

(defn min_
  "Returns the smaller of a scalar and a FuzzyNumber using <_."
  [x Y]
  (if (<_ x Y) x Y))

(defn max_
  "Returns the greater of a scalar and a FuzzyNumber using >_."
  [x Y]
  (if (>_ x Y) x Y))

(defmulti ?+?
  "Returns the sum of two values, which may be FuzzyNumbers or constants. Uses reflection."
  (fn [X Y] [(type X) (type Y)]))

(defmethod ?+? [FuzzyNumber FuzzyNumber] [X Y] (_+_ X Y))
(defmethod ?+? [FuzzyNumber Number]      [X Y] (_+  X Y))
(defmethod ?+? [Number      FuzzyNumber] [X Y] ( +_ X Y))
(defmethod ?+? [Number      Number]      [X Y] ( +  X Y))

(defmulti ?-?
  "Returns the difference of two values, which may be FuzzyNumbers or constants. Uses reflection."
  (fn [X Y] [(type X) (type Y)]))

(defmethod ?-? [FuzzyNumber FuzzyNumber] [X Y] (_-_ X Y))
(defmethod ?-? [FuzzyNumber Number]      [X Y] (_-  X Y))
(defmethod ?-? [Number      FuzzyNumber] [X Y] ( -_ X Y))
(defmethod ?-? [Number      Number]      [X Y] ( -  X Y))

(defmulti ?*?
  "Returns the product of two values, which may be FuzzyNumbers or constants. Uses reflection."
  (fn [X Y] [(type X) (type Y)]))

(defmethod ?*? [FuzzyNumber FuzzyNumber] [X Y] (_*_ X Y))
(defmethod ?*? [FuzzyNumber Number]      [X Y] (_*  X Y))
(defmethod ?*? [Number      FuzzyNumber] [X Y] ( *_ X Y))
(defmethod ?*? [Number      Number]      [X Y] ( *  X Y))

(defmulti ?d?
  "Returns the quotient of two values, which may be FuzzyNumbers or constants. Uses reflection."
  (fn [X Y] [(type X) (type Y)]))

(defmethod ?d? [FuzzyNumber FuzzyNumber] [X Y] (_d_ X Y))
(defmethod ?d? [FuzzyNumber Number]      [X Y] (_d  X Y))
(defmethod ?d? [Number      FuzzyNumber] [X Y] ( d_ X Y))
(defmethod ?d? [Number      Number]      [X Y] ( /  X Y))

(defmulti ?<?
  "Compares two values, which may be FuzzyNumbers of constants, and returns true if X < Y. Uses reflection."
  (fn [X Y] [(type X) (type Y)]))

(defmethod ?<? [FuzzyNumber FuzzyNumber] [X Y] (_<_ X Y))
(defmethod ?<? [FuzzyNumber Number]      [X Y] (_<  X Y))
(defmethod ?<? [Number      FuzzyNumber] [X Y] ( <_ X Y))
(defmethod ?<? [Number      Number]      [X Y] ( <  X Y))

(defmulti ?>?
  "Compares two values, which may be FuzzyNumbers of constants, and returns true if X > Y. Uses reflection."
  (fn [X Y] [(type X) (type Y)]))

(defmethod ?>? [FuzzyNumber FuzzyNumber] [X Y] (_>_ X Y))
(defmethod ?>? [FuzzyNumber Number]      [X Y] (_>  X Y))
(defmethod ?>? [Number      FuzzyNumber] [X Y] ( >_ X Y))
(defmethod ?>? [Number      Number]      [X Y] ( >  X Y))

(defmulti ?min?
  "Returns the smaller of two values, which may be FuzzyNumbers or constants. Uses reflection."
  (fn [X Y] [(type X) (type Y)]))

(defmethod ?min? [FuzzyNumber FuzzyNumber] [X Y] (_min_ X Y))
(defmethod ?min? [FuzzyNumber Number]      [X Y] (_min  X Y))
(defmethod ?min? [Number      FuzzyNumber] [X Y] ( min_ X Y))
(defmethod ?min? [Number      Number]      [X Y] ( min  X Y))

(defmulti ?max?
  "Returns the larger of two values, which may be FuzzyNumbers or constants. Uses reflection."
  (fn [X Y] [(type X) (type Y)]))

(defmethod ?max? [FuzzyNumber FuzzyNumber] [X Y] (_max_ X Y))
(defmethod ?max? [FuzzyNumber Number]      [X Y] (_max  X Y))
(defmethod ?max? [Number      FuzzyNumber] [X Y] ( max_ X Y))
(defmethod ?max? [Number      Number]      [X Y] ( max  X Y))

(def fuzzy-arithmetic-mapping
  '{+   ?+?
    -   ?-?
    *   ?*?
    /   ?d?
    <   ?<?
    >   ?>?
    min ?min?
    max ?max?})

(defn fuzzify-fn
  "Transforms f into its fuzzy arithmetic equivalent, using the
   mappings defined in fuzzy-arithmetic-mapping."
  [f]
  (if-let [new-f (fuzzy-arithmetic-mapping f)]
    new-f
    (if (list? f)
      (let [[lambda args & body] f]
        (if (and (= lambda 'fn) (vector? args))
          `(~lambda ~args ~@(replace-all fuzzy-arithmetic-mapping body))))
      f)))

(defmacro rv-fn
  "Transforms f into its fuzzy arithmetic equivalent, fuzzy-f, and
   calls (fuzzy-f X Y). Uses reflection on the types of X and Y as
   well as any numeric values used in f."
  [f & args]
  `(~(fuzzify-fn f) ~@args))

(defn rv-mean
  "Returns the mean of a FuzzyNumber."
  [X]
  (:mean X))

(defn rv-sum
  "Returns the sum of a sequence of FuzzyNumbers using _+_."
  [Xs]
  (cond (empty? Xs)
        _0_

        (== (count Xs) 1)
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
  (+ (* (marsaglia-normal) (Math/sqrt (:var X))) (:mean X)))

(defn draw-repeatedly
  "Takes a fuzzy number X, and returns an infinite lazy sequence of
   normally-distributed, pseudo-random numbers that match the
   parameters of X, (or a finite sequence of length n, if an integer n
   is provided)."
  ([{:keys [mean var]}]
     (let [sigma (Math/sqrt var)]
       (map #(+ (* sigma %) mean) (repeatedly marsaglia-normal))))
  ([n X]
     (take n (draw-repeatedly X))))
