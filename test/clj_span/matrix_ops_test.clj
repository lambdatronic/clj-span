(ns clj-span.matrix-ops-test
  (:use clojure.test
        clojure.pprint
        clj-misc.matrix-ops :reload-all))


(deftest test-neighbors-clockwise
    (is (= (get-neighbors-clockwise 10 10 [2 2]) [[3 2] [3 3] [2 3] [1 3] [1 2] [1 1] [2 1] [3 1]]))
    (is (= (get-neighbors-clockwise 10 10 [1 0]) [[2 0] [2 1] [1 1] [0 1] [0 0]])))

;(deftest test-group-by-aga
;  (is (= (g
;([8 4] [9 2] [10 2])
