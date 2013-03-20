;;; Copyright 2010-2013 Gary Johnson
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
;;; This namespace defines the line-of-sight model.
;;;
;;; * Routes run from Source to Use
;;;
;;; * Contain positive utility values (total decayed source values)
;;;
;;; * Projects a line of sight from each user to source point
;;;
;;; * Weights utility by amount of visible height of source above view
;;;   line
;;;
;;; * Sink effects are computed as the decayed negative utility (sink
;;;   value) received from any sink location along a view path to a
;;;   source point.

(ns clj-span.models.line-of-sight
  (:use [clj-misc.utils      :only (euclidean-distance-2 p def- between? with-progress-bar-cool with-message)]
        [clj-misc.matrix-ops :only (find-line-between get-line-fn)]
        [clj-span.thinklab-monitor :only (monitor-info with-interrupt-checking)])
  (:require [clojure.core.reducers :as r])
  (:import (org.integratedmodelling.thinklab.api.listeners IMonitor)))

(refer 'clj-span.core :only '(distribute-flow! service-carrier with-typed-math-syms))

(def ^:dynamic _0_)
(def ^:dynamic _+_)
(def ^:dynamic _-_)
(def ^:dynamic _*_)
(def ^:dynamic _d_)
(def ^:dynamic _*)
(def ^:dynamic *_)
(def ^:dynamic _d)
(def ^:dynamic _-)
(def ^:dynamic -_)
(def ^:dynamic _>_)
(def ^:dynamic _<_)
(def ^:dynamic _max_)
(def ^:dynamic rv-fn)
(def ^:dynamic _>)

;; in meters
(def max-source-view-distance 50000.0) ;; 50km
(def max-sink-view-distance    2000.0) ;; 2km

(defn source-decay [^double distance] (- 1.0 (Math/pow (/ distance max-source-view-distance) 2)))
(defn sink-decay   [^double distance] (- 1.0 (Math/pow (/ distance max-sink-view-distance)   2)))

(defn compute-view-impact
  [scenic-value scenic-elev use-elev slope distance water-present?]
  (let [projected-elev (rv-fn '(fn [e r] (max 0.0 (+ e r))) use-elev (_* slope distance))]
    (if (and water-present? (_<_ slope _0_))
      scenic-value
      (let [visible-fraction (-_ 1.0 (rv-fn '(fn [p s] (if (< p s) (/ p s) 1.0))
                                            projected-elev
                                            scenic-elev))]
        (_*_ scenic-value visible-fraction)))))

(defn compute-sink-effects
  [sink-layer filtered-sight-line use-elev]
  (persistent!
   (reduce
    (fn [acc [point elevation distance slope]]
      (let [sink-value (get-in sink-layer point)]
        (if (not= _0_ sink-value)
          (let [view-impact (compute-view-impact sink-value elevation use-elev slope distance nil)]
            (if (not= _0_ view-impact)
              (assoc! acc point (*_ view-impact (sink-decay distance)))
              acc))
          acc)))
    (transient {})
    filtered-sight-line)))

(defn prune-hidden-points
  [starting-elev starting-slope elev-layer use-elev use-loc-in-m to-meters sight-line-segment]
  (persistent!
   (reduce
    (fn [{:keys [max-elev max-slope filtered-line] :as acc} point]
      (let [curr-elev (get-in elev-layer point)]
        (if (or (nil? max-elev)
                (_>_ curr-elev max-elev))
          (let [distance   (euclidean-distance-2 use-loc-in-m (to-meters point))
                curr-slope (_d (_-_ curr-elev use-elev) distance)]
            (if (_>_ curr-slope max-slope)
              (assoc! acc
                      :max-elev      (if (nil? max-elev) max-elev curr-elev)
                      :max-slope     curr-slope
                      :filtered-line (conj filtered-line [point curr-elev distance max-slope]))
              acc))
          acc)))
    (transient {:max-elev      starting-elev
                :max-slope     starting-slope
                :filtered-line []})
    sight-line-segment)))

(defn split-sight-line
  [elev-layer sight-line]
  (let [[initial-view-space sight-line-remainder] (split-with (fn [[pointA pointB]]
                                                                (not (_>_ (get-in elev-layer pointB) (get-in elev-layer pointA))))
                                                              (partition 2 1 sight-line))]
    [(map second initial-view-space)
     (map second sight-line-remainder)]))

(defn filter-sight-line
  "Returns a sequence of 4-tuples of [point elevation distance slope] for all points which can be seen along the sight-line."
  [sight-line elev-layer use-elev use-loc-in-m to-meters]
  (let [[initial-view-space sight-line-remainder] (split-sight-line elev-layer sight-line)
        initial-view-slope (_- (_d (_-_ (get-in elev-layer (second sight-line)) use-elev)
                                   (euclidean-distance-2 use-loc-in-m (to-meters (second sight-line))))
                               0.01) ;; epsilon to include the first step
        first-segment      (prune-hidden-points nil ;; disregard the increasing elevation constraint
                                                initial-view-slope
                                                elev-layer
                                                use-elev
                                                use-loc-in-m
                                                to-meters
                                                initial-view-space)
        second-segment     (prune-hidden-points (get-in elev-layer (or (last initial-view-space) (first sight-line)))
                                                (:max-slope first-segment)
                                                elev-layer
                                                use-elev
                                                use-loc-in-m
                                                to-meters
                                                sight-line-remainder)]
    (concat (:filtered-line first-segment) (:filtered-line second-segment))))

(defn raycast!
  "Finds a line of sight path between source and use points, checks
   for obstructions, and determines (using elevation info) how much of
   the source element can be seen from the use point.  A distance
   decay function is applied to the results to compute the visual
   utility originating from the source point."
  [{:keys [source-layer sink-layer elev-layer water-layer cache-layer
           possible-flow-layer actual-flow-layer to-meters trans-threshold monitor]}
   [source-point use-point source-loc-in-m use-loc-in-m distance-decay]]
  (with-interrupt-checking ^IMonitor monitor
    (let [use-elev            (get-in elev-layer use-point)
          filtered-sight-line (filter-sight-line (find-line-between use-point source-point)
                                                 elev-layer
                                                 use-elev
                                                 use-loc-in-m
                                                 to-meters)
          [final-point final-elev final-distance final-slope] (last filtered-sight-line)]
      (if (= final-point source-point) ;; source point is visible from use point
        (let [possible-weight (*_ distance-decay
                                  (compute-view-impact (get-in source-layer source-point)
                                                       final-elev
                                                       use-elev
                                                       final-slope
                                                       final-distance
                                                       (and water-layer (not= _0_ (get-in water-layer source-point)))))]
          (when (_> possible-weight trans-threshold)
            ;; FIXME: Consing up memory for the sink-effects map.
            ;;        Store this in a tripartite graph instead.
            (let [sink-effects  (let [sink-effects (compute-sink-effects sink-layer filtered-sight-line use-elev)
                                      sink-value (get-in sink-layer use-point)]
                                  (if (not= _0_ sink-value)
                                    (assoc sink-effects use-point sink-value)
                                    sink-effects))
                  actual-weight (rv-fn '(fn [p s] (max 0.0 (- p s))) possible-weight (reduce _+_ _0_ (vals sink-effects)))
                  carrier       (struct-map service-carrier
                                  :source-id       source-point
                                  ;; :route           (bitpack-route (reverse (cons use-point (map first filtered-sight-line))))
                                  :possible-weight possible-weight
                                  :actual-weight   actual-weight
                                  :sink-effects    sink-effects)]
              (dosync
               (doseq [id (cons use-point (map first filtered-sight-line))]
                 (commute (get-in possible-flow-layer id) _+_ possible-weight)
                 (if (not= _0_ actual-weight)
                   (commute (get-in actual-flow-layer id) _+_ actual-weight)))
               ;; FIXME: Conjing the new carrier onto the cache layer.
               ;;        Eliminate this cache-layer thing and replace
               ;;        it with a tripartite graph.
               (commute (get-in cache-layer use-point) conj carrier)))))))))

(defn select-in-range-views
  [use-points source-points to-meters]
  (filterv identity
           (for [use-point use-points source-point source-points]
             (when (not= source-point use-point) ;; no in-situ use
               (let [use-loc-in-m    (to-meters use-point)
                     source-loc-in-m (to-meters source-point)
                     distance-decay  (source-decay
                                      (euclidean-distance-2 use-loc-in-m source-loc-in-m))]
                 (if (pos? distance-decay) ;; we are in potential sight range
                   [source-point use-point source-loc-in-m use-loc-in-m distance-decay]))))))

(defmethod distribute-flow! "LineOfSight"
  [{:keys [flow-layers source-points use-points cell-width cell-height value-type monitor]
    :as params}]
  (let [num-view-lines (* (count source-points) (count use-points))
        to-meters      (fn [[i j]] [(* i cell-height) (* j cell-width)])]
    (monitor-info monitor (str "scanning " num-view-lines " possible view lines"))
    (with-message (str "Scanning " num-view-lines " possible view lines...\n") "\nAll done."
      (with-typed-math-syms value-type [_0_ _+_ _-_ _*_ _d_ _* *_ _d _- -_ _>_ _<_ _max_ rv-fn _>]
        (r/foldcat
         (r/map (p raycast! (assoc params
                              :elev-layer  (flow-layers "Altitude")
                              :water-layer (flow-layers "WaterBodies")
                              :to-meters   (memoize to-meters)))
                (select-in-range-views use-points source-points to-meters)))
        (monitor-info monitor (str "completed LineOfSight simulation successfully"))))))
