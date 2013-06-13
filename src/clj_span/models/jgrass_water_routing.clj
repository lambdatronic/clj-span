(ns clj-span.models.jgrass-water-routing
  (:use [clj-misc.utils      :only [magnitude-2]]
        [clj-misc.matrix-ops :only [get-rows get-cols make-matrix transpose-matrix matrix2seq on-bounds? subtract-ids] :exclude [get-neighbors]]))

(def compass-to-offset ;; [dy dx]
  {:N  [ 1  0]
   :E  [ 0  1]
   :S  [-1  0]
   :W  [ 0 -1]
   :NE [ 1  1]
   :SE [-1  1]
   :SW [-1 -1]
   :NW [ 1 -1]})

(def offset-to-code ;; 1 to 8 counterclockwise from :N
  {[ 1  0] 1
   [ 0  1] 7
   [-1  0] 5
   [ 0 -1] 3
   [ 1  1] 8
   [-1  1] 6
   [-1 -1] 4
   [ 1 -1] 2})

(defn get-neighbors
  "Returns a sequence of [y x] pairs for the selected neighbors of the input point."
  [[y x] init-dir order selection]
  {:pre [(every? integer? [y x])
         (contains? #{:N :E :S :W :NE :SE :SW :NW} init-dir)
         (contains? #{:clockwise :counterclockwise} order)
         (contains? #{:cardinal :diagonal :all} selection)]}
  (let [all-neighbors      (if (= order :clockwise)
                             (take 8 (drop-while #(not= init-dir %) (cycle [:N :NE :E :SE :S :SW :W :NW])))
                             (take 8 (drop-while #(not= init-dir %) (cycle [:N :NW :W :SW :S :SE :E :NE]))))
        filtered-neighbors (case selection
                             :cardinal (filter #{:N :E :S :W} all-neighbors)
                             :diagonal (filter #{:NE :SE :SW :NW} all-neighbors)
                             :all      all-neighbors)]
    (map (comp (fn [[dy dx]] [(+ y dy) (+ x dx)])
               compass-to-offset)
         filtered-neighbors)))

;; FIXME: I need to prevent opposing flow dirs.
(defn dir-to-lowest-neighbor
  "This function sets the value at dir[j i] to be an integer 1-8,
  which is the direction to the most downhill neighbor (using the
  metric slope) with a preference for cardinal directions over
  diagonal ones. A value of 0 indicates that I am the lowest of my
  neighbors. A value of -1 indicates that this value cannot be
  reliably calculated because of missing elevation data."
  [cell-idx cardinal-runs diagonal-runs elevations]
  (let [this-elevation     (get-in elevations cell-idx)
        cardinal-neighbors (get-neighbors cell-idx :N :counterclockwise :cardinal)
        diagonal-neighbors (get-neighbors cell-idx :N :counterclockwise :diagonal)
        lowest-cardinal-neighbor (apply max-key :slope (map (fn [neighbor run]
                                                              (let [rise  (- this-elevation (get-in elevations neighbor))
                                                                    slope (/ rise run)]
                                                                {:id neighbor :slope slope}))
                                                            cardinal-neighbors
                                                            cardinal-runs))
        lowest-diagonal-neighbor (apply max-key :slope (map (fn [neighbor run]
                                                              (let [rise  (- this-elevation (get-in elevations neighbor))
                                                                    slope (/ rise run)]
                                                                {:id neighbor :slope slope}))
                                                            diagonal-neighbors
                                                            diagonal-runs))]
    (cond (and (neg? (:slope lowest-cardinal-neighbor))
               (neg? (:slope lowest-diagonal-neighbor)))
          ;; cell-idx is a pit
          0

          (== (:slope lowest-cardinal-neighbor)
              (:slope lowest-diagonal-neighbor))
          ;; we prefer the cardinal neighbor over the diagonal one
          (offset-to-code (subtract-ids (:id lowest-cardinal-neighbor) cell-idx))

          :else
          ;; we take the neighbor with the greatest slope
          (offset-to-code (subtract-ids (:id (max-key :slope lowest-cardinal-neighbor lowest-diagonal-neighbor)) cell-idx)))))

;; (defn darea
;;   "This function sets arr[j i] to -1 on boundaries and in cells where
;;   dir[j i] is also -1. It then sets arr[j i] to 1 everywhere else.
;;   Then it scans through all of this cell's neighbors. If any of them
;;   are on the map bounds or have a -1 for their dir value, then arr[j
;;   i] is set to -1. It then recursively calls itself for each
;;   neighboring cell which drains into it (according to the dir matrix)
;;   to make sure the arr values are set for all upstream values prior to
;;   calculating this one. Finally, it assigns arr[j i] to be the number
;;   of upstream cells in its watershed."
;;   [i j]
;;   nil)

(defn setdfnoflood [rows cols cell-width cell-height elevations]
  (let [cardinal-runs (map (fn [[dy dx]] (magnitude-2 [(* dy cell-height)
                                                       (* dx cell-width)]))
                           (get-neighbors [0 0] :N :counterclockwise :cardinal))
        diagonal-runs (map (fn [[dy dx]] (magnitude-2 [(* dy cell-height)
                                                       (* dx cell-width)]))
                           (get-neighbors [0 0] :N :counterclockwise :diagonal))
        dir           (make-matrix (get-rows elevations)
                                   (get-cols elevations)
                                   (fn [cell-idx]
                                     (if (or (on-bounds? cell-idx)
                                             (nil? (get-in elevations cell-idx)))
                                       -1
                                       (dir-to-lowest-neighbor cell-idx cardinal-runs diagonal-runs elevations))))
        n             (count (filter zero? (matrix2seq dir)))
        arr           (make-matrix rows cols (constantly 0))]
    ;; Resume on line 378
    ))

;; This function takes a pit-layer, which is a 2D matrix of doubles.
;; No-data in any cell is indicated by a nil value. The elevations
;; matrix is a transposed version of pit-layer.
(defn safe-get-values [pit-layer cell-width cell-height]
  (let [rows       (get-rows pit-layer)
        cols       (get-cols pit-layer)
        elevations (transpose-matrix pit-layer)]
    (transpose-matrix
     (setdfnoflood rows cols cell-width cell-height elevations))))
