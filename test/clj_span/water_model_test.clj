(ns clj-span.water-model-test
  (:use clojure.test
        clojure.pprint
        clj-span.core
        clj-span.models.surface-water :reload-all
        clj-misc.utils
        clj-misc.matrix-ops)
  (:require [clojure.core.reducers :as r]))

(def value-type :numbers)

(defn register-math-syms [t]
  (with-typed-math-syms value-type [_0_ _+_ _*_ _d_ *_ _d  _<_  _>_ rv-fn _>]
    (t)))

(use-fixtures :once register-math-syms)

(def rows 12)

(def cols 11)

(def source-layer
 [[9.0	9.0	8.0	8.0	5.0	3.0 	3.0	6.0	6.0	6.0	8.0]
  [9.0	8.0	8.0	5.0	5.0	5.0 	3.0	5.0	5.0	6.0	8.0]
  [8.0	8.0	8.0	5.0	3.0	3.0 	3.0	5.0	5.0	5.0	8.0]
  [8.0	8.0	5.0	5.0	3.0	3.0 	3.0	3.0	3.0	3.0	8.0]
  [5.0	5.0	5.0	5.0	3.0	3.0 	2.0	2.0	4.0	6.0	8.0]
  [5.0	5.0	5.0	5.0	3.0	3.0 	3.0	1.0	5.0	5.0	8.0]
  [8.0	8.0	8.0	8.0	5.0	3.0 	7.0	7.0	7.0	8.0	8.0]
  [8.0	8.0	8.0	8.0	5.0	7.0 	7.0	7.0	7.0	8.0	8.0]
  [5.0	5.0	5.0	5.0	5.0	7.0 7.0	12.0	12.0	12.0	12.0]
  [5.0	5.0	5.0	5.0	5.0	7.0	12.0	12.0	12.0	12.0	12.0]
  [5.0	5.0	5.0	5.0	5.0	7.0	14.0	12.0	12.0	12.0	12.0]
  [5.0	5.0	5.0	5.0	5.0	7.0	18.0	12.0	12.0	12.0	12.0]])


(def sink-layer
 [[0.1	0.1	0.1	0.1	0.1	0.1	0.5	2.0	2.0	3.0	2.0]
[0.1	0.1	0.1	0.1	0.1	0.5	2.0	3.0	3.0	3.0	3.0]
[0.5	0.5	0.5	0.5	0.5	0.5	3.0	3.0	3.0	3.0	3.0]
[0.5	0.5	0.5	0.5	0.5	2.0	4.0	3.0	3.0	3.0	3.0]
[2.0	0.5	2.0	2.0	2.0	2.0	4.0	4.0	4.0	3.0	2.0]
[2.0	0.5	2.0	3.0	3.0	3.0	4.0	4.0	3.0	3.0	3.0]
[2.0	0.5	3.0	4.0	3.0	3.0	4.0	4.0	3.0	3.0	2.0]
[3.0	0.5	4.0	4.0	4.0	4.0	4.0	3.0	3.0	3.0	2.0]
[3.0	4.0	4.0	4.0	4.0	4.0	3.0	3.0	2.0	2.0	2.0]
[3.0	4.0	4.0	4.0	3.0	3.0	2.0	2.0	0.5	2.0	2.0]
[3.0	4.0	4.0	4.0	3.0	3.0	2.0	2.0	2.0	2.0	2.0]
[3.0	4.0	4.0	3.0	3.0	3.0	2.0	0.5	0.1	0.5	0.5]])

(def use-layer
  [[0.0	0.0	0.0	0.0	0.0	0.0	0.0	0.0	0.0	0.0	0.0]
[0.0	0.0	0.0	0.0	0.0	0.0	0.0	2.0	0.0	0.0	0.0]
[0.0	0.0	0.0	0.0	0.0	0.0	0.0	0.0	0.0	0.0	0.0]
[0.0	0.0	0.0	0.0	0.0	0.0	0.0	0.0	0.0	0.0	0.0]
[0.0	0.0	0.0	0.0	0.0	0.0	0.0	0.0	0.0	0.0	0.0]
[0.0	0.0	0.0	0.0	0.0	0.0	0.0	0.0	0.0	0.0	0.0]
[0.0	0.0	100.0	0.0	0.0	0.0	0.0	0.0	0.0	20.0	0.0]
[0.0	100.0	0.0	0.0	1.0	0.0	0.0	0.0	10.0	150.0	0.0]
[0.0	1.0	1.0	0.0	1.0	0.0	0.0	0.0	0.0	0.0	0.0]
[0.0	1.0	1.0	1.0	0.0	0.0	0.0	50.0		0.0	0.0]
[0.0	1.0	1.0	0.0	0.0	0.0	0.0	0.0	0.0	0.0	0.0]
[0.0	1.0	1.0	0.0	0.0	0.0	0.0	0.0	0.0	0.0	0.0]])

(def elev-layer
 [[30.0 55.0 34.0 32.0 33.0 32.0 22.0 12.0 10.0 9.0 11.0]
 [30.0 60.0 32.0 32.0 32.0 28.0 11.0 5.0 5.0 6.0 7.0]
 [28.0 29.0 27.0 27.0 27.0 20.0 6.0 5.0 5.0 5.0 8.0]
 [22.0 28.0 22.0 22.0 22.0 15.0 3.0 5.0 5.0 5.0 8.0]
 [18.0 27.0 17.0 17.0 17.0 11.0 2.0 2.0 4.0 6.0 10.0]
 [15.0 26.0 12.0  8.0 9.0 9.0 3.0 1.0 5.0 5.0 8.0]
 [12.0 25.0  7.0  3.0 5.0 5.0 1.0 3.0 6.0 8.0 11.0]
 [9.0 24.0  2.0  2.0 4.0 4.0 3.0 5.0 8.0 8.0 12.0]
 [6.0  3.0  1.0  3.0 3.0 3.0 8.0 9.0 11.0 12.0 14.0]
 [8.0  3.0  1.0  3.0 7.0 9.0 12.0 13.0 20.0 13.0 15.0]
 [8.0  3.0  1.0  3.0 8.0 9.0 14.0 15.0 17.0 15.0 18.0]
 [5.0  3.0  1.0  5.0 5.0 6.0 18.0 20.0 30.0 20.0 20.0]])

(def water-layer
 [[0.0	0.0	0.0	0.0	0.0	0.0	0.0	0.0	0.0	1.0	0.0]
  [0.0	0.0	0.0	0.0	0.0	0.0	0.0	0.0	0.0	1.0	1.0]
  [0.0	0.0	0.0	0.0	0.0	0.0	0.0	0.0	0.0	1.0	0.0]
  [0.0	0.0	0.0	0.0	0.0	0.0	0.0	1.0	1.0	0.0	0.0]
  [0.0	0.0	0.0	0.0	0.0	0.0	0.0	1.0	0.0	0.0	0.0]
  [0.0	0.0	0.0	0.0	0.0	0.0	1.0	0.0	1.0	0.0	0.0]
 	[0.0	0.0	0.0	0.0	0.0	1.0	0.0	0.0	0.0	1.0	0.0]
	[0.0	0.0	0.0	0.0	1.0	0.0	0.0	0.0	0.0	1.0	0.0]
	[0.0	0.0	0.0	0.0	1.0	0.0	0.0	0.0	0.0	1.0	0.0]
	[0.0	0.0	1.0	1.0	0.0	0.0	0.0	0.0	0.0	1.0	0.0]
	[0.0	0.0	1.0	0.0	0.0	0.0	0.0	0.0	1.0	1.0	0.0]
	[0.0	0.0	1.0	0.0	0.0	0.0	0.0	0.0	1.0	0.0	0.0]])

(def source-points (register-math-syms #(filter-matrix-for-coords (p not= _0_) source-layer)))

(def use-points [[7 1] [6 2] [9 7] [7 9] [6 9] [7 8]])


(def ^:dynamic trans-threshold 0.001)

(deftest test-create-in-stream
  (let [params {:flow-layers {"River" water-layer}}]
    (is (= (count (:in-stream? (create-in-stream-test params)))
           23))))


(deftest test-link-streams-to-users
   (let [params {:flow-layers {"River" water-layer}
                 :rows rows
                 :cols cols
                 :use-points use-points}]
     (is (= (get (-> params 
                   create-in-stream-test
                   link-streams-to-users)
                 :stream-intakes)
         {[8 9] [[7 8]], [10 8] [[9 7]], [7 4] [[6 2]], [9 2] [[7 1]], [6 9] [[6 9]], [7 9] [[7 9]]})))) 

(deftest test-find-lowest
  (is (= (find-lowest elev-layer [[1 1]]) [1 1]))
  (is (= (find-lowest elev-layer (get-neighbors rows cols [1 1])) [2 2]))
  (is (= (find-lowest elev-layer (get-neighbors rows cols [4 7])) [5 7]))
  (is (or (= (find-lowest elev-layer (get-neighbors rows cols [8 3])) [9 2])
          (= (find-lowest elev-layer (get-neighbors rows cols [8 3])) [8 2]))))

(def in-stream? (let [params {:flow-layers {"River" water-layer}}]
                  (:in-stream? (create-in-stream-test params))))

(deftest test-lowest-neighbors-overland-simple
  (is (= (lowest-neighbors-overland [1 1]  in-stream? elev-layer rows cols) [2 2]))
  (is (= (lowest-neighbors-overland [10 6] in-stream? elev-layer rows cols) [11 5]))
  (is (= (lowest-neighbors-overland [5 2]  in-stream? elev-layer rows cols) [6 3])))

(deftest test-lowest-neighbors-overland-complete
  (is (= (make-matrix rows cols #(lowest-neighbors-overland % in-stream? elev-layer rows cols))
         [[nil nil nil nil nil nil nil nil nil nil nil] 
          [nil [2 2] [2 3] [2 4] [2 5] [2 6] [2 7] [1 7] [2 9] [2 9] nil] 
          [nil [3 2] [3 3] [3 4] [3 5] [3 6] [3 7] [3 8] [3 7] [3 8] nil] 
          [nil [4 2] [4 3] [4 4] [4 5] [4 6] [4 7] [4 7] [4 7] [2 9] nil] 
          [nil [5 2] [5 3] [5 3] [5 3] [5 6] [4 7] [5 6] [4 7] [3 8] nil] 
          [nil [6 2] [6 3] [6 3] [6 5] [5 6] [4 7] [4 7] [4 7] [5 8] nil] 
          [nil [7 2] [7 3] [7 4] [7 4] [5 6] [5 6] [5 6] [5 8] [5 8] nil] 
          [nil [8 2] [8 2] [8 4] [8 4] [8 4] [6 5] [6 6] [6 9] [6 9] nil] 
          [nil [9 2] [9 2] [9 2] [9 3] [8 4] [7 6] [7 6] [7 9] [7 9] nil] 
          [nil [10 2] [10 2] [10 2] [8 4] [8 4] [8 5] [10 8] [8 9] [8 9] nil] 
          [nil [11 2] [11 2] [9 2] [9 3] [11 4] [11 5] [10 8] [9 9] [9 9] nil] 
          [nil nil nil nil nil nil nil nil nil nil nil]])))

(defn get-water-neighbors [id]
  (seq (filter in-stream? (get-neighbors-clockwise rows cols id))))

(deftest test-find-bounded-stream-segment
  (let [explore-stream (p find-next-in-stream-step rows cols elev-layer in-stream?)]
    (is (= (find-bounded-stream-segment [8 4] in-stream? explore-stream)
           '([11 2] [10 2] [9 2] [9 3] [8 4] [7 4] [6 5] [5 6] [4 7])))
    (is (= (find-bounded-stream-segment [7 9] in-stream? explore-stream)
           '([4 7] [5 8] [6 9] [7 9] [8 9] [9 9] [10 9] [10 8] [11 8])))
    (is (= (find-bounded-stream-segment [3 7] in-stream? explore-stream)
           '([4 7] [3 7] [3 8] [2 9] [1 9])))))

(deftest test-select-stream-path-dirs
  (let [explore-stream (p find-next-in-stream-step rows cols elev-layer in-stream?)]
    (is (= (select-stream-path-dirs elev-layer
                                    (find-bounded-stream-segment [8 4] in-stream? explore-stream))
           {[4 7] [5 6], [5 6] [6 5], [6 5] [7 4], [7 4] [8 4], [8 4] [9 3], [9 3] [9 2], [9 2] [10 2], [10 2] [11 2]}))
    (is (= (select-stream-path-dirs elev-layer
                                    (find-bounded-stream-segment [7 9] in-stream? explore-stream))
           {[11 8] [10 8], [10 8] [10 9], [10 9] [9 9], [9 9] [8 9], [8 9] [7 9], [7 9] [6 9], [6 9] [5 8], [5 8] [4 7]}))
    (is (= (select-stream-path-dirs elev-layer
                                    (find-bounded-stream-segment [3 7] in-stream? explore-stream))
           {[1 9] [2 9], [2 9] [3 8], [3 8] [3 7], [3 7] [4 7]}))))

(deftest test-determine-river-flow-directions
  (is (= (determine-river-flow-directions in-stream? elev-layer rows cols)
         {[6 5] [7 4], [10 9] [9 9], [9 9] [8 9], [5 6] [6 5], [8 9] [7 9],
          [7 9] [6 9], [4 7] [5 6], [5 8] [4 7], [6 9] [5 8], [3 7] [4 7],
          [3 8] [3 7], [2 9] [3 8], [1 9] [2 9], [10 2] [11 2], [9 2] [10 2],
          [9 3] [9 2], [8 4] [9 3], [7 4] [8 4], [11 8] [10 8], [10 8] [10 9]})))

(deftest test-build-stream-network
  (is (= (:stream-network (build-stream-network {:in-stream? in-stream? 
                                                 :elev-layer elev-layer 
                                                 :rows rows 
                                                 :cols cols}))
         [[nil  nil    nil   nil   nil    nil    nil    nil   nil     nil  nil]
          [nil  [2 2]  [2 3] [2 4] [2 5]  [2 6]  [2 7]  [1 7] [2 9]  [2 9] nil]
          [nil  [3 2]  [3 3] [3 4] [3 5]  [3 6]  [3 7]  [3 8] [3 7]  [3 8] nil]
          [nil  [4 2]  [4 3] [4 4] [4 5]  [4 6]  [4 7]  [4 7] [3 7]  [2 9] nil]
          [nil  [5 2]  [5 3] [5 3] [5 3]  [5 6]  [4 7]  [5 6] [4 7]  [3 8] nil]
          [nil  [6 2]  [6 3] [6 3] [6 5]  [5 6]  [6 5]  [4 7] [4 7]  [5 8] nil]
          [nil  [7 2]  [7 3] [7 4] [7 4]  [7 4]  [5 6]  [5 6] [5 8]  [5 8] nil]
          [nil  [8 2]  [8 2] [8 4] [8 4]  [8 4]  [6 5]  [6 6] [6 9]  [6 9] nil]
          [nil  [9 2]  [9 2] [9 2] [9 3]  [8 4]  [7 6]  [7 6] [7 9]  [7 9] nil]
          [nil [10 2] [10 2] [9 2] [8 4]  [8 4]  [8 5] [10 8] [8 9]  [8 9] nil]
          [nil [11 2] [11 2] [9 2] [9 3] [11 4] [11 5] [10 8] [10 9] [9 9] nil]
          [nil  nil    nil   nil   nil    nil    nil    nil   [10 8]  nil  nil]])))

(def stream-intakes
  (let [params {:flow-layers {"River" water-layer}
                :rows rows
                :cols cols
                :use-points use-points}]
    (get (-> params 
           create-in-stream-test
           link-streams-to-users)
         :stream-intakes)))

(deftest test-water-neighbors
  (is (= (get-water-neighbors [9 9]) [[10 9] [8 9] [10 8]]))
  (is (= (get-water-neighbors [9 5]) [[8 4]]))
  (is (= (get-water-neighbors [9 3]) [[8 4] [9 2] [10 2]])))

(deftest test-group-by-adjacency
  (is (= (group-by-adjacency (get-water-neighbors [9 9])) ['([10 8] [10 9]) [[8 9]]]))
  (is (= (group-by-adjacency (get-water-neighbors [9 5])) ['([8 4])]))
  (is (= (group-by-adjacency (get-water-neighbors [9 3])) ['([8 4]) '([9 2] [10 2])]))
  (is (= (group-by-adjacency (get-water-neighbors [10 3])) ['([9 3] [9 2] [10 2] [11 2])])))

(def stream-network
  (:stream-network (build-stream-network {:in-stream? in-stream? 
                                          :elev-layer elev-layer 
                                          :rows rows 
                                          :cols cols})))

(deftest test-filter-upstream-nodes
  (is (= (:service-network (filter-upstream-nodes {:stream-intakes  stream-intakes
                                                  :stream-network stream-network
                                                  :rows rows 
                                                  :cols cols}))
         [[nil nil nil nil nil nil nil nil nil nil nil]
          [nil [2 2] [2 3] [2 4] [2 5] [2 6] [2 7] nil [2 9] [2 9] nil]
          [nil [3 2] [3 3] [3 4] [3 5] [3 6] [3 7] [3 8] [3 7] [3 8] nil]
          [nil [4 2] [4 3] [4 4] [4 5] [4 6] [4 7] [4 7] [3 7] [2 9] nil]
          [nil [5 2] [5 3] [5 3] [5 3] [5 6] [4 7] [5 6] [4 7] [3 8] nil]
          [nil [6 2] [6 3] [6 3] [6 5] [5 6] [6 5] [4 7] [4 7] [5 8] nil]
          [nil [7 2] [7 3] [7 4] [7 4] [7 4] [5 6] [5 6] [5 8] [5 8] nil]
          [nil [8 2] [8 2] [8 4] [8 4] [8 4] [6 5] [6 6] [6 9] [6 9] nil]
          [nil [9 2] [9 2] [9 2] [9 3] [8 4] [7 6] [7 6] [7 9] [7 9] nil]
          [nil nil [10 2] [9 2] [8 4] [8 4] [8 5] [10 8] [8 9] [8 9] nil]
          [nil nil nil [9 2] [9 3] nil nil [10 8] [10 9] [9 9] nil]
          [nil nil nil nil nil nil nil nil [10 8] nil nil]])))

(deftest test-find-most-downstream-intakes
  (let [stream-network (:service-network (filter-upstream-nodes {:stream-intakes stream-intakes
                                                                :stream-network stream-network
                                                                :rows rows 
                                                                :cols cols}))]
    (is (= (find-most-downstream-intakes stream-intakes stream-network)
           '([9 2])))))
