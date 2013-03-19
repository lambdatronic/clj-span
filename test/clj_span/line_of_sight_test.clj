(ns clj-span.line-of-sight-test
  (:use clojure.test
        clj-span.core
        clj-span.models.line-of-sight :reload-all
        clj-misc.utils
        clj-misc.matrix-ops))

(def value-type :numbers)

(defn register-math-syms [t]
  (with-typed-math-syms value-type [_0_ _+_ _-_ _*_ _d_ _* *_ _d _- -_ _>_ _<_ _max_ rv-fn _>]
    (t)))

(use-fixtures :once register-math-syms)

(def source-layer
  [[0.0 100.0 0.0 0.0 0.0 100.0 0.0   0.0   0.0 0.0]
   [0.0   0.0 0.0 0.0 0.0   0.0 0.0   0.0   0.0 0.0]
   [0.0   0.0 0.0 0.0 0.0   0.0 0.0   0.0   0.0 0.0]
   [0.0   0.0 0.0 0.0 0.0   0.0 0.0 100.0   0.0 0.0]
   [0.0   0.0 0.0 0.0 0.0   0.0 0.0   0.0   0.0 0.0]
   [0.0   0.0 0.0 0.0 0.0   0.0 0.0   0.0   0.0 0.0]
   [0.0   0.0 0.0 0.0 0.0   0.0 0.0   0.0   0.0 0.0]
   [0.0   0.0 0.0 0.0 0.0   0.0 0.0   0.0   0.0 0.0]
   [0.0   0.0 0.0 0.0 0.0   0.0 0.0   0.0 100.0 0.0]
   [0.0   0.0 0.0 0.0 0.0   0.0 0.0   0.0   0.0 0.0]])

(def sink-layer
  [[0.0 0.0 0.0 0.0 0.0  0.0   0.0 0.0 0.0 0.0]
   [0.0 0.0 0.0 0.0 0.0  0.0   0.0 0.0 0.0 0.0]
   [0.0 0.0 0.0 0.0 0.0  0.0   0.0 0.0 0.0 0.0]
   [0.0 0.0 0.0 0.0 0.0 10.0   0.0 0.0 0.0 0.0]
   [0.0 0.0 0.0 0.0 0.0  0.0   0.0 0.0 0.0 0.0]
   [0.0 0.0 0.0 0.0 0.0 10.0   0.0 0.0 0.0 0.0]
   [0.0 0.0 0.0 0.0 0.0  0.0   0.0 0.0 0.0 0.0]
   [0.0 0.0 0.0 0.0 0.0  0.0   0.0 0.0 0.0 0.0]
   [0.0 0.0 0.0 0.0 0.0  0.0 100.0 10.0 0.0 0.0]
   [0.0 0.0 0.0 0.0 0.0  0.0   0.0 0.0 0.0 0.0]])

(def use-layer
  [[0.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0]
   [0.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0]
   [0.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0]
   [0.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0]
   [0.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0]
   [0.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0]
   [0.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0]
   [0.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0]
   [0.0 0.0 0.0 0.0 0.0 1.0 0.0 0.0 0.0 0.0]
   [0.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0]])

(def elev-layer
 [[30.0 60.0 32.0 32.0 32.0 28.0 11.0  5.0  5.0 5.0]
  [30.0 29.0 27.0 27.0 27.0 20.0  6.0  5.0  5.0 5.0]
  [30.0 28.0 22.0 22.0 22.0 15.0  3.0  5.0  5.0 5.0]
  [30.0 27.0 17.0 17.0 17.0 11.0  2.0  2.0  5.0 5.0]
  [30.0 26.0 12.0  8.0  9.0  9.0  0.0  1.0  5.0 5.0]
  [30.0 25.0  7.0  3.0  5.0  5.0  1.0  3.0  5.0 5.0]
  [30.0 24.0  2.0  2.0  4.0  4.0  3.0  5.0  8.0 5.0]
  [30.0 23.0  1.0  3.0  3.0  3.0  8.0  9.0 11.0 5.0]
  [30.0 22.0  1.0  3.0  7.0  9.0 12.0 13.0 20.0 5.0]
  [30.0 21.0  1.0  3.0  8.0  9.0 14.0 15.0 17.0 5.0]])

(def water-layer
  [[0.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0]
   [0.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0]
   [0.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0]
   [0.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0]
   [0.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0]
   [0.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0]
   [0.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0]
   [0.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0]
   [0.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0]
   [0.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0]])

(def source-points [[0 1] [3 7] [0 5] [8 8]])

(def use-points (register-math-syms #(filter-matrix-for-coords (p not= _0_) use-layer)))

(def ^:dynamic cell-height 100.0)

(def ^:dynamic cell-width  100.0)

(defn to-meters [[i j]] [(* i cell-height) (* j cell-width)])

(def ^:dynamic trans-threshold 1.0)

(deftest distance-filtering-small-cells
  (binding [cell-height 100.0
            cell-width  100.0]
    (is (= (map #(take 2 %) (select-in-range-views use-points source-points to-meters))
           '(([0 1] [8 5])
             ([3 7] [8 5])
             ([0 5] [8 5])
             ([8 8] [8 5]))))))

(deftest distance-filtering-large-cells
  (binding [cell-height 15000.0
            cell-width  15000.0]
    (is (= (map #(take 2 %) (select-in-range-views use-points source-points to-meters))
           '(([8 8] [8 5]))))))

(deftest sight-line-1
  (is (= (find-line-between (first use-points) (first source-points))
         '([8 5] [7 5] [7 4] [6 4] [5 4] [5 3] [4 3] [3 3] [3 2] [2 2] [1 2] [1 1] [0 1]))))

(deftest sight-line-2
  (is (= (find-line-between (first use-points) (second source-points))
         '([8 5] [7 5] [7 6] [6 6] [5 6] [4 6] [4 7] [3 7]))))

(deftest sight-line-3
  (is (= (find-line-between (first use-points) (nth source-points 2))
         '([8 5] [7 5] [6 5] [5 5] [4 5] [3 5] [2 5] [1 5] [0 5]))))

(deftest sight-line-4
  (is (= (find-line-between (first use-points) (nth source-points 3))
         '([8 5] [8 6] [8 7] [8 8]))))

(deftest sight-line-splitting-1
  (let [source-point (first source-points)
        use-point    (first use-points)
        sight-line   (find-line-between use-point source-point)]
    (is (= (split-sight-line elev-layer sight-line)
           '[([7 5] [7 4])
             ([6 4] [5 4] [5 3] [4 3] [3 3] [3 2] [2 2] [1 2] [1 1] [0 1])]))))

(deftest sight-line-splitting-2
  (let [source-point (second source-points)
        use-point    (first use-points)
        sight-line   (find-line-between use-point source-point)]
    (is (= (split-sight-line elev-layer sight-line)
           '[([7 5])
             ([7 6] [6 6] [5 6] [4 6] [4 7] [3 7])]))))

(deftest sight-line-splitting-3
  (let [source-point (nth source-points 2)
        use-point    (first use-points)
        sight-line   (find-line-between use-point source-point)]
    (is (= (split-sight-line elev-layer sight-line)
           '[([7 5])
             ([6 5] [5 5] [4 5] [3 5] [2 5] [1 5] [0 5])]))))

(deftest sight-line-splitting-4
  (let [source-point (nth source-points 3)
        use-point    (first use-points)
        sight-line   (find-line-between use-point source-point)]
    (is (= (split-sight-line elev-layer sight-line)
           '[()
             ([8 6] [8 7] [8 8])]))))

(deftest sight-line-pruning-1
  (let [source-point (first source-points)
        use-point    (first use-points)
        sight-line   (find-line-between use-point source-point)
        use-elev     (get-in elev-layer use-point)
        use-loc-in-m (to-meters use-point)
        [initial-view-space sight-line-remainder] (split-sight-line elev-layer sight-line)
        initial-view-slope                        (_- (_d (_-_ (get-in elev-layer (second sight-line)) use-elev)
                                                          (euclidean-distance-2 use-loc-in-m (to-meters (second sight-line))))
                                                      0.01) ;; epsilon to include the first step
        first-segment                             (prune-hidden-points nil ;; disregard the increasing elevation constraint
                                                                       initial-view-slope
                                                                       elev-layer
                                                                       use-elev
                                                                       use-loc-in-m
                                                                       to-meters
                                                                       initial-view-space)
        second-segment                            (prune-hidden-points (get-in elev-layer (or (last initial-view-space) (second sight-line)))
                                                                       (:max-slope first-segment)
                                                                       elev-layer
                                                                       use-elev
                                                                       use-loc-in-m
                                                                       to-meters
                                                                       sight-line-remainder)]
    (is (= first-segment  {:max-elev nil
                           :max-slope -0.04242640687119285
                           :filtered-line [[[7 5] 3.0 100.0 -0.06999999999999999]
                                           [[7 4] 3.0 141.4213562373095 -0.06]]}))
    (is (= second-segment {:max-elev 60.0
                           :max-slope 0.05701973342624464
                           :filtered-line [[[6 4] 4.0 223.60679774997897 -0.04242640687119285]
                                           [[5 4] 5.0 316.22776601683796 -0.022360679774997897]
                                           [[4 3] 8.0 447.21359549995793 -0.012649110640673516]
                                           [[3 3] 17.0 538.5164807134504 -0.00223606797749979]
                                           [[2 2] 22.0 670.820393249937 0.014855627054164149]
                                           [[1 2] 27.0 761.5773105863908 0.019379255804998177]
                                           [[1 1] 29.0 806.2257748298549 0.02363515791475006]
                                           [[0 1] 60.0 894.4271909999159 0.02480694691784169]]}))))

(deftest sight-line-pruning-2
  (let [source-point (second source-points)
        use-point    (first use-points)
        sight-line   (find-line-between use-point source-point)
        use-elev     (get-in elev-layer use-point)
        use-loc-in-m (to-meters use-point)
        [initial-view-space sight-line-remainder] (split-sight-line elev-layer sight-line)
        initial-view-slope                        (_- (_d (_-_ (get-in elev-layer (second sight-line)) use-elev)
                                                          (euclidean-distance-2 use-loc-in-m (to-meters (second sight-line))))
                                                      0.01) ;; epsilon to include the first step
        first-segment                             (prune-hidden-points nil ;; disregard the increasing elevation constraint
                                                                       initial-view-slope
                                                                       elev-layer
                                                                       use-elev
                                                                       use-loc-in-m
                                                                       to-meters
                                                                       initial-view-space)
        second-segment                            (prune-hidden-points (get-in elev-layer (or (last initial-view-space) (second sight-line)))
                                                                       (:max-slope first-segment)
                                                                       elev-layer
                                                                       use-elev
                                                                       use-loc-in-m
                                                                       to-meters
                                                                       sight-line-remainder)]
    (is (= first-segment  {:max-elev nil
                           :max-slope -0.06
                           :filtered-line [[[7 5] 3.0 100.0 -0.06999999999999999]]}))
    (is (= second-segment {:max-elev 8.0
                           :max-slope -0.007071067811865475
                           :filtered-line [[[7 6] 8.0 141.4213562373095 -0.06]]}))))

(deftest slope-filtering-1
  (let [source-point (first source-points)
        use-point    (first use-points)
        sight-line   (find-line-between use-point source-point)
        use-elev     (get-in elev-layer use-point)
        use-loc-in-m (to-meters use-point)]
    (is (= (filter-sight-line sight-line elev-layer use-elev use-loc-in-m to-meters)
           [[[7 5] 3.0 100.0 -0.06999999999999999]
            [[7 4] 3.0 141.4213562373095 -0.06]
            [[6 4] 4.0 223.60679774997897 -0.04242640687119285]
            [[5 4] 5.0 316.22776601683796 -0.022360679774997897]
            [[4 3] 8.0 447.21359549995793 -0.012649110640673516]
            [[3 3] 17.0 538.5164807134504 -0.00223606797749979]
            [[2 2] 22.0 670.820393249937 0.014855627054164149]
            [[1 2] 27.0 761.5773105863908 0.019379255804998177]
            [[1 1] 29.0 806.2257748298549 0.02363515791475006]
            [[0 1] 60.0 894.4271909999159 0.02480694691784169]]))))

(deftest slope-filtering-2
  (let [source-point (second source-points)
        use-point    (first use-points)
        sight-line   (find-line-between use-point source-point)
        use-elev     (get-in elev-layer use-point)
        use-loc-in-m (to-meters use-point)]
    (is (= (filter-sight-line sight-line elev-layer use-elev use-loc-in-m to-meters)
           [[[7 5] 3.0 100.0 -0.06999999999999999]
            [[7 6] 8.0 141.4213562373095 -0.06]]))))

(deftest slope-filtering-3
  (let [source-point (nth source-points 2)
        use-point    (first use-points)
        sight-line   (find-line-between use-point source-point)
        use-elev     (get-in elev-layer use-point)
        use-loc-in-m (to-meters use-point)]
    (is (= (filter-sight-line sight-line elev-layer use-elev use-loc-in-m to-meters)
           '[[[7 5] 3.0 100.0 -0.06999999999999999]
             [[6 5] 4.0 200.0 -0.06]
             [[5 5] 5.0 300.0 -0.025]
             [[4 5] 9.0 400.0 -0.013333333333333334]
             [[3 5] 11.0 500.0 0.0]
             [[2 5] 15.0 600.0 0.004]
             [[1 5] 20.0 700.0 0.01]
             [[0 5] 28.0 800.0 0.015714285714285715]]))))

(deftest slope-filtering-4
  (let [source-point (nth source-points 3)
        use-point    (first use-points)
        sight-line   (find-line-between use-point source-point)
        use-elev     (get-in elev-layer use-point)
        use-loc-in-m (to-meters use-point)]
    (is (= (filter-sight-line sight-line elev-layer use-elev use-loc-in-m to-meters)
           '[[[8 6] 12.0 100.0 0.019999999999999997]
             [[8 8] 20.0 300.0 0.03]]))))

(deftest view-impact-calculation-1
  (let [source-point        (first source-points)
        use-point           (first use-points)
        use-elev            (get-in elev-layer use-point)
        source-loc-in-m     (to-meters source-point)
        use-loc-in-m        (to-meters use-point)
        distance-decay      (source-decay (euclidean-distance-2 use-loc-in-m source-loc-in-m))
        filtered-sight-line (filter-sight-line (find-line-between use-point source-point)
                                               elev-layer
                                               use-elev
                                               use-loc-in-m
                                               to-meters)
        [final-point final-elev final-distance final-slope] (last filtered-sight-line)]
    (is (= (if (= final-point source-point)
             (*_ distance-decay
                 (compute-view-impact (get-in source-layer source-point)
                                      final-elev
                                      use-elev
                                      final-slope
                                      final-distance
                                      (and water-layer (not= _0_ (get-in water-layer source-point))))))
           48.0046205225042))))

(deftest view-impact-calculation-2
  (let [source-point        (second source-points)
        use-point           (first use-points)
        use-elev            (get-in elev-layer use-point)
        source-loc-in-m     (to-meters source-point)
        use-loc-in-m        (to-meters use-point)
        distance-decay      (source-decay (euclidean-distance-2 use-loc-in-m source-loc-in-m))
        filtered-sight-line (filter-sight-line (find-line-between use-point source-point)
                                               elev-layer
                                               use-elev
                                               use-loc-in-m
                                               to-meters)
        [final-point final-elev final-distance final-slope] (last filtered-sight-line)]
    (is (= (if (= final-point source-point)
             (*_ distance-decay
                 (compute-view-impact (get-in source-layer source-point)
                                      final-elev
                                      use-elev
                                      final-slope
                                      final-distance
                                      (and water-layer (not= _0_ (get-in water-layer source-point))))))
           nil))))

(deftest view-impact-calculation-3
  (let [source-point        (nth source-points 2)
        use-point           (first use-points)
        use-elev            (get-in elev-layer use-point)
        source-loc-in-m     (to-meters source-point)
        use-loc-in-m        (to-meters use-point)
        distance-decay      (source-decay (euclidean-distance-2 use-loc-in-m source-loc-in-m))
        filtered-sight-line (filter-sight-line (find-line-between use-point source-point)
                                               elev-layer
                                               use-elev
                                               use-loc-in-m
                                               to-meters)
        [final-point final-elev final-distance final-slope] (last filtered-sight-line)]
    (is (= (if (= final-point source-point)
             (*_ distance-decay
                 (compute-view-impact (get-in source-layer source-point)
                                      final-elev
                                      use-elev
                                      final-slope
                                      final-distance
                                      (and water-layer (not= _0_ (get-in water-layer source-point))))))
           22.95330612244898))))

(deftest view-impact-calculation-4
  (let [source-point        (nth source-points 3)
        use-point           (first use-points)
        use-elev            (get-in elev-layer use-point)
        source-loc-in-m     (to-meters source-point)
        use-loc-in-m        (to-meters use-point)
        distance-decay      (source-decay (euclidean-distance-2 use-loc-in-m source-loc-in-m))
        filtered-sight-line (filter-sight-line (find-line-between use-point source-point)
                                               elev-layer
                                               use-elev
                                               use-loc-in-m
                                               to-meters)
        [final-point final-elev final-distance final-slope] (last filtered-sight-line)]
    (is (= (if (= final-point source-point)
             (*_ distance-decay
                 (compute-view-impact (get-in source-layer source-point)
                                      final-elev
                                      use-elev
                                      final-slope
                                      final-distance
                                      (and water-layer (not= _0_ (get-in water-layer source-point))))))
           9.999639999999998))))

(deftest sink-effects-calculation-1
  (let [source-point        (first source-points)
        use-point           (first use-points)
        use-elev            (get-in elev-layer use-point)
        use-loc-in-m        (to-meters use-point)
        filtered-sight-line (filter-sight-line (find-line-between use-point source-point)
                                               elev-layer
                                               use-elev
                                               use-loc-in-m
                                               to-meters)]
    (is (= (compute-sink-effects sink-layer filtered-sight-line use-elev)
           {}))))

(deftest sink-effects-calculation-2
  (let [source-point        (second source-points)
        use-point           (first use-points)
        use-elev            (get-in elev-layer use-point)
        use-loc-in-m        (to-meters use-point)
        filtered-sight-line (filter-sight-line (find-line-between use-point source-point)
                                               elev-layer
                                               use-elev
                                               use-loc-in-m
                                               to-meters)]
    (is (= (compute-sink-effects sink-layer filtered-sight-line use-elev)
           {}))))

(deftest sink-effects-calculation-3
  (let [source-point        (nth source-points 2)
        use-point           (first use-points)
        use-elev            (get-in elev-layer use-point)
        use-loc-in-m        (to-meters use-point)
        filtered-sight-line (filter-sight-line (find-line-between use-point source-point)
                                               elev-layer
                                               use-elev
                                               use-loc-in-m
                                               to-meters)]
    (is (= (compute-sink-effects sink-layer filtered-sight-line use-elev)
           {[5 5] 6.8425
            [3 5] 1.7045454545454541}))))

(deftest sink-effects-calculation-4
  (let [source-point        (nth source-points 3)
        use-point           (first use-points)
        use-elev            (get-in elev-layer use-point)
        use-loc-in-m        (to-meters use-point)
        filtered-sight-line (filter-sight-line (find-line-between use-point source-point)
                                               elev-layer
                                               use-elev
                                               use-loc-in-m
                                               to-meters)]
    (is (= (compute-sink-effects sink-layer filtered-sight-line use-elev)
           {[8 6] 8.312500000000005}))))

(deftest actual-weight-calculation-1
  (let [source-point        (first source-points)
        use-point           (first use-points)
        use-elev            (get-in elev-layer use-point)
        source-loc-in-m     (to-meters source-point)
        use-loc-in-m        (to-meters use-point)
        distance-decay      (source-decay (euclidean-distance-2 use-loc-in-m source-loc-in-m))
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
                actual-weight (rv-fn '(fn [p s] (max 0.0 (- p s))) possible-weight (reduce _+_ _0_ (vals sink-effects)))]
            (is (= actual-weight 48.0046205225042))))))))

(deftest actual-weight-calculation-2
  (let [source-point        (second source-points)
        use-point           (first use-points)
        use-elev            (get-in elev-layer use-point)
        source-loc-in-m     (to-meters source-point)
        use-loc-in-m        (to-meters use-point)
        distance-decay      (source-decay (euclidean-distance-2 use-loc-in-m source-loc-in-m))
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
                actual-weight (rv-fn '(fn [p s] (max 0.0 (- p s))) possible-weight (reduce _+_ _0_ (vals sink-effects)))]
            (is (= actual-weight nil))))))))

(deftest actual-weight-calculation-3
  (let [source-point        (nth source-points 2)
        use-point           (first use-points)
        use-elev            (get-in elev-layer use-point)
        source-loc-in-m     (to-meters source-point)
        use-loc-in-m        (to-meters use-point)
        distance-decay      (source-decay (euclidean-distance-2 use-loc-in-m source-loc-in-m))
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
                actual-weight (rv-fn '(fn [p s] (max 0.0 (- p s))) possible-weight (reduce _+_ _0_ (vals sink-effects)))]
            (is (= actual-weight 14.406260667903524))))))))

(deftest actual-weight-calculation-4
  (let [source-point        (nth source-points 3)
        use-point           (first use-points)
        use-elev            (get-in elev-layer use-point)
        source-loc-in-m     (to-meters source-point)
        use-loc-in-m        (to-meters use-point)
        distance-decay      (source-decay (euclidean-distance-2 use-loc-in-m source-loc-in-m))
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
                actual-weight (rv-fn '(fn [p s] (max 0.0 (- p s))) possible-weight (reduce _+_ _0_ (vals sink-effects)))]
            (is (= actual-weight 1.6871399999999923))))))))

;; (run-tests)
