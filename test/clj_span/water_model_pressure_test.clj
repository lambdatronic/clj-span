(ns clj-span.water-model-pressure-test
  (:use clojure.test
        clojure.pprint
        clj-span.core
        clj-span.models.surface-water :reload-all
        clj-misc.utils
        clj-span.water-model-test
        clj-misc.matrix-ops)
  (:require [clojure.core.reducers :as r]))


;(defn register-math-syms [t]
;  (with-typed-math-syms value-type [_0_ _+_ _*_ _d_ *_ _d  _<_  _>_ rv-fn _> _min_]
;    (t)))
;
;(use-fixtures :once register-math-syms)





(def elev-layer-init
  [[30.0	55.0	34.0	32.0	33.0	32.0	22.0	12.0	10.0	 9.0	 8.0	 6.0]
   [ 0.0	60.0	32.0	32.0	32.0	28.0	11.0	 5.0	 5.0	 6.0	 7.0	15.0]
   [28.0	29.0	27.0	27.0	27.0	20.0	 6.0	 5.0	 5.0	 5.0	 8.0	13.0]
   [22.0	28.0	22.0	22.0	22.0	15.0	 3.0	 5.0	 5.0	 5.0	 8.0	11.0]
   [18.0	27.0	17.0	17.0	17.0	11.0	 2.0   2.0	 4.0	 6.0	10.0	16.0]
   [15.0	26.0	12.0	 8.0 	 9.0   9.0	 3.0	 1.0	 5.0	 5.0	 8.0	19.0]
   [12.0	25.0	 7.0	 3.0	 5.0	 5.0	 1.0	 3.0	 6.0	 8.0	11.0	14.0]
   [ 9.0	24.0	 2.0	 2.0	 4.0	 4.0	 3.0	 5.0	 8.0	 8.0	12.0	12.0]
   [ 6.0	 3.0	 1.0	 3.0	 3.0	 3.0	 8.0	 9.0	11.0	12.0	14.0	11.0]
   [ 8.0	 3.0	 1.0	 3.0	 7.0	 9.0	12.0	13.0	20.0	13.0	15.0	 7.0]
   [ 8.0	 3.0	 1.0	 3.0	 8.0	 9.0	14.0	15.0	17.0	15.0	18.0	 9.0]
   [ 5.0	 3.0	 1.0	 5.0	 5.0	 6.0	18.0	20.0	30.0	20.0	20.0	11.0]])

(def water-layer-init
	[[0.0	0.0	0.0	0.0	0.0	0.0	0.0	0.0	0.0	0.0	0.0	1.0]
	 [0.0	0.0	0.0	0.0	0.0	0.0	0.0	0.0	0.0	1.0	1.0	0.0]
	 [0.0	0.0	0.0	0.0	0.0	0.0	0.0	0.0	0.0	1.0	0.0	0.0]
	 [0.0	1.0	0.0	0.0	0.0	0.0	0.0	1.0	1.0	0.0	0.0	0.0]
	 [0.0	1.0	0.0	0.0	0.0	0.0	2.0	1.0	0.0	0.0	0.0	0.0]
	 [0.0	1.0	1.0	0.0	0.0	0.0	1.0	0.0	1.0	0.0	0.0	0.0]
	 [0.0	0.0	1.0	0.0	0.0	1.0	0.0	0.0	0.0	1.0	0.0	0.0]
	 [0.0	0.0	0.0	1.0	1.0	0.0	0.0	0.0	0.0	1.0	0.0	0.0]
	 [0.0	0.0	0.0	0.0	1.0	0.0	0.0	0.0	0.0	1.0	0.0	0.0]
	 [0.0	0.0	1.0	1.0	0.0	0.0	0.0	0.0	0.0	1.0	0.0	0.0]
	 [0.0	0.0	1.0	0.0	0.0	0.0	0.0	0.0	0.0	1.0	0.0	0.0]
	 [1.0	1.0	1.0	0.0	0.0	0.0	0.0	0.0	0.0	0.0	0.0	0.0]])

(def size 12)


; generates a uniformly distributed random matrix
(defn make-random-matrix [factor bound]
  (make-matrix (* factor size) (* factor size) (fn[_] (rand bound))))
               
(defn make-random-int-matrix [factor bound]
  (make-matrix (* factor size) (* factor size) (fn[_] (rand-int bound))))

(defn make-palindrome [factor layer]
  (make-matrix (* factor size)  (* factor size)
      (fn [[i j]] (get-in layer [(mod i size) (mod j size)]))))
(defn ubertest
  [factor]
(let 
  [params    {:source-layer (make-random-matrix factor 20)
              :sink-layer   (make-random-matrix factor 12)
              :use-layer   (make-random-int-matrix factor 2)
              :flow-layers {"Altitude"  (make-palindrome factor elev-layer-init)
                            "River"     (make-palindrome factor water-layer-init)}
              :source-threshold   1.0
              :sink-threshold     1.0
              :use-threshold      1.0
              :trans-threshold    1.0
              :cell-width         100.0
              :cell-height        100.0
              :rv-max-states      10
              :downscaling-factor 1
              :source-type        :finite
              :sink-type          :finite
              :use-type           :finite
              :benefit-type       :rival
              :value-type         :numbers
              :result-type        :java-hashmap
              :flow-model         "SurfaceWaterMovement"
              :animation?         false}]
  (run-span params)))


(defn simple-uber []
(let 
  [params    {:source-layer clj-span.water-model-test/source-layer
              :sink-layer   clj-span.water-model-test/sink-layer
              :use-layer   clj-span.water-model-test/use-layer
              :flow-layers {"Altitude"  clj-span.water-model-test/elev-layer
                            "River"     clj-span.water-model-test/water-layer}
              :source-threshold   1.0
              :sink-threshold     1.0
              :use-threshold      1.0
              :trans-threshold    1.0
              :cell-width         100.0
              :cell-height        100.0
              :rv-max-states      10
              :downscaling-factor 1
              :source-type        :finite
              :sink-type          :finite
              :use-type           :finite
              :benefit-type       :rival
              :value-type         :numbers
              :result-type        :java-hashmap
              :flow-model         "SurfaceWaterMovement"
              :animation?         true}]
  (run-span params)))

