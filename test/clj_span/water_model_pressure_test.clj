(ns clj-span.water-model-pressure-test
  (:use clojure.test
        clojure.pprint
        clj-span.core
        clj-span.gui
        clj-span.models.surface-water :reload-all
        [clj-span.worldgen :only [read-layer-from-file write-layer-to-file] :rename {write-layer-to-file write-matrix-to-file}]
        clj-misc.utils
        clj-misc.matrix-ops
        clj-span.elevation-correction)
  (:require [clojure.core.reducers :as r]
            [clj-span.water-model-test :as wmt]))

(def value-type :numbers)

(defn register-math-syms [t]
  (with-typed-math-syms value-type [_0_ _+_ _-_ _*_ _d_ *_ _d  _<_  _>_ rv-fn _> _min_]
    (t)))

(use-fixtures :once register-math-syms)

(def elev-layer-init
  [[30.0  55.0  34.0  32.0  33.0  32.0  22.0  12.0  10.0   9.0   8.0   6.0]
   [ 0.0  60.0  32.0  32.0  32.0  28.0  11.0   5.0   5.0   6.0   7.0  15.0]
   [28.0  29.0  27.0  27.0  27.0  20.0   6.0   5.0   5.0   5.0   8.0  13.0]
   [22.0  28.0  22.0  22.0  22.0  15.0   3.0   5.0   5.0   5.0   8.0  11.0]
   [18.0  27.0  17.0  17.0  17.0  11.0   2.0   2.0   4.0   6.0  10.0  16.0]
   [15.0  26.0  12.0   8.0   9.0   9.0   3.0   1.0   5.0   5.0   8.0  19.0]
   [12.0  25.0   7.0   3.0   5.0   5.0   1.0   3.0   6.0   8.0  11.0  14.0]
   [ 9.0  24.0   2.0   2.0   4.0   4.0   3.0   5.0   8.0   8.0  12.0  12.0]
   [ 6.0   3.0   1.0   3.0   3.0   3.0   8.0   9.0  11.0  12.0  14.0  11.0]
   [ 8.0   3.0   1.0   3.0   7.0   9.0  12.0  13.0  20.0  13.0  15.0   7.0]
   [ 8.0   3.0   1.0   3.0   8.0   9.0  14.0  15.0  17.0  15.0  18.0   9.0]
   [ 5.0   3.0   1.0   5.0   5.0   6.0  18.0  20.0  30.0  20.0  20.0  11.0]])

(def water-layer-init
  [[0.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0 1.0]
   [0.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0 1.0 1.0 0.0]
   [0.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0 1.0 0.0 0.0]
   [0.0 1.0 0.0 0.0 0.0 0.0 0.0 1.0 1.0 0.0 0.0 0.0]
   [0.0 1.0 0.0 0.0 0.0 0.0 1.0 1.0 0.0 0.0 0.0 0.0]
   [0.0 1.0 1.0 0.0 0.0 0.0 1.0 0.0 1.0 0.0 0.0 0.0]
   [0.0 0.0 1.0 0.0 0.0 1.0 0.0 0.0 0.0 1.0 0.0 0.0]
   [0.0 0.0 0.0 1.0 1.0 0.0 0.0 0.0 0.0 1.0 0.0 0.0]
   [0.0 0.0 0.0 0.0 1.0 0.0 0.0 0.0 0.0 1.0 0.0 0.0]
   [0.0 0.0 1.0 1.0 0.0 0.0 0.0 0.0 0.0 1.0 0.0 0.0]
   [0.0 0.0 1.0 0.0 0.0 0.0 0.0 0.0 0.0 1.0 0.0 0.0]
   [0.0 1.0 1.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0]])

(def tanzania-elevation-layer          (delay (read-layer-from-file "input_layers/tanzania_elevation.clj")))

(def tanzania-elevation-filled-layer   (delay (read-layer-from-file "input_layers/tanzania_elevation_filled.clj")))

(def tanzania-elevation-averaged-layer (delay (read-layer-from-file "input_layers/tanzania_elevation_averaged.clj")))

(def tanzania-water-layer              (delay (read-layer-from-file "input_layers/tanzania_water.clj")))

(def size 12)

(def r-gen (java.util.Random. 12345))
(defn random [bound] (* (.nextDouble r-gen) bound))
(defn random-int [bound] (.nextInt r-gen  bound))

; generates a uniformly distributed random matrix
(defn make-random-matrix-by-dims [rows cols bound]
  (make-matrix rows cols (fn [_] (random bound))))

(defn make-random-matrix [factor bound]
  (make-matrix (* factor size) (* factor size) (fn [_] (random bound))))

(defn make-random-int-matrix [factor bound]
  (make-matrix (* factor size) (* factor size) (fn [_] (rand-int bound))))

(defn make-palindrome [factor layer]
  (make-matrix (* factor size) (* factor size)
               (fn [[i j]] (get-in layer [(mod i size) (mod j size)]))))

(defn make-palindrome-by-dims [rows cols layer]
	(make-matrix rows cols               
			(fn [[i j]] (get-in layer [(mod i size) (mod j size)]))))


(defn ubersimple
  []
  (run-span {:source-layer       wmt/source-layer
             :sink-layer         wmt/sink-layer
             :use-layer          wmt/use-layer
             :flow-layers        {"Altitude" wmt/elev-layer
                                  "River"    wmt/water-layer}
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
             :value-type         value-type
             :result-type        :java-hashmap
             :flow-model         "SurfaceWaterMovement"
             :animation?         true}))

(defn ubertest
  [factor]
  (run-span {:source-layer       (make-random-matrix factor 20)
             :sink-layer         (make-random-matrix factor 12)
             :use-layer          (make-random-matrix factor 10)
             :flow-layers        {"Altitude" (make-palindrome factor elev-layer-init)
                                  "River"    (make-palindrome factor water-layer-init)}
             :source-threshold   5.0
             :sink-threshold     2.0
             :use-threshold      2.0
             :trans-threshold    1.0
             :cell-width         100.0
             :cell-height        100.0
             :rv-max-states      10
             :downscaling-factor 1
             :source-type        :finite
             :sink-type          :finite
             :use-type           :finite
             :benefit-type       :rival
             :value-type         value-type
             :result-type        :java-hashmap
             :flow-model         "SurfaceWaterMovement"
             :animation?         true}))

(defn tanzania-test
  [elev-type]
  {:pre [(contains? #{:orig :filled :averaged :crazy} elev-type)]}
  (run-span {:source-layer       (make-random-matrix-by-dims 239 222 20)
             :sink-layer         (make-random-matrix-by-dims 239 222 12)
             :use-layer          (make-random-matrix-by-dims 239 222 10)
             :flow-layers        {"Altitude" (case elev-type
                                               :orig     @tanzania-elevation-layer
                                               :filled   @tanzania-elevation-filled-layer
                                               :averaged @tanzania-elevation-averaged-layer
                                               :crazy    (make-palindrome-by-dims 239 222 elev-layer-init))
                                  "River"    @tanzania-water-layer}
             :source-threshold   5.0
             :sink-threshold     2.0
             :use-threshold      9.0
             :trans-threshold    1.0
             :cell-width         100.0
             :cell-height        100.0
             :rv-max-states      10
             :downscaling-factor 1
             :source-type        :finite
             :sink-type          :finite
             :use-type           :finite
             :benefit-type       :rival
             :value-type         value-type
             :result-type        :java-hashmap
             :flow-model         "SurfaceWaterMovement"
             :animation?         true}))
