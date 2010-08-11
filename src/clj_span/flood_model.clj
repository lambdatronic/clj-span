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
;;; This namespace defines the flood model.

(ns clj-span.flood-model
  (:use [clj-span.params         :only (*trans-threshold*)]
        [clj-span.model-api      :only (distribute-flow service-carrier)]
        [clj-misc.utils          :only (mapmap seq2map seq2redundant-map euclidean-distance square-distance def- p &)]
        [clj-misc.matrix-ops     :only (get-rows get-cols make-matrix filter-matrix-for-coords in-bounds? find-line-between map-matrix)]
        [clj-misc.randvars       :only (_0_ _+_ _-_ _*_ _d_ -_ _* _d rv-min rv-mean rv-cdf-lookup)]
        [clj-span.sediment-model :only (hydrosheds-delta-codes)]))

(defn- move-points-into-stream-channel
  "Returns a map of in-stream-ids to lists of the out-of-stream-ids
   that were shifted into this position.  If the location's downstream
   path leads it beyond the map bounds or into the ocean or an inland
   sink prior to reaching a stream, then it will be left out of the
   returned results."
  [step-downstream in-stream? data-points]
  (dissoc (seq2redundant-map data-points
                             (fn [id] [(my->> id
                                              (iterate step-downstream)
                                              (take-while (& not nil?))
                                              (filter in-stream?)
                                              first
                                              vec)
                                       id])
                             conj)
          []))

(defn- flood-activation-factors
  "Returns a map of each data-id (e.g. a sink or use location) to a
   number between 0.0 and 1.0, representing its relative position
   between the stream edge (1.0) and the floodplain boundary (0.0).
   Any data-ids whose elevations are above the projected elevation
   from the stream to the floodplain boundary are left out of this map
   and should be considered to have an activation-factor of 0.0."
  [in-floodplain? elevation-layer in-stream-map]
  (into {}
        (remove nil?
                (for [in-stream-id (keys in-stream-map) data-id (in-stream-map in-stream-id)]
                  (if (= in-stream-id data-id)
                    ;; location is already in-stream, activation is 100%
                    [data-id 1.0]
                    ;; location is out-of-stream, activation is scaled by the
                    ;; relative elevation difference between this location,
                    ;; the in-stream proxy location, and the nearest
                    ;; floodplain boundary
                    (let [loc-delta       (map - data-id in-stream-id)
                          outside-id      (first (filter (& not in-floodplain?)
                                                         (rest (iterate (p map + loc-delta) data-id))))
                          inside-id       (map - outside-id loc-delta)
                          boundary-id     (first (filter (& not in-floodplain?)
                                                         (find-line-between inside-id outside-id)))
                          rise            (_-_ (get-in elevation-layer boundary-id)
                                               (get-in elevation-layer in-stream-id))
                          run-to-boundary (euclidean-distance in-stream-id boundary-id)
                          run-to-data     (euclidean-distance in-stream-id data-id)
                          slope           (_d rise run-to-boundary)
                          elev-limit      (_+_ (_* slope run-to-data) (get-in elevation-layer in-stream-id))
                          elev-at-loc     (get-in elevation-layer data-id)]
                      (if (rv-lt elev-at-loc elev-limit)
                        [data-id (- 1.0 (/ run-to-data run-to-boundary))])))))))

(defn local-sink-effects
  "FIXME: Something is not right with my computation of the sink-cap
   upper bounds."
  [sink-caps affected-sinks actual-weight]
  (if (seq affected-sinks)
    (seq2map affected-sinks
             (let [total-sink-cap (reduce _+_ (map (& deref sink-caps) affected-sinks))]
               (if (pos? (rv-gt actual-weight total-sink-cap))
                 #(let [sink-cap @(sink-caps %)] [% sink-cap])
                 (let [source-sink-ratio (_d_ actual-weight total-sink-cap)]
                   #(let [sink-cap @(sink-caps %)] [% (_*_ sink-cap source-sink-ratio)])))))))

(defn- step-downstream!
  "Computes the state of the flood-carrier after it takes another step
   downstream.  If it encounters a sink location, it drops some water
   according to the remaining sink capacity at this location.  If it
   encounters a use location, a service-carrier is stored in the
   user's carrier-cache."
  [cache-layer step-downstream in-stream? sink-map use-map sink-AFs use-AFs sink-caps
   {:keys [source-id route possible-weight actual-weight sink-effects stream-bound?]}]
  ;;[current-id source-ids source-fractions incoming-utilities sink-effects]
  (when-let [new-id (step-downstream (peek route))]
    (if stream-bound?
      (let [unsaturated-sink?  (& (p not= _0_) deref sink-caps)
            affected-sinks     (filter unsaturated-sink? (sink-map new-id))
            sink-effects       (merge sink-effects (local-sink-effects sink-caps affected-sinks actual-weight))
            ;; RESUME CODING HERE.
            outgoing-utilities (if (seq affected-sinks) ; sinks encountered
                                 (do
                                   (doseq [sink-id affected-sinks]
                                     (swap! (sink-caps sink-id) _-_ (sink-effects sink-id)))
                                   (let [total-sunk   (reduce _+_ (map sink-effects affected-sinks))
                                         out-fraction (-_ 1 (_d_ total-sunk total-incoming))]
                                     (map (p _*_ out-fraction) incoming-utilities)))
                                 incoming-utilities)]
        (doseq [cache (map (p get-in cache-layer) (if (seq affected-sinks) (use-map new-id)))]
          (swap! cache concat
                 (map (fn [sid sfrac utility]
                        (struct-map service-carrier
                          :source-id       sid
                          :route           nil
                          :possible-weight _0_
                          :actual-weight   utility
                          :sink-effects    (mapmap identity (p _*_ sfrac) sink-effects)))
                      source-ids
                      source-fractions
                      incoming-utilities)))
        (when (< (rv-cdf-lookup (reduce _+_ _0_ outgoing-utilities) *trans-threshold*) 0.5)
          [new-id source-ids source-fractions outgoing-utilities sink-effects]))))

(defn- distribute-downstream!
  "Constructs a sequence of flood-carrier objects (one per source
   point) and then iteratively computes the next-step downstream
   flood-carriers from the previous until they no longer have any
   water, fall off the map bounds, or hit an inland sink.  All the
   carriers are moved together in timesteps (more or less)."
  [cache-layer step-downstream in-stream? source-layer
   source-points sink-map use-map sink-AFs use-AFs sink-caps]
  (println "Moving the flood-carriers downstream...")
  (doseq [_ (take-while seq (iterate
                             (fn [flood-carriers]
                               (remove nil?
                                       (pmap (p step-downstream!
                                                cache-layer
                                                step-downstream
                                                in-stream?
                                                sink-map
                                                use-map
                                                sink-AFs
                                                use-AFs
                                                sink-caps)
                                             flood-carriers)))
                             (map
                              #(let [source-weight (get-in source-layer %)]
                                 (struct-map service-carrier
                                   :source-id       %
                                   :route           [%]
                                   :possible-weight source-weight
                                   :actual-weight   source-weight
                                   :sink-effects    {}
                                   :stream-bound?   false))
                              source-points)))]
    (print "*") (flush))
  (println "\nAll done."))

(defmethod distribute-flow "Flood"
  [_ source-layer sink-layer use-layer
   {hydrosheds-layer "Hydrosheds", stream-layer "RiverStream",
    floodplain-layer "FloodPlainPresence", elevation-layer "Altitude"}]
  (println "Running Flood flow model.")
  (let [rows          (get-rows source-layer)
        cols          (get-cols source-layer)
        cache-layer   (make-matrix rows cols (constantly (atom ())))
        [source-points sink-points use-points] (pmap (p filter-matrix-for-coords (p not= _0_))
                                                     [source-layer sink-layer use-layer])]
    (println "Source points:" (count source-points))
    (println "Sink points:  " (count sink-points))
    (println "Use points:   " (count use-points))
    (let [flow-delta         (fn [id] (hydrosheds-delta-codes (get-in hydrosheds-layer id)))
          step-downstream    (fn [id] (if-let [dir (flow-delta id)] ; if nil, we've hit 0.0 (ocean) or -1.0 (inland sink)
                                        (let [next-id (map + id dir)]
                                          (if (in-bounds? rows cols next-id)
                                            next-id))))
          in-stream?         (fn [id] (not= _0_ (get-in stream-layer id)))
          in-floodplain?     (fn [id] (not= _0_ (get-in floodplain-layer id)))
          [sink-map use-map] (do (println "Shifting sink and use points into the nearest stream channel...")
                                 (time (pmap (p move-points-into-stream-channel step-downstream in-stream?)
                                             [sink-points use-points])))
          [sink-AFs use-AFs] (do (println "Computing sink and use flood-activation-factors...")
                                 (time (pmap (p flood-activation-factors in-floodplain? elevation-layer)
                                             [sink-map use-map])))
          sink-caps          (seq2map sink-points (fn [id] [id (atom (get-in sink-layer id))]))]
      (time (distribute-downstream! cache-layer step-downstream in-stream? source-layer
                                    source-points sink-map use-map sink-AFs use-AFs sink-caps))
      (println "Simulation complete. Returning the cache-layer.")
      (map-matrix (& seq deref) cache-layer))))
