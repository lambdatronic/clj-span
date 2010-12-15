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
;;; This namespace defines the coastal-storm-protection model.
;;;

(ns clj-span.coastal-storm-protection-model
  (:use [clj-span.model-api  :only (distribute-flow service-carrier)]
        [clj-misc.utils      :only (seq2map p & angular-distance)]
        [clj-misc.matrix-ops :only (get-rows
                                    get-cols
                                    make-matrix
                                    map-matrix
                                    subtract-ids
                                    filter-matrix-for-coords
                                    get-neighbors
                                    find-point-at-dist-in-m
                                    find-line-between)]
        [clj-misc.randvars   :only (_0_)]))

(def storm-to-wave-orientations
     {[ 0  1] [ 1  0]
      [ 1  1] [ 1 -1]
      [ 1  0] [ 0 -1]
      [ 1 -1] [-1 -1]
      [ 0 -1] [-1  0]
      [-1 -1] [-1  1]
      [-1  0] [ 0  1]
      [-1  1] [ 1  1]})

(defn find-wave-line
  [storm-source-point storm-orientation wave-width cell-width cell-height]
  (let [wave-orientation (storm-to-wave-orientations storm-orientation) ;; 90 degrees left of storm-orientation
        wave-reach       (/ wave-width 2)
        wave-left-edge   (find-point-at-dist-in-m storm-source-point wave-orientation         wave-reach cell-width cell-height)
        wave-right-edge  (find-point-at-dist-in-m storm-source-point (map - wave-orientation) wave-reach cell-width cell-height)]
    (find-line-between wave-left-edge wave-right-edge)))

(defn get-storm-orientation
  [on-track? rows cols id current-orientation]
  (if-let [on-track-neighbors (seq (filter on-track? (get-neighbors rows cols id)))]
    (let [orientation-deltas (seq2map (map #(subtract-ids % id) on-track-neighbors)
                                      (fn [neighbor-orientation]
                                        [(angular-distance current-orientation neighbor-orientation) neighbor-orientation]))]
      (orientation-deltas (apply min (keys orientation-deltas))))))

;; FIXME: make sure all the right layers are being passed in with these concept names
;; FIXME: need to get the cell-dims in here
;; FIXME: find a way to specify the wave width and initial storm direction
(defmethod distribute-flow "CoastalStormMovement"
  [_ source-layer sink-layer use-layer
   {storm-tracks-layer "StormTracks", geomorphologic-sink-layer "GeomorphologicSink"}]
  (println "Running Coastal Storm Protection flow model.")
  (let [rows        (get-rows source-layer)
        cols        (get-cols source-layer)
        cell-width  100
        cell-height 100
        cache-layer (make-matrix rows cols (fn [_] (ref ())))
        [source-points sink-points use-points] (pmap (p filter-matrix-for-coords (p not= _0_))
                                                     [source-layer sink-layer use-layer])]
    (println "Source points:" (count source-points))
    (println "Sink points:  " (count sink-points))
    (println "Use points:   " (count use-points))
    ;; [X] Lookup the storm name.
    ;; [X] Create a function to determine the wave's new orientation.
    ;; [X] Discover the storm direction.
    ;; [X] Project a swath of carriers 100km wide perpendicular to the storm direction.
    ;; [ ] Deplete all sinks that the wave intersects.
    ;; [ ] Move the carriers together to their new positions along the wavefront and repeat the sink depletion process.
    ;; [ ] If users are encountered, store a carrier on the user and keep going (non-rival use).
    ;; [ ] If a carrier's possible-weight falls below the threshold, stop the carrier.
    ;; [ ] Exit when all carriers have finished moving.
    (let [storm-source-point (first source-points) ;; we are only going to use one source point in this model
          storm-name         (get-in storm-tracks-layer storm-source-point)
          on-track?          #(= storm-name (get-in storm-tracks-layer %))
          storm-orientation  (get-storm-orientation on-track? rows cols storm-source-point [0 -1]) ;; start the storm to the west
          wave-line          (find-wave-line storm-source-point storm-orientation 100000 cell-width cell-height) ;; wave width = 100km
          wave-height        (get-in source-layer storm-source-point)
          storm-carriers     (map #(struct-map service-carrier
                                     :source-id       %
                                     :route           [%]
                                     :possible-weight wave-height
                                     :actual-weight   wave-height
                                     :sink-effects    {})
                                  wave-line)
          sink-caps          (seq2map sink-points (fn [id] [id (ref (get-in sink-layer id))]))]
      ;; Haven't used geomorphologic-sink-layer, use-points, sink-caps, storm-carriers
      ;;(flow-wave! ?)
      (println "Simulation complete. Returning the cache-layer.")
      (map-matrix (& seq deref) cache-layer))))
