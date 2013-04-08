(ns clj-span.elevation-correction
  (:use clj-misc.matrix-ops :reload-all ))

(defn average [sequence]
  (let [length (count sequence)]
    (if (zero? length)
      0
      (/ (reduce + sequence) length)))
)



(defn neighborhood-values
	"Returns the values of the 8 neighboring cells"
	[layer rows cols id]
		(map #(get-in layer %) (get-neighbors rows cols id)))

		
(defn smooth
	[elevation rows cols id]
	(let 
		[own-elev	(get-in elevation id)]
	(if (on-bounds? rows cols id)
		{:value own-elev :flag 0}
		(let
			 [neighbors-elevs (neighborhood-values elevation rows cols id)
		 	  min-elev		  (reduce min  neighbors-elevs)]
			(cond 
				; (== own-elev min-elev){:value (+ 0.001 own-elev) :flag 1} ; Dont use that option
				; (< own-elev min-elev) {:value (average neighbors-elevs) :flag 1} ; This should converge
				(< own-elev min-elev) {:value min-elev :flag 1}
				:else {:value own-elev :flag 0})))))
			
		

(defn dem-smooth
	[elevation rows cols]
		(loop 	[counter  (* rows cols)
				 layer    elevation]
				(if (> counter 0)
					(let [dem 		 (make-matrix rows cols #(smooth layer rows cols  %))
					  	  dem-flags  (make-matrix rows cols #(:flag (get-in dem %)))
					  	  dem-values (make-matrix rows cols #(:value (get-in dem %)))
						  pits 		 (reduce-matrix + 0 dem-flags)]
					(do (println "pits:" pits)
						(recur  pits dem-values)))
						layer))
						)


		
		
			


