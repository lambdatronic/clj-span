(ns clj-span.elevation-correction
  (:use [clj-misc.matrix-ops :only [is-matrix? get-rows get-cols make-matrix get-neighbors on-bounds?]]))

(defn average
  [sequence]
  (let [length (count sequence)]
    (if (zero? length)
      0
      (/ (reduce + sequence) length))))

(defn neighborhood-values
  "Returns the values of the 8 neighboring cells"
  [elevation-layer rows cols id]
  (map #(get-in elevation-layer %) (get-neighbors rows cols id)))

(defn neighbor-min
  [elevation-layer rows cols id]
  (let [own-elev (get-in elevation-layer id)]
    (if (on-bounds? rows cols id)
      own-elev
      (let [neighbor-elevs (neighborhood-values elevation-layer rows cols id)
            min-elev       (reduce min neighbor-elevs)]
        (if (< own-elev min-elev)
          min-elev
          own-elev)))))

(defn neighbor-avg
  [elevation-layer rows cols id]
  (let [own-elev (get-in elevation-layer id)]
    (if (on-bounds? rows cols id)
      own-elev
      (let [neighbor-elevs (neighborhood-values elevation-layer rows cols id)
            min-elev       (reduce min neighbor-elevs)]
        (if (< own-elev min-elev)
          (average neighbor-elevs)
          own-elev)))))

(defn fill-pits
  [pit-filling-algorithm elevation-layer]
  (let [rows       (get-rows elevation-layer)
        cols       (get-cols elevation-layer)
        filling-fn (case pit-filling-algorithm
                     :neighbor-min #(neighbor-min elevation-layer rows cols %)
                     :neighbor-avg #(neighbor-avg elevation-layer rows cols %))
        new-elevation-layer (make-matrix rows cols filling-fn)]
    (if (not= elevation-layer new-elevation-layer)
      new-elevation-layer)))

(defn dem-smooth
  "Returns a new elevation layer based on the original by applying one
   of the following pit-filling algorithms: :neighbor-min :neighbor-avg."
  [elevation-layer pit-filling-algorithm]
  {:pre [(is-matrix? elevation-layer)
         (contains? #{:neighbor-min :neighbor-avg} pit-filling-algorithm)]}
    (last (take-while is-matrix? (iterate #(fill-pits pit-filling-algorithm %) elevation-layer))))
