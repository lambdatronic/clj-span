(defproject clj-span/clj-span "1.0.0-SNAPSHOT"
  :description
"clj-span - SPAN models for Ecosystem Service Assessment
Copyright 2009-2013 Gary W. Johnson (gjohnson@green-compass.org)
----------------------------------------------------------------------
This application provides a suite of spatial models that simulate the
flow of ecosystem services from the landscapes which provide them to
the people who receive them in a region determined by the user's input
maps. This implementation is based on the paper 'Service Path
Attribution Networks (SPANs): Spatially Quantifying the Flow of
Ecosystem Services from Landscapes to People' (Springer LNCS 2010 -
Johnson et al.)
----------------------------------------------------------------------"
  :license {:name "GNU General Public License v3",
            :url "http://www.gnu.org/licenses/gpl.html"}
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [meridian/clj-jts "0.0.1"]
                 [integratedmodelling/thinklab-api "1.0.0"]]
  :min-lein-version "2.0.0"
  :warn-on-reflection true
  :aot [clj-span.commandline clj-span.java-span-bridge]
  :main clj-span.commandline
  :repositories [["thinklab" "http://integratedmodelling.org/sw/lib"]])
