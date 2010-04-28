;;; Copyright 2009 Gary Johnson
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

(ns clj-span.sandbox)

;;; This is a multipart issue: 
;;;
;;;  - Factor SPAN out of ARIES/Thinklab to build and run as an independent project (clj-span.jar). 
;;;  - Re-envision map/observation preprocessing as a series of transformations on the input data 
;;;  - Use bottom-up programming to build a declarative agent-based modelling API to replace the current flow model system 
;;;  - Port existing models to new agent-based framework (this changes their concurrency story) 
;;;  - Let carriers move from source or use points and travel to the other (think recreation) 
;;;  - Allow agents to acquire more weight as they travel (or carry multiple weights: i.e. quantity + quality) 
;;;  - Allow multiple beneficiary types (rival and non-rival) and multiple sink types in a single model run 
;;;  - Complete algorithm optimizations (see dependent issue) 
;;;  - Update documentation (README, comments, API docs) 
;;;  - Add temporal dynamics (see dependent issue) 
;;;
;;; What I need is a new framework to express the SPAN dynamics,
;;; starting from scratch.  Here how it will work:
;;;
;;; 1) The jar is loaded by a JVM, making all the namespaces available
;;;
;;; 2) A top-level main function is called (clj-span.core/main) with a
;;; set of input parameters.  These describe the incoming data sources
;;; and the flow parameters that are exposed to the user.  This
;;; function can be called from the command line or by another
;;; Java/Clojure library.  The input parameters will allow the user to
;;; state whether information is coming in as GIS layers or via
;;; Observations from Thinklab.  Random world generation should also
;;; be allowed for testing purposes.
;;;
;;; 3) A set of data transformers will be applied to massage the input
;;; types into the necessary inputs for the SPAN model (in the form of
;;; a location network).  This is essentially the world generation
;;; phase.
;;;
;;; 4) The agents are constructed and initialized in each source or
;;; use location (this should be an input param).  These agents have a
;;; service description vector (e.g. <quantity,quality>) and a route
;;; description (preferably bitpacked for space efficiency).  Both
;;; will need to be in mutable refs or atoms.
;;;
;;; 5) The behavior of the carrier agents and the source, sink, and
;;; use locations will be described by declarative rules.  A language
;;; and parser for these must be devised.  Multiple types of sinks and
;;; rival/non-rival users will be allowed in a single run.
;;;
;;; 6) The movement (and decay) rules are applied to all agents in one
;;; pass (this is the first timestep).  This is repeated over and over
;;; again until all the carrier agents' weights have dropped below the
;;; transition threshold.  Weights may increase due to some rule
;;; effects.
;;;
;;; 7) The <weight,route> pairs stored on the sink and use locations
;;; are collected into a tripartite graph structure for analysis and
;;; storage of the results in a compact format.
;;;
;;; 8) Map results are made available via the analyzer API.  These may
;;; be called on the tripartite graph directly by other Java/Clojure
;;; code or can be requested at the beginning of the model run in the
;;; input parameters.  The output format should be specified as either
;;; GIS layers, Thinklab Observations, or something else useful to the
;;; end user.

