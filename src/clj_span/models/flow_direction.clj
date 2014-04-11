(ns clj-span.models.flow-direction
  (:use [clojure.core.matrix]
        [clojure.core.matrix.operators]
        [clj-misc.utils :only [mapmap]]
        [clj-span.thinklab-monitor :only [monitor-info]])
  (:require [clojure.core.reducers :as r]))

(set-current-implementation :vectorz)

(defn on-bounds?
  "Returns true if the point is on an edge row or column."
  [rows cols [i j]]
  (or (= 0 i)
      (= 0 j)
      (= (dec rows) i)
      (= (dec cols) j)))

(defn neighborhood-points
  "Returns a vector of the points in the 3x3 matrix centered on the passed in point."
  [point]
  (+ point [-1 -1] (index-seq-for-shape [3 3])))

;; FIXME: unused
(defn neighborhood-values
  "Returns a 3x3 submatrix of values centered on the passed in point."
  [matrix [i j]]
  (submatrix matrix (dec i) 3 (dec j) 3))

(def flow-offset
  "[[4 3 2]
    [5   1]
    [6 7 8]]"
  {1.0 [0   1]   ; E  (1)
   2.0 [-1  1]   ; NE (2)
   3.0 [-1  0]   ; N  (3)
   4.0 [-1 -1]   ; NW (4)
   5.0 [0  -1]   ; W  (5)
   6.0 [1  -1]   ; SW (6)
   7.0 [1   0]   ; S  (7)
   8.0 [1   1]}) ; SE (8)

(def NO-FLOW-DIRECTION Double/NaN)

(defn downstream-point
  "Returns the neighboring point into which the given point flows."
  [flow-matrix point]
  (let [flow-direction-code (apply mget flow-matrix point)]
    (if (not= flow-direction-code NO-FLOW-DIRECTION)
      (+ point (flow-offset flow-direction-code)))))

(defn upstream-points
  "Returns a vector of points which flow directly into the given
   point. If the point is on the bounds of the flow matrix, it will be
   considered to have no upstream points."
  [flow-matrix rows cols point]
  (if-not (on-bounds? rows cols point)
    (filterv #(if-let [p (downstream-point flow-matrix %)]
                (= point p))
             (neighborhood-points point))))

(defn propagate-unused-flow-backwards
  "Returns a map of {:result-name -> matrix} for all 18 of the SPAN outputs."
  [theoretical-source flow-matrix intermediate-span-outputs]
  (let [inaccessible-source (fill theoretical-source 0.0)
        actual-source       (fill theoretical-source 0.0)
        unused-flow         (fill theoretical-source 0.0)
        downstream-use      (fill theoretical-source 0.0)
        possible-flow       (:possible-flow intermediate-span-outputs)
        outgoing-flow       (:outgoing-flow intermediate-span-outputs)
        actual-use          (:actual-use intermediate-span-outputs)
        rows                (row-count flow-matrix)
        cols                (column-count flow-matrix)]
    (doseq [[i j] (:outlet-points intermediate-span-outputs)]
      (mset! unused-flow i j (mget outgoing-flow i j))
      (loop [open-list  [[i j]]
             closed-set (transient #{})]
        (when-first [[i j] open-list]
          (let [local-upstream-points   (upstream-points flow-matrix rows cols [i j])
                local-source            (mget theoretical-source i j)
                upstream-outgoing-flows (mapv (fn [[i j]] (mget outgoing-flow i j)) local-upstream-points)
                total-input             (reduce + local-source upstream-outgoing-flows)]
            (if (pos? total-input)
              (let [local-weight            (/ local-source total-input)
                    upstream-weights        (/ upstream-outgoing-flows total-input)
                    total-unused-flow       (mget unused-flow i j)
                    local-unused-flow       (* local-weight total-unused-flow)
                    upstream-unused-flows   (* upstream-weights total-unused-flow)
                    total-downstream-use    (+ (mget actual-use i j) (mget downstream-use i j))
                    local-downstream-use    (* local-weight total-downstream-use)
                    upstream-downstream-use (* upstream-weights total-downstream-use)]
                (when (pos? (mget theoretical-source i j))
                  (mset! inaccessible-source i j local-unused-flow)
                  (mset! actual-source i j local-downstream-use))
                (mapv (fn [[i j] uf du]
                        (mset! unused-flow i j uf)
                        (mset! downstream-use i j du))
                      local-upstream-points
                      upstream-unused-flows
                      upstream-downstream-use)
                (recur (concat (remove closed-set local-upstream-points) (rest open-list))
                       (conj! closed-set [i j])))
              (recur (rest open-list)
                     (conj! closed-set [i j])))))))
    (let [inaccessible-source (emap! (fn [i t p] (if (pos? p) i t))
                                     inaccessible-source
                                     theoretical-source
                                     possible-flow)
          possible-source (- theoretical-source inaccessible-source)]
      (assoc (dissoc intermediate-span-outputs :outgoing-flow :outlet-points)
        :inaccessible-source inaccessible-source
        :possible-source     possible-source
        :blocked-source      (- possible-source actual-source)
        :actual-source       actual-source))))

(defn propagate-initial-flow-forwards
  "Returns a map of {:result-name -> matrix} for 14/18 of the SPAN
   outputs plus the outgoing flow matrix and a list of all serviceshed
   outlet points."
  [theoretical-source theoretical-sink theoretical-use flow-matrix ordered-points]
  (let [upstream-production  (fill theoretical-source 0.0)
        upstream-absorption  (fill theoretical-source 0.0)
        upstream-consumption (fill theoretical-source 0.0)
        incoming-flow        (fill theoretical-source 0.0)
        actual-sink          (fill theoretical-source 0.0)
        actual-flow          (fill theoretical-source 0.0)
        actual-use           (fill theoretical-source 0.0)
        outgoing-flow        (fill theoretical-source 0.0)
        rows                 (row-count flow-matrix)
        cols                 (column-count flow-matrix)]
    (doseq [serviceshed-band (sort-by key > ordered-points)]
      (doseq [[i j] (val serviceshed-band)]
        (let [local-upstream-points (upstream-points flow-matrix rows cols [i j])]
          (when (seq local-upstream-points)
            (mset! upstream-production i j
                   (reduce + (r/map (fn [[i j]] (+ (mget upstream-production i j)
                                                   (mget theoretical-source i j)))
                                    local-upstream-points)))
            (when (or (pos? (mget upstream-production i j))
                      (pos? (mget theoretical-source i j)))
              (mset! upstream-absorption i j
                     (reduce + (r/map (fn [[i j]] (+ (mget upstream-absorption i j)
                                                     (mget actual-sink i j)))
                                      local-upstream-points)))
              (mset! upstream-consumption i j
                     (reduce + (r/map (fn [[i j]] (+ (mget upstream-consumption i j)
                                                     (mget actual-use i j)))
                                      local-upstream-points)))
              (mset! incoming-flow i j
                     (reduce + (r/map (fn [[i j]] (mget outgoing-flow i j))
                                      local-upstream-points)))))
          (when (or (pos? (mget upstream-production i j))
                    (pos? (mget theoretical-source i j)))
            (mset! actual-sink i j
                   (min (+ (mget incoming-flow i j)
                           (mget theoretical-source i j))
                        (mget theoretical-sink i j)))
            (mset! actual-flow i j
                   (- (+ (mget incoming-flow i j)
                         (mget theoretical-source i j))
                      (mget actual-sink i j)))
            (mset! actual-use i j
                   (min (mget actual-flow i j)
                        (mget theoretical-use i j)))
            (mset! outgoing-flow i j
                   (- (mget actual-flow i j)
                      (mget actual-use i j)))))))
    (let [possible-flow (+ upstream-production theoretical-source)
          possible-use  (emap min possible-flow theoretical-use)
          blocked-flow  (+ upstream-absorption actual-sink)]
      {:theoretical-source theoretical-source
       :theoretical-sink   theoretical-sink
       :theoretical-use    theoretical-use
       :inaccessible-sink  (- theoretical-sink actual-sink)
       :inaccessible-use   (- theoretical-use possible-use)
       :possible-use       possible-use
       :possible-flow      possible-flow
       :blocked-use        (emap min blocked-flow (- theoretical-use actual-use))
       :blocked-flow       blocked-flow
       :rival-use          (emap min upstream-consumption (- theoretical-use actual-use))
       :rival-flow         upstream-consumption
       :actual-sink        actual-sink
       :actual-use         actual-use
       :actual-flow        actual-flow
       :outgoing-flow      outgoing-flow
       :outlet-points      (ordered-points 0)})))

(defn depth-first-graph-ordering
  "Traverses a graph in depth-first order, returning a map of the node
   values encountered to the number of steps they are from the root
   node."
  [root successors]
  (loop [open-list     [root]
         ordered-nodes (transient {root 0})]
    (if (empty? open-list)
      (persistent! ordered-nodes)
      (let [this-node  (first open-list)
            next-level (inc (ordered-nodes this-node))]
        (if-let [children (seq (remove ordered-nodes (successors this-node)))]
          (recur (concat children (rest open-list))
                 (reduce #(assoc! %1 %2 next-level) ordered-nodes children))
          (recur (rest open-list)
                 ordered-nodes))))))

(defn order-upstream-points
  "Returns a map of {tree-depth -> [point1 point2 ... pointN]} for one serviceshed."
  [flow-matrix outlet-point]
  (let [rows (row-count flow-matrix)
        cols (column-count flow-matrix)
        outlet-distances (depth-first-graph-ordering outlet-point #(upstream-points flow-matrix rows cols %))]
    (group-by outlet-distances (keys outlet-distances))))

(defn has-downstream-user?
  "Returns true if this point has a downstream use point."
  [flow-matrix use-matrix point]
  (if-let [next-point (downstream-point flow-matrix point)]
    (if (pos? (apply mget use-matrix next-point))
      true
      (recur flow-matrix use-matrix next-point))
    false))

(defn find-serviceshed-outlets
  "Returns all use points with no downstream users."
  [flow-matrix use-matrix use-points]
  (remove #(has-downstream-user? flow-matrix use-matrix %) use-points))

(defn order-serviceshed-points
  "Returns a unified map of {tree-depth -> [point1 point2 ... pointN]} for all servicesheds."
  [flow-matrix use-matrix use-points]
  (apply merge-with join
         (for [outlet-point (find-serviceshed-outlets flow-matrix use-matrix use-points)]
           (order-upstream-points flow-matrix outlet-point))))

(defn distribute-flow
  "Applies the SPAN ecosystem service distribution algorithm to its
  inputs, producing the 18 SPAN output matrices according to the
  chosen flow-type."
  [use-points source-matrix sink-matrix use-matrix flow-matrix]
  (->> (order-serviceshed-points flow-matrix use-matrix use-points)
       (propagate-initial-flow-forwards source-matrix sink-matrix use-matrix flow-matrix)
       (propagate-unused-flow-backwards source-matrix flow-matrix)))

(defmulti compute-flow-directions
  "Returns a matrix of encoded arrows pointing from each point to its next downstream point."
  (fn [flow-type routing-matrices use-points] flow-type))

(defmethod compute-flow-directions :default
  [flow-type routing-matrices use-points]
  (throw (ex-info (str "compute-flow-directions is undefined for flow-type " flow-type) {})))

(defmethod compute-flow-directions :surface-water
  "For now, simply return Ferdinando's passed-in FlowDirection layer."
  [flow-type routing-matrices use-points]
  (routing-matrices "FlowDirection"))

(defn merge-span-outputs
  "Combines the 18 SPAN output matrices from multiple runs by summing."
  [merged-span-outputs next-span-output]
  (merge-with += merged-span-outputs next-span-output))

(defn points-where
  "Returns the [i j] coordinates for each point which satisfies pred?."
  [pred? matrix]
  (filterv (fn [point] (pred? (apply mget matrix point)))
           (index-seq matrix)))

(defn run-span-model
  "Produces the 18 SPAN output matrices according to the chosen flow-type."
  [flow-type source-matrix sink-matrix use-matrix routing-matrices]
  (let [use-points (points-where pos? use-matrix)]
    (case flow-type
      ;; for these models, the flow surface is constant for all users
      (:surface-water :sediment :dissolved-nutrients)
      (->> (compute-flow-directions flow-type routing-matrices use-points)
           (distribute-flow use-points source-matrix sink-matrix use-matrix))
      ;; for these models, the flow surface is user-dependent
      (:viewshed :gaussian :trail-system)
      (reduce merge-span-outputs
              (r/map #(->> (compute-flow-directions flow-type routing-matrices [%])
                           (distribute-flow [%] source-matrix sink-matrix use-matrix))
                     use-points)))))

(defmethod distribute-flow! "FlowDirection"
  [{:keys [flow-model source-layer sink-layer use-layer flow-layers monitor]}]
  (let [results (mapmap (fn [label] (keyword (str (name label) "-layer")))
                        identity
                        (run-span-model :surface-water
                                        (matrix source-layer)
                                        (matrix sink-layer)
                                        (matrix use-layer)
                                        (mapmap identity matrix flow-layers)))]
    (monitor-info monitor (str "completed FlowDirection simulation successfully"))
    results))
