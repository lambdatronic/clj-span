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
;;; This namespace defines the surface-water model.
;;;

(ns clj-span.models.surface-water
  (:use [clj-misc.utils      :only (seq2map mapmap iterate-while-seq with-message
                                    memoize-by-first-arg angular-distance p & def-
                                    with-progress-bar-cool)]
        [clj-misc.matrix-ops :only (get-neighbors on-bounds? subtract-ids find-nearest filter-matrix-for-coords)]))

(refer 'clj-span.core :only '(distribute-flow! service-carrier with-typed-math-syms))

(def ^:dynamic _0_)
(def ^:dynamic _+_)
(def ^:dynamic *_)
(def ^:dynamic _d)
(def ^:dynamic rv-fn)
(def ^:dynamic _min_)
(def ^:dynamic _>)

(defn lowest-neighbors
  [id in-stream? flow-layers rows cols]
  (if-not (on-bounds? rows cols id)
    (let [elev-layer     (flow-layers "Altitude")
          neighbors      (if (in-stream? id)
                           ;; Step downstream
                           (filter in-stream? (get-neighbors rows cols id))
                           ;; Step downhill
                           (get-neighbors rows cols id))
          local-elev     (get-in elev-layer id)
          neighbor-elevs (map (p get-in elev-layer) neighbors)
          min-elev       (reduce _min_ local-elev neighbor-elevs)]
      (filter #(= min-elev (get-in elev-layer %)) neighbors))))
(def- lowest-neighbors (memoize-by-first-arg lowest-neighbors))

(defn nearest-to-bearing
  [bearing id neighbors]
  (if (seq neighbors)
    (if bearing
      (let [bearing-changes (seq2map neighbors
                                     #(let [bearing-to-neighbor (subtract-ids % id)]
                                        [(angular-distance bearing bearing-to-neighbor)
                                         %]))]
        (bearing-changes (apply min (keys bearing-changes))))
      (first neighbors))))

;; FIXME: Somehow this still doesn't terminate correctly for some carriers.
(defn find-next-step
  [id in-stream? flow-layers rows cols bearing]
  (let [prev-id (if bearing (subtract-ids id bearing))]
    (nearest-to-bearing bearing
                        id
                        (remove (p = prev-id)
                                (lowest-neighbors id
                                                  in-stream?
                                                  flow-layers
                                                  rows
                                                  cols)))))
(def- find-next-step (memoize find-next-step))

(defn handle-use-effects!
  "Computes the amount sunk by each sink encountered along an
   out-of-stream flow path. Reduces the sink-caps for each sink which
   captures some of the service medium. Returns remaining
   actual-weight and the local sink effects."
  [current-id possible-weight actual-weight stream-intakes
   possible-use-caps actual-use-caps cache-layer possible-flow-layer
   actual-flow-layer mm2-per-cell surface-water-carrier]
  (if-let [use-id (stream-intakes current-id)]
    (dosync
     (let [possible-use-cap-ref (possible-use-caps use-id)
           actual-use-cap-ref   (actual-use-caps   use-id)
           possible-use-cap     (deref possible-use-cap-ref)
           actual-use-cap       (deref actual-use-cap-ref)
           [new-possible-weight possible-use]
           (if (= _0_ possible-use-cap)
             [possible-weight _0_]
             (do
               (alter possible-use-cap-ref #(rv-fn '(fn [p u] (max (- u p) 0.0)) possible-weight %))
               [(rv-fn '(fn [p u] (max (- p u) 0.0)) possible-weight possible-use-cap)
                (rv-fn '(fn [p u] (min p u))         possible-weight possible-use-cap)]))
           [new-actual-weight actual-use]
           (if (or (= _0_ actual-use-cap)
                   (= _0_ actual-weight))
             [actual-weight _0_]
             (do
               (alter actual-use-cap-ref #(rv-fn '(fn [a u] (max (- u a) 0.0)) actual-weight %))
               [(rv-fn '(fn [a u] (max (- a u) 0.0)) actual-weight actual-use-cap)
                (rv-fn '(fn [a u] (min a u))         actual-weight actual-use-cap)]))]
       (if (or (not= _0_ possible-use)
               (not= _0_ actual-use))
         (let [possible-benefit (_d possible-use mm2-per-cell)
               actual-benefit   (_d actual-use   mm2-per-cell)]
           (doseq [id (:route surface-water-carrier)]
             (if (not= _0_ possible-benefit)
               (commute (get-in possible-flow-layer id) _+_ possible-benefit))
             (if (not= _0_ actual-benefit)
               (commute (get-in actual-flow-layer id) _+_ actual-benefit)))
           (commute (get-in cache-layer use-id) conj (assoc surface-water-carrier
                                                       :route           nil
                                                       :possible-weight possible-benefit
                                                       :actual-weight   actual-benefit
                                                       :sink-effects    (mapmap identity #(_d % mm2-per-cell)
                                                                                (:sink-effects surface-water-carrier))))))
       [new-possible-weight new-actual-weight]))
    [possible-weight actual-weight]))

(defn handle-sink-effects!
  "Computes the amount sunk by each sink encountered along an
   out-of-stream flow path. Reduces the sink-caps for each sink which
   captures some of the service medium. Returns remaining
   actual-weight and the local sink effects."
  [current-id actual-weight sink-caps]
  (if-let [sink-cap-ref (sink-caps current-id)]
    (dosync
     (let [sink-cap (deref sink-cap-ref)]
       (if (or (= _0_ actual-weight)
               (= _0_ sink-cap))
         [actual-weight {}]
         (do
           (alter sink-cap-ref #(rv-fn '(fn [a s] (max (- s a) 0.0)) actual-weight %))
           [(rv-fn '(fn [a s] (max (- a s) 0.0)) actual-weight sink-cap)
            {current-id (rv-fn '(fn [a s] (min a s)) actual-weight sink-cap)}]))))
    [actual-weight {}]))

;; FIXME: Make sure carriers can hop from stream to stream as necessary.
(defn to-the-ocean!
  "Computes the state of the surface-water-carrier after it takes
   another step downhill.  If it encounters a sink location, it drops
   some water according to the remaining sink capacity at this
   location."
  [{:keys [cache-layer possible-flow-layer actual-flow-layer sink-caps possible-use-caps actual-use-caps
           in-stream? stream-intakes mm2-per-cell trans-threshold-volume flow-layers rows cols] :as params}
   {:keys [route possible-weight actual-weight sink-effects stream-bound?] :as surface-water-carrier}]
  (let [current-id (peek route)
        prev-id    (peek (pop route))
        bearing    (if prev-id (subtract-ids current-id prev-id))]
    ;; (dosync
    ;;  (alter (get-in possible-flow-layer current-id) _+_ (_d possible-weight mm2-per-cell))
    ;;  (alter (get-in actual-flow-layer   current-id) _+_ (_d actual-weight   mm2-per-cell)))
    (if stream-bound?
      (let [[new-possible-weight new-actual-weight] (handle-use-effects! current-id
                                                                         possible-weight
                                                                         actual-weight
                                                                         stream-intakes
                                                                         possible-use-caps
                                                                         actual-use-caps
                                                                         cache-layer
                                                                         possible-flow-layer
                                                                         actual-flow-layer
                                                                         mm2-per-cell
                                                                         surface-water-carrier)]
        (if (_> new-possible-weight trans-threshold-volume)
          (if-let [next-id (find-next-step current-id in-stream? flow-layers rows cols bearing)]
            (assoc surface-water-carrier
              :route           (conj route next-id)
              :possible-weight new-possible-weight
              :actual-weight   new-actual-weight))))
      (let [[new-actual-weight new-sink-effects] (handle-sink-effects! current-id
                                                                       actual-weight
                                                                       sink-caps)]
        (if-let [next-id (find-next-step current-id in-stream? flow-layers rows cols bearing)]
          (assoc surface-water-carrier
            :route           (conj route next-id)
            :actual-weight   new-actual-weight
            :sink-effects    (merge-with _+_ sink-effects new-sink-effects)
            :stream-bound?   (in-stream? next-id)))))))

(defn report-carrier-counts
  [surface-water-carriers]
  (let [on-land-carriers   (count (remove :stream-bound? surface-water-carriers))
        in-stream-carriers (- (count surface-water-carriers) on-land-carriers)]
    (printf "Carriers: %10d | On Land: %10d | In Stream: %10d%n"
            (+ on-land-carriers in-stream-carriers)
            on-land-carriers
            in-stream-carriers)))

(defn move-carriers-one-step-downstream
  [params surface-water-carriers]
  (report-carrier-counts surface-water-carriers)
  (pmap (p to-the-ocean! params) surface-water-carriers))

(defn create-initial-service-carriers
  [{:keys [source-layer source-points mm2-per-cell in-stream?]}]
  (map
   #(let [source-weight (*_ mm2-per-cell (get-in source-layer %))]
      (struct-map service-carrier
        :source-id       %
        :route           [%]
        :possible-weight source-weight
        :actual-weight   source-weight
        :sink-effects    {}
        :stream-bound?   (in-stream? %)))
   source-points))

(defn stop-unless-reducing
  [n coll]
  (dorun (take-while (fn [[p c]] (> p c)) (partition 2 1 (map count (take-nth n coll))))))

(defn propagate-runoff!
  "Constructs a sequence of surface-water-carrier objects (one per
   source point) and then iteratively propagates them downhill until
   they reach a stream location, get stuck in a low elevation point,
   or fall off the map bounds.  Once they reach a stream location, the
   carriers will attempt to continue downhill while staying in a
   stream course.  Sinks affect carriers overland.  Users affect
   carriers in stream channels.  All the carriers are moved together
   in timesteps (more or less)."
  [params]
  (with-message "Moving the surface water carriers downhill and downstream...\n" "All done."
    (stop-unless-reducing
     100
     (iterate-while-seq
      (p move-carriers-one-step-downstream params)
      (create-initial-service-carriers params)))))

(defn find-nearest-stream-point!
  [in-stream? claimed-intakes rows cols id]
  (dosync
   (let [available-intake? (complement @claimed-intakes)
         stream-point      (find-nearest #(and (in-stream? %) (available-intake? %)) rows cols id)]
     (if stream-point (alter claimed-intakes conj [stream-point id])))))

;; FIXME: Try shuffling the (remove in-stream? use-points) line to reduce transaction clashes.
(defn find-nearest-stream-points
  [in-stream? rows cols use-points]
  (with-message
    "Finding nearest stream points to all users...\n"
    #(str "\nDone. [Claimed intakes: " (count %) "]")
    (let [in-stream-users (filter in-stream? use-points)
          claimed-intakes (ref (zipmap in-stream-users in-stream-users))]
      (println "Detected" (count in-stream-users) "in-stream users.\nContinuing with out-of-stream users...")
      (with-progress-bar-cool
        :drop
        (- (count use-points) (count in-stream-users))
        (pmap (p find-nearest-stream-point! in-stream? claimed-intakes rows cols)
              (remove in-stream? use-points)))
      @claimed-intakes)))

(defn link-streams-to-users
  "Stores a map of {stream-ids -> nearest-use-ids} under (params :stream-intakes)."
  [{:keys [rows cols use-points in-stream?] :as params}]
  (assoc params
    :stream-intakes (find-nearest-stream-points in-stream? rows cols use-points)))

(defn make-buckets
  "Stores maps from {ids -> mm3-ref} for sink, possible-use, and actual-use in params."
  [{:keys [sink-layer sink-points use-layer use-points mm2-per-cell] :as params}]
  (assoc params
    :sink-caps         (seq2map sink-points (fn [id] [id (ref (*_ mm2-per-cell (get-in sink-layer id)))]))
    :possible-use-caps (seq2map use-points  (fn [id] [id (ref (*_ mm2-per-cell (get-in use-layer  id)))]))
    :actual-use-caps   (seq2map use-points  (fn [id] [id (ref (*_ mm2-per-cell (get-in use-layer  id)))]))))

(defn create-in-stream-test
  "Stores a set of all in-stream ids under (params :in-stream?)."
  [{:keys [flow-layers] :as params}]
  (assoc params
    :in-stream? (set (filter-matrix-for-coords #(not= _0_ %) (flow-layers "River")))))

(defn compute-trans-threshold-volume
  "Stores trans-threshold * mm2-per-cell under (params :trans-threshold-volume)."
  [{:keys [trans-threshold mm2-per-cell] :as params}]
  (assoc params
    :trans-threshold-volume (* trans-threshold mm2-per-cell)))

(defn compute-mm2-per-cell
  "Stores cell-width * cell-height * 10^6 under (params :mm2-per-cell)."
  [{:keys [cell-width cell-height] :as params}]
  (assoc params
    :mm2-per-cell (* cell-width cell-height (Math/pow 10.0 6.0))))

(defmethod distribute-flow! "SurfaceWaterMovement"
  [{:keys [source-layer sink-layer use-layer flow-layers
           cache-layer possible-flow-layer actual-flow-layer
           source-points sink-points use-points
           cell-width cell-height rows cols
           value-type trans-threshold]
    :as params}]
  (with-typed-math-syms value-type [_0_ _+_ *_ _d rv-fn _min_ _>]
    (-> params
        compute-mm2-per-cell
        compute-trans-threshold-volume
        create-in-stream-test
        make-buckets
        link-streams-to-users
        propagate-runoff!)))
