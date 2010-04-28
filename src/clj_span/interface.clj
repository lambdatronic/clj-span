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
;;; This namespace defines the text-based menu interface for viewing
;;; the results of a SPAN model run.

(ns clj-span.interface
  (:use	[clj-misc.matrix-ops :only (print-matrix get-rows get-cols)]
	[clj-span.analyzer   :only (theoretical-source
				    theoretical-sink
				    theoretical-use
				    inaccessible-source
				    inaccessible-sink
				    inaccessible-use
				    possible-flow
				    possible-source
				    possible-sink
				    possible-use
				    blocked-flow
				    blocked-source
				    blocked-sink
				    blocked-use
				    actual-flow
				    actual-source
				    actual-sink
				    actual-use)]))

(defn- select-location
  "Prompts for coords and returns the corresponding location object."
  [locations rows cols]
  (loop []
    (printf "%nInput location coords%n")
    (let [coords [(do (printf "Row [0-%d]: " (dec rows)) (flush) (read))
		  (do (printf "Col [0-%d]: " (dec cols)) (flush) (read))]
	  location (some #(and (= (:id %) coords) %) locations)]
      (if location
	location
	(do (printf "No location at %s. Enter another selection.%n" coords)
	    (recur))))))

(defn- select-menu-option
  "Prompts the user with a menu of choices and returns the label
   corresponding to their selection."
  [prompt-list]
  (let [prompts     (vec prompt-list)
	num-prompts (count prompts)]
    (loop []
      (printf "%nOptions Menu:%n")
      (dotimes [i num-prompts]
	(printf " %d) %s%n" (inc i) (prompts i)))
      (print "Choice: ")
      (flush)
      (let [choice (read)]
	(if (and (integer? choice) (> choice 0) (<= choice num-prompts))
	  (prompts (dec choice))
	  (do (println "Invalid selection. Please choose a number from the menu.")
	      (recur)))))))

(defn- select-map-by-feature
  "Prompts for a feature available in the union of the source, sink,
   use, and flow layers, and returns a map of {[i j] -> value} for the
   one selected, where value is either a double or a probability
   distribution."
  [source-layer sink-layer use-layer flow-layers]
  (let [feature-names    (list* "Source" "Sink" "Use" (keys flow-layers))
	selected-feature (select-menu-option feature-names)
	selected-layer   ((-> flow-layers
			      (assoc "Source" source-layer)
			      (assoc "Sink"   sink-layer)
			      (assoc "Use"    use-layer))
			  selected-feature)]
    (into {} (for [i (range (get-rows source-layer)) j (range (get-cols source-layer)) :let [id [i j]]]
	       [id (get-in selected-layer id)]))))

(defn- view-location-properties
  "Prints a summary of the post-simulation properties of the
   location."
  [location]
  (let [fmt-str (str
		 "%nLocation %s%n"
		 "--------------------%n"
		 "Neighbors:     %s%n"
		 "Source:        %s%n"
		 "Sink:          %s%n"
		 "Use:           %s%n"
		 "Flow Features: %s%n"
		 "Carriers Encountered: %d%n")]
    (printf fmt-str
	    (:id location)
	    (seq (:neighbors location))
	    (:source location)
	    (:sink location)
	    (:use location)
	    (:flow-features location)
	    (count @(:carrier-cache location)))))

(defn- coord-map-to-matrix
  "Renders a map of {[i j] -> value} into a 2D matrix, where value is
   either a double or a probability distribution."
  [rows cols coord-map]
  (let [matrix (make-array (class (val (first coord-map))) rows cols)]
    (doseq [[i j :as key] (keys coord-map)]
	(aset matrix i j (coord-map key)))
    matrix))

(defn show-span-results-menu
  [flow-model source-layer sink-layer use-layer flow-layers locations]
  (let [rows (get-rows source-layer)
	cols (get-cols source-layer)
	menu (array-map
	      "View Theoretical Source"  #(theoretical-source       locations)
	      "View Theoretical Sink"    #(theoretical-sink         locations)
	      "View Theoretical Use"     #(theoretical-use          locations)
	      "View Inaccessible Source" #(inaccessible-source      locations)
	      "View Inaccessible Sink"   #(inaccessible-sink        locations)
	      "View Inaccessible Use"    #(inaccessible-use         locations)
	      "View Possible Flow"       #(possible-flow            locations flow-model)
	      "View Possible Source"     #(possible-source          locations)
	      "View Possible Sink"       #(possible-sink            locations)
	      "View Possible Use"        #(possible-use             locations)
	      "View Blocked Flow"        #(blocked-flow             locations flow-model)
	      "View Blocked Source"      #(blocked-source           locations flow-model)
	      "View Blocked Sink"        #(blocked-sink             locations flow-model)
	      "View Blocked Use"         #(blocked-use              locations flow-model)
	      "View Actual Flow"         #(actual-flow              locations flow-model)
	      "View Actual Source"       #(actual-source            locations flow-model)
	      "View Actual Sink"         #(actual-sink              locations flow-model)
	      "View Actual Use"          #(actual-use               locations flow-model)
	      "View Location Properties" #(view-location-properties (select-location locations rows cols))
	      "View Feature Map"         #(select-map-by-feature    source-layer
								    sink-layer
								    use-layer
								    flow-layers)
	      "Quit"                     nil)
	prompts (keys menu)]
    (println "Flow-Model Results for" flow-model "at resolution" rows "x" cols ":")
    (loop [choice (select-menu-option prompts)]
      (let [action (menu choice)]
	(when (fn? action)
	  (let [coord-map (action)]
	    (when (map? coord-map)
	      (newline)
	      (print-matrix (coord-map-to-matrix rows cols coord-map))
	      (newline)
	      (println "Distinct values:" (count (distinct (vals coord-map)))))
	    (recur (select-menu-option prompts))))))))
