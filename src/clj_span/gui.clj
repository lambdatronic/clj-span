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
;;; This namespace defines functions for displaying matrices as color
;;; images. The run-animation and end-animation functions provide a
;;; somewhat ad-hoc toolkit for simple animations.

(ns clj-span.gui
  (:use [clojure.java.io     :as io]
        [clj-misc.utils      :only (&)]
        [clj-misc.matrix-ops :only (get-rows
                                    get-cols
                                    map-matrix
                                    make-matrix
                                    matrix-min
                                    matrix-max
                                    reduce-matrix
                                    normalize-matrix)])
  (:require (clj-misc [numbers :as nb] [varprop :as vp] [randvars :as rv]))
  (:import (java.awt Color Graphics Dimension)
           (java.awt.image BufferedImage)
           (javax.swing JPanel JFrame)
           (javax.imageio ImageIO)
           (java.io IOException)))

(defn fill-cell [^Graphics g x y scale color]
  (doto g
    (.setColor color)
    (.fillRect (* x scale) (* y scale) scale scale)))

;; (defn get-cell-color [mean stdev]
;;   (let [r (float mean)
;;         g (float (- 1.0 mean))
;;         b (float 0.0)
;;         a (float (- 1.0 stdev))]
;;     (Color. r g b a)))

(defn get-cell-color [^double percentage] ; [0-1]
  (let [h (float (- 0.7 (* percentage 0.7))) ; blue to red
        s (float 1.0)
        b (float 1.0)]
    (Color/getHSBColor h s b)))

(def ^:dynamic *legend-color-height* 20) ; pixels
(def ^:dynamic *legend-text-height*  10) ; pixels
(def ^:dynamic *legend-padding*       5) ; pixels

(defn render-ref-layer [^Graphics g ref-layer scale x-dim y-dim img-width img-height legend-max deref-val]
  (let [img             (BufferedImage. img-width img-height BufferedImage/TYPE_INT_ARGB)
        bg              (.getGraphics img)
        legend-top      (+ (* scale y-dim) *legend-padding*)
        legend-width    (- img-width (* 2 *legend-padding*))]
    ;; Update legend-max globally to the largest value seen thus far
    (swap! legend-max max (reduce-matrix (fn [x-max x] (max x-max (deref-val x))) 0.0 ref-layer))
    ;; Set background color to white
    (doto bg
      (.setColor Color/WHITE)
      (.fillRect 0 0 img-width img-height))
    ;; Draw map image
    (doseq [x (range x-dim)]
      (doseq [y (range y-dim)]
        (let [cell-val (deref-val (get-in ref-layer [y x]))]
          (if-not (zero? cell-val)
            (fill-cell bg x (- y-dim y 1) scale (get-cell-color (/ cell-val @legend-max)))))))
    ;; Draw color legend
    (doseq [x (range *legend-padding* (- img-width *legend-padding*))] ; add whitespace padding on left and right of legend
      (let [cell-color (get-cell-color (/ (- x *legend-padding*) legend-width))] ; ranges from [0-1]
        (doseq [y (range *legend-color-height*)]
          (fill-cell bg x (+ legend-top y) 1 cell-color))))
    ;; Add legend text
    (let [metrics        (.getFontMetrics bg)
          min-val-string "Min: 0.0"
          min-val-width  (.stringWidth metrics min-val-string)
          max-val-string (format "Max: %.1f" @legend-max)
          max-val-width  (.stringWidth metrics max-val-string)]
      (doto bg
        (.setColor (get-cell-color 0.0))
        (.drawString min-val-string *legend-padding* (- img-height *legend-padding*))
        (.setColor (get-cell-color 1.0))
        (.drawString max-val-string (- img-width *legend-padding* max-val-width) (- img-height *legend-padding*))))
    ;; Draw new BufferedImage and dispose of Graphics context.
    (.drawImage g img 0 0 nil)
    (.dispose bg)))

(defn render-normalized [layer scale x-dim y-dim rv-to-number] ;; SLLLOOOOOOWWWWWWWWW
  (let [numeric-layer    (map-matrix rv-to-number layer)
        min-layer-value  (matrix-min numeric-layer 0.0)
        max-layer-value  (matrix-max numeric-layer)
        normalized-layer (normalize-matrix numeric-layer)
        img              (BufferedImage. (* scale x-dim)
                                         (+ (* scale y-dim) *legend-color-height* *legend-text-height* (* *legend-padding* 3))
                                         BufferedImage/TYPE_INT_ARGB)
        bg               (.getGraphics img)]
    ;; Set background color to white
    (doto bg
      (.setColor Color/WHITE)
      (.fillRect 0 0 (.getWidth img) (.getHeight img)))
    ;; Draw map image
    (doseq [x (range x-dim)]
      (doseq [y (range y-dim)]
        (let [percentage (get-in normalized-layer [y x])]
          (if-not (zero? percentage)
            (fill-cell bg x (- y-dim y 1) scale (get-cell-color percentage))))))
    ;; Draw color legend
    (doseq [x (range *legend-padding* (- (.getWidth img) *legend-padding*))] ; add whitespace padding on left and right of legend
      (let [cell-color (get-cell-color (/ (- x *legend-padding*) (- (.getWidth img) (* 2 *legend-padding*))))] ; ranges from [0-1]
        (doseq [y (range *legend-color-height*)]
          (fill-cell bg x (+ (* scale y-dim) *legend-padding* y) 1 cell-color))))
    ;; Add legend text
    (let [metrics        (.getFontMetrics bg)
          min-val-string (format "Min: %.1f" min-layer-value)
          min-val-width  (.stringWidth metrics min-val-string)
          max-val-string (format "Max: %.1f" max-layer-value)
          max-val-width  (.stringWidth metrics max-val-string)]
      (doto bg
        (.setColor (get-cell-color (if-not (zero? max-layer-value) (/ min-layer-value max-layer-value) 1.0)))
        (.drawString min-val-string *legend-padding* (- (.getHeight img) *legend-padding*))
        (.setColor (get-cell-color 1.0))
        (.drawString max-val-string (- (.getWidth img) *legend-padding* max-val-width) (- (.getHeight img) *legend-padding*))))
    ;; Dispose of Graphics context and return BufferedImage
    (.dispose bg)
    img))

(defn write-layer-to-file [dirname file-prefix layer scale value-type]
  (let [[rv-mean rv-stdev] (case value-type
                             :numbers  [nb/rv-mean nb/rv-stdev]
                             :varprop  [vp/rv-mean vp/rv-stdev]
                             :randvars [rv/rv-mean rv/rv-stdev])
        y-dim (get-rows layer)
        x-dim (get-cols layer)]
    (let [outfile (io/file dirname (str file-prefix "-mean.png"))]
      (try (ImageIO/write (render-normalized layer scale x-dim y-dim rv-mean) "png" outfile)
           (catch IOException e (println "Failed to write mean layer for" file-prefix "to file" (.getName outfile)))))
    (if-not (= value-type :numbers)
      (let [outfile (io/file dirname (str file-prefix "-stdev.png"))]
        (try (ImageIO/write (render-normalized layer scale x-dim y-dim rv-stdev) "png" outfile)
             (catch IOException e (println "Failed to write stdev layer for" file-prefix "to file" (.getName outfile))))))))

(defn draw-layer [title layer scale value-type]
  (let [[rv-mean rv-stdev] (case value-type
                             :numbers  [nb/rv-mean nb/rv-stdev]
                             :varprop  [vp/rv-mean vp/rv-stdev]
                             :randvars [rv/rv-mean rv/rv-stdev])
        y-dim       (get-rows layer)
        x-dim       (get-cols layer)
        img-width   (* scale x-dim)
        img-height  (+ (* scale y-dim) *legend-color-height* *legend-text-height* (* *legend-padding* 3))
        mean-panel  (doto (proxy [JPanel] [] (paint [g] (let [img (render-normalized layer scale x-dim y-dim rv-mean)]
                                                          (.drawImage g img 0 0 nil))))
                      (.setPreferredSize (Dimension. img-width img-height)))
        stdev-panel (doto (proxy [JPanel] [] (paint [g] (let [img (render-normalized layer scale x-dim y-dim rv-stdev)]
                                                          (.drawImage g img 0 0 nil))))
                      (.setPreferredSize (Dimension. img-width img-height)))]
    (doto (JFrame. (str title " Mean")) (.add mean-panel) .pack .show)
    (if-not (= value-type :numbers)
      (doto (JFrame. (str title " Standard Deviation")) (.add stdev-panel) .pack .show))
    [mean-panel stdev-panel]))

(defn draw-ref-layer [title ref-layer scale legend-max value-type]
  (let [[rv-mean rv-stdev] (case value-type
                             :numbers  [nb/rv-mean nb/rv-stdev]
                             :varprop  [vp/rv-mean vp/rv-stdev]
                             :randvars [rv/rv-mean rv/rv-stdev])
        deref-mean  (& rv-mean deref)
        deref-stdev (& rv-stdev deref)
        y-dim       (get-rows ref-layer)
        x-dim       (get-cols ref-layer)
        img-width   (* scale x-dim)
        img-height  (+ (* scale y-dim) *legend-color-height* *legend-text-height* (* *legend-padding* 3))
        mean-panel  (doto (proxy [JPanel] [] (paint [^Graphics g] (render-ref-layer g ref-layer scale x-dim y-dim
                                                                                    img-width img-height
                                                                                    legend-max deref-mean)))
                      (.setPreferredSize (Dimension. img-width img-height)))
        stdev-panel (if-not (= value-type :numbers)
                      (doto (proxy [JPanel] [] (paint [^Graphics g] (render-ref-layer g ref-layer scale x-dim y-dim
                                                                                      img-width img-height
                                                                                      legend-max deref-stdev)))
                        (.setPreferredSize (Dimension. img-width img-height))))]
    (doto (JFrame. (str title " Mean")) (.add mean-panel) .pack .show)
    (if-not (= value-type :numbers)
      (doto (JFrame. (str title " Standard Deviation")) (.add stdev-panel) .pack .show))
    [mean-panel stdev-panel]))

(defn draw-points [ids scale value-type]
  (let [[_+ _0_]    (case value-type
                      :numbers  [nb/_+ nb/_0_]
                      :varprop  [vp/_+ vp/_0_]
                      :randvars [rv/_+ rv/_0_])
        max-y       (apply max (map first  ids))
        max-x       (apply max (map second ids))
        point-vals  (zipmap ids (repeat (_+ _0_ 1.0)))
        point-layer (make-matrix (inc max-y) (inc max-x) #(get point-vals % _0_))]
    (draw-layer "Points" point-layer scale value-type)))

(def ^:dynamic *animation-sleep-ms* 1000)

(def animation-running? (atom false))

(defn run-animation [panels]
  (when @animation-running?
    (send-off *agent* run-animation)
    (doseq [^JPanel panel panels]
      (.repaint panel))
    (Thread/sleep *animation-sleep-ms*)
    panels))
