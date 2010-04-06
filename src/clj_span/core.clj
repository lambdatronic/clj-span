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

(ns clj-span.core
  (:gen-class)
  (:import (java.io File))
  (:use [clojure.set :as set :only (difference)]
	[clojure.contrib.duck-streams :only (spit)]))

(def #^{:private true} usage-message
     (str
      "Usage: java -cp clj-span.jar:clojure.jar:clojure-contrib.jar clj_span.core \\ \n"
      "            -source-layer     <filepath> \\ \n"
      "            -sink-layer       <filepath> \\ \n"
      "            -use-layer        <filepath> \\ \n"
      "            -source-threshold <double> \\ \n"
      "            -sink-threshold   <double> \\ \n"
      "            -use-threshold    <double> \\ \n"
      "            -trans-threshold  <double> \\ \n"
      "            -rv-max-states    <integer> \\ \n"
      "            -sink-type        <absolute|relative> \\ \n"
      "            -use-type         <absolute|relative> \\ \n"
      "            -benefit-type     <rival|non-rival> \\ \n"
      "            -flow-model       <line-of-sight|proximity|carbon|hydrosheds>\n"))

(defmulti #^{:private true} print-usage (fn [error-type extra-info] error-type))

(defmethod print-usage :args-not-even [_ extra-info]
  (println (str "\nError: The number of input arguments must be even.\n\n" usage-message)))

(defmethod print-usage :param-errors [_ extra-info]
  (let [error-message (apply str (interpose "\t\n" extra-info))]
    (println (str "\nError: The parameter values that you entered are incorrect.\n\t" error-message "\n\n" usage-message))))

(def #^{:private true} param-tests
     [["-source-layer"     #(.exists (File. %))        " is not a valid filepath."            ]
      ["-sink-layer"       #(.exists (File. %))        " is not a valid filepath."            ]
      ["-use-layer"        #(.exists (File. %))        " is not a valid filepath."            ]
      ["-source-threshold" #(float?   (read-string %)) " is not a double."                    ]
      ["-sink-threshold"   #(float?   (read-string %)) " is not a double."                    ]
      ["-use-threshold"    #(float?   (read-string %)) " is not a double."                    ]
      ["-trans-threshold"  #(float?   (read-string %)) " is not a double."                    ]
      ["-rv-max-states"    #(integer? (read-string %)) " is not an integer."                  ]
      ["-sink-type"        #{"absolute" "relative"}    " must be one of absolute or relative."]
      ["-use-type"         #{"absolute" "relative"}    " must be one of absolute or relative." ]
      ["-benefit-type"     #{"rival" "non-rival"}      " must be one of rival or non-rival"   ]
      ["-flow-model"       #{"line-of-sight" "proximity" "carbon" "hydrosheds"} " must be one of line-of-sight, proximity, carbon, or hydrosheds."]])

(defn- valid-params?
  [params]
  (let [errors (remove nil?
		       (map (fn [[key test? error-msg]]
			      (if-let [val (params key)]
				(if (not (test? val)) (str val error-msg))
				(str "No value provided for " key)))
			    param-tests))
	input-keys   (set (keys params))
	valid-keys   (set (map first param-tests))
	invalid-keys (set/difference input-keys valid-keys)
	errors (concat errors (map #(str % " is not a valid parameter name.") invalid-keys))]
    (if (empty? errors)
      true
      (print-usage :param-errors errors))))

(defn -main
  [& args]
  (if (odd? (count args))
    (print-usage :args-not-even nil)
    (let [input-params (into {} (map vec (partition 2 args)))]
      (when (valid-params? input-params)
	(println "\nCongratulations! Everything was input correctly!\n")
	(doseq [[key _ _] param-tests] (println (find input-params key)))))))

(defn generate-layer-at-random
  [filename rows cols type]
  {:pre [(#{:discrete :continuous} type)]}
  (binding [*print-dup* true]
    (spit filename
	  (let [meta (if (= type :discrete)
		       {:type :discrete-distribution}
		       {:type :continuous-distribution})]
	    (vec (for [_ (range rows)]
		   (vec (for [_ (range cols)]
			  (with-meta {(rand 100.0) 1} meta)))))))))

(defn generate-layer-from-ascii-grid
  [filename]
  (let [lines  (read-lines filename)
	rows (Integer/parseInt (second (re-find #"^NROWS\s+(\d+)" (first  lines))))
	cols (Integer/parseInt (second (re-find #"^NCOLS\s+(\d+)" (second lines))))
	data (drop-while #(re-find #"^[^\d].*" %) lines)]
    (comment "...process the data...")))

(defn load-layer [filename] (read-string (slurp filename)))

;;(into {} (map (fn [[k v]] [(keyword (subs k 1)) v])
