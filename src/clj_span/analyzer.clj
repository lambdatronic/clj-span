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
;;; This namespace defines functions for analyzing the location
;;; sequence returned by clj-span.core/simulate-service-flows.  Each
;;; public function may be applied independently of the others to
;;; generate its result matrix.

(ns clj-span.analyzer
  (:use [clj-misc.utils      :only (p)]
        [clj-misc.matrix-ops :only (get-rows get-cols matrix2seq map-matrix make-matrix)])
  (:require (clj-misc [numbers :as nb] [varprop :as vp] [randvars :as rv])))

(defn theoretical-source
  "If source-type is finite, return source-layer. Else return
   source-layer * num-users."
  [{:keys [value-type source-type source-layer use-layer]}]
  (if (= source-type :finite)
    source-layer
    (let [[*_ _0_]  (condp = value-type
                      :numbers  [nb/*_ nb/_0_]
                      :varprop  [vp/*_ vp/_0_]
                      :randvars [rv/*_ rv/_0_])
          num-users (count (remove (p = _0_) (matrix2seq use-layer)))]
      (map-matrix (p *_ num-users) source-layer))))
(def theoretical-source (memoize theoretical-source))

(defn possible-source
  "Returns a matrix of RVs, in which each cell contains the amount of
   its theoretical source that impacts a user along any flow path,
   disregarding the negative effects of sinks and rival users."
  [{:keys [value-type cache-layer]}]
  (let [[_+_ _0_] (condp = value-type
                    :numbers  [nb/_+_ nb/_0_]
                    :varprop  [vp/_+_ vp/_0_]
                    :randvars [rv/_+_ rv/_0_])
        coord-map (apply merge-with _+_ {}
                         (for [cache (remove nil? (matrix2seq cache-layer))
                               {:keys [source-id possible-weight]} cache]
                           {source-id possible-weight}))]
    (make-matrix (get-rows cache-layer) (get-cols cache-layer) #(get coord-map % _0_))))
(def possible-source (memoize possible-source))

(defn actual-source
  "Returns a matrix of RVs, in which each cell contains the amount of
   its theoretical source that impacts a user along any flow path,
   including the negative effects of sinks and rival users."
  [{:keys [value-type cache-layer]}]
  (let [[_+_ _0_] (condp = value-type
                    :numbers  [nb/_+_ nb/_0_]
                    :varprop  [vp/_+_ vp/_0_]
                    :randvars [rv/_+_ rv/_0_])
        coord-map (apply merge-with _+_ {}
                         (for [cache (remove nil? (matrix2seq cache-layer))
                               {:keys [source-id actual-weight]} cache]
                           {source-id actual-weight}))]
    (make-matrix (get-rows cache-layer) (get-cols cache-layer) #(get coord-map % _0_))))
(def actual-source (memoize actual-source))

(defn theoretical-sink
  "If sink-type is finite, return sink-layer. Else return sink-layer
   * max-flowpaths (limited by total-source if source-type is
   finite).  If sink-type is nil, we may assume that there are no
   sinks in this model."
  [{:keys [value-type source-type sink-type source-layer sink-layer use-layer]}]
  (if (nil? sink-type)
    (let [_0_ (condp = value-type
                :numbers  nb/_0_
                :varprop  vp/_0_
                :randvars rv/_0_)]
      (make-matrix (get-rows source-layer) (get-cols source-layer) (constantly _0_)))
    (if (= sink-type :finite)
      sink-layer
      (let [[_+_ _* *_ _min_ _0_] (condp = value-type
                                    :numbers  [nb/_+_ nb/_* nb/*_ nb/_min_ nb/_0_]
                                    :varprop  [vp/_+_ vp/_* vp/*_ vp/_min_ vp/_0_]
                                    :randvars [rv/_+_ rv/_* rv/*_ rv/_min_ rv/_0_])
            num-sources           (count (remove (p = _0_) (matrix2seq source-layer)))
            num-users             (count (remove (p = _0_) (matrix2seq use-layer)))
            max-flowpaths         (* num-sources num-users)
            total-source          (reduce _+_ _0_ (remove (p = _0_) (matrix2seq source-layer)))
            per-sink-limit        (if (source-type :finite)
                                    total-source
                                    (_* total-source num-users))]
        (map-matrix #(if (= _0_ %) _0_ (_min_ (*_ max-flowpaths %) per-sink-limit)) sink-layer)))))
(def theoretical-sink (memoize theoretical-sink))

(defn actual-sink
  "Returns a matrix of RVs, in which each cell contains the fraction
   of its theoretical sink that impacts a user along any flow path."
  [{:keys [value-type cache-layer]}]
  (let [[_+_ _0_] (condp = value-type
                    :numbers  [nb/_+_ nb/_0_]
                    :varprop  [vp/_+_ vp/_0_]
                    :randvars [rv/_+_ rv/_0_])
        coord-map (apply merge-with _+_ {}
                         (for [cache (remove nil? (matrix2seq cache-layer))
                               {:keys [sink-effects]} cache]
                           sink-effects))]
    (make-matrix (get-rows cache-layer) (get-cols cache-layer) #(get coord-map % _0_))))
(def actual-sink (memoize actual-sink))

(defn theoretical-use
  "If use-type is finite, return use-layer. Else return a new layer
   in which all non-zero use values have been replaced with
   total-source (or with total-source divided by num-users if
   source-type is finite)."
  [{:keys [value-type use-type source-layer use-layer]}]
  (if (= use-type :finite)
    use-layer
    (let [[_+_ _0_]    (condp = value-type
                         :numbers  [nb/_+_ nb/_0_]
                         :varprop  [vp/_+_ vp/_0_]
                         :randvars [rv/_+_ rv/_0_])
          total-source (reduce _+_ _0_ (remove (p = _0_) (matrix2seq source-layer)))]
      (map-matrix #(if (not= _0_ %) total-source _0_) use-layer))))
(def theoretical-use (memoize theoretical-use))

(defn possible-use
  "Returns a matrix of RVs, in which each cell contains the amount of
   its theoretical source that impacts a user along any flow path,
   disregarding the negative effects of sinks and rival users."
  [{:keys [value-type cache-layer]}]
  (let [[_+_ _0_] (condp = value-type
                    :numbers  [nb/_+_ nb/_0_]
                    :varprop  [vp/_+_ vp/_0_]
                    :randvars [rv/_+_ rv/_0_])]
    (map-matrix #(reduce _+_ _0_ (map :possible-weight %)) cache-layer)))
(def possible-use (memoize possible-use))

(defn actual-use
  "Returns a matrix of RVs, in which each cell contains the amount of
   its theoretical source that impacts a user along any flow path,
   disregarding the negative effects of sinks and rival users."
  [{:keys [value-type cache-layer]}]
  (let [[_+_ _0_] (condp = value-type
                    :numbers  [nb/_+_ nb/_0_]
                    :varprop  [vp/_+_ vp/_0_]
                    :randvars [rv/_+_ rv/_0_])]
    (map-matrix #(reduce _+_ _0_ (map :actual-weight %)) cache-layer)))
(def actual-use (memoize actual-use))

(defn possible-flow
  [params]
  (:possible-flow-layer params))

(defn actual-flow
  [params]
  (:actual-flow-layer params))

;;(defn- rerun-possible-route
;;  [flow-model {:keys [source-id route possible-weight use-effects]}]
;;  (let [route-ids (rseq (unbitpack-route source-id route))]
;;    (zipmap route-ids
;;            (map (p undecay flow-model)
;;                 (if (empty? use-effects)
;;                   (repeat possible-weight)
;;                   ;;         (reductions
;;                   ;;          #(_+_ %1 (get use-effects %2 _0_))
;;                   ;;          possible-weight
;;                   ;;          route-ids)
;;                   (reduce
;;                    ;;#(conj %1 (_+_ (peek %1) (get use-effects %2 _0_)))
;;                    #(conj %1 (if-let [u (get use-effects %2)] (_+_ (peek %1) u) (peek %1)))
;;                    [possible-weight]
;;                    route-ids))
;;                 (iterate inc 0)))))
;;
;;(defn- rerun-actual-route
;;  [flow-model {:keys [source-id route actual-weight sink-effects use-effects] :or {use-effects {}} :as carrier}]
;;  (if (empty? sink-effects)
;;    (rerun-possible-route flow-model carrier)
;;    (let [route-ids (rseq (unbitpack-route source-id route))]
;;      (zipmap route-ids
;;              (map (p undecay flow-model)
;;                   ;;         (reductions
;;                   ;;          #(reduce _+_ %1 (remove nil? ((juxt sink-effects use-effects) %2)))
;;                   ;;          actual-weight
;;                   ;;          route-ids)
;;                   (reduce
;;                    #(conj %1 (reduce _+_ (peek %1) (remove nil? [(sink-effects %2) (use-effects %2)])))
;;                    ;;          #(conj %1 (reduce _+_ (peek %1) (remove nil? ((juxt sink-effects use-effects) %2))))
;;                    [actual-weight]
;;                    route-ids)
;;                   (iterate inc 0))))))
;;
;;(defn possible-flow
;;  [cache-layer flow-model]
;;  (let [rows (get-rows cache-layer)
;;        cols (get-cols cache-layer)]
;;    (if-let [carriers-with-routes (seq (filter :route (apply concat (matrix2seq cache-layer))))]
;;      (let [coord-map (apply merge-with _+_
;;                             (with-progress-bar (pmap (p rerun-possible-route flow-model) (take 10 carriers-with-routes))))]
;;        (make-matrix rows cols #(get coord-map % _0_)))
;;      (possible-source cache-layer))))
;;(def possible-flow (memoize possible-flow))
;;
;;(defn actual-flow
;;  [cache-layer flow-model]
;;  (let [rows (get-rows cache-layer)
;;        cols (get-cols cache-layer)]
;;    (if-let [carriers-with-routes (seq (filter :route (apply concat (matrix2seq cache-layer))))]
;;      (let [coord-map (apply merge-with _+_
;;                             (with-progress-bar (pmap (p rerun-actual-route flow-model) carriers-with-routes)))]
;;        (make-matrix rows cols #(get coord-map % _0_)))
;;      (actual-source cache-layer))))
;;(def actual-flow (memoize actual-flow))

(defn inaccessible-source
  "Returns a map of {location-id -> inaccessible-source}.
   Inaccessible-source is the amount of the theoretical-source which
   cannot be used by any location either due to propagation decay,
   lack of use capacity, or lack of flow pathways to use locations."
  [{:keys [value-type source-type source-layer use-layer cache-layer]}]
  (let [rv-fn (condp = value-type
                :numbers  nb/rv-fn
                :varprop  vp/rv-fn
                :randvars rv/rv-fn)]
    (map-matrix #(rv-fn '(fn [t p] (max (- t p) 0.0)) %1 %2)
                (theoretical-source {:value-type value-type :source-type source-type :source-layer source-layer :use-layer use-layer})
                (possible-source    {:value-type value-type :cache-layer cache-layer}))))

(defn inaccessible-sink
  "Returns a map of {location-id -> inaccessible-sink}.
   Inaccessible-sink is the amount of the theoretical-sink which
   cannot be utilized by any location either due to propagation decay
   of the asset or lack of flow pathways through the sink locations."
  [{:keys [value-type source-type sink-type source-layer sink-layer use-layer cache-layer]}]
  (let [rv-fn (condp = value-type
                :numbers  nb/rv-fn
                :varprop  vp/rv-fn
                :randvars rv/rv-fn)]
    (map-matrix #(rv-fn '(fn [t a] (max (- t a) 0.0)) %1 %2)
                (theoretical-sink {:value-type value-type :source-type source-type :sink-type sink-type
                                   :source-layer source-layer :sink-layer sink-layer :use-layer use-layer})
                (actual-sink      {:value-type value-type :cache-layer cache-layer}))))

(defn inaccessible-use
  "Returns a map of {location-id -> inaccessible-use}.
   Inaccessible-use is the amount of the theoretical-use which cannot
   be utilized by each location either due to propagation decay of the
   asset or lack of flow pathways to use locations."
  [{:keys [value-type use-type source-layer use-layer cache-layer]}]
  (let [rv-fn (condp = value-type
                :numbers  nb/rv-fn
                :varprop  vp/rv-fn
                :randvars rv/rv-fn)]
    (map-matrix #(rv-fn '(fn [t p] (max (- t p) 0.0)) %1 %2)
                (theoretical-use {:value-type value-type :use-type use-type :source-layer source-layer :use-layer use-layer})
                (possible-use    {:value-type value-type :cache-layer cache-layer}))))

(defn blocked-source
  "Returns a map of {location-id -> blocked-source}.
   Blocked-source is the amount of the possible-source which cannot be
   used by any location due to upstream sinks or uses."
  [{:keys [value-type cache-layer]}]
  (let [rv-fn (condp = value-type
                :numbers  nb/rv-fn
                :varprop  vp/rv-fn
                :randvars rv/rv-fn)]
    (map-matrix #(rv-fn '(fn [p a] (max (- p a) 0.0)) %1 %2)
                (possible-source {:value-type value-type :cache-layer cache-layer})
                (actual-source   {:value-type value-type :cache-layer cache-layer}))))

(defn blocked-use
  "Returns a map of {location-id -> blocked-use}.
   Blocked-use is the amount of the possible-use which cannot be
   realized due to upstream sinks or uses."
  [{:keys [value-type cache-layer]}]
  (let [rv-fn (condp = value-type
                :numbers  nb/rv-fn
                :varprop  vp/rv-fn
                :randvars rv/rv-fn)]
    (map-matrix #(rv-fn '(fn [p a] (max (- p a) 0.0)) %1 %2)
                (possible-use {:value-type value-type :cache-layer cache-layer})
                (actual-use   {:value-type value-type :cache-layer cache-layer}))))

(defn blocked-flow
  "Returns a map of {location-id -> blocked-flow}.
   Blocked-flow is the amount of the possible-flow which cannot be
   realized due to upstream sinks or uses."
  [{:keys [value-type possible-flow-layer actual-flow-layer]}]
  (let [rv-fn (condp = value-type
                :numbers  nb/rv-fn
                :varprop  vp/rv-fn
                :randvars rv/rv-fn)]
    (map-matrix #(rv-fn '(fn [p a] (max (- p a) 0.0)) %1 %2)
                possible-flow-layer
                actual-flow-layer)))
