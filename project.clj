(defproject clj-span "1.0.0-SNAPSHOT"
  :description
"clj-span - SPAN models for Ecosystem Service Assessment
Copyright 2009-2012 Gary W. Johnson (gjohnson@green-compass.org)
----------------------------------------------------------------------
This application provides a suite of spatial models that simulate the
flow of ecosystem services from the landscapes which provide them to
the people who receive them in a region determined by the user's input
maps. This implementation is based on the paper 'Service Path
Attribution Networks (SPANs): Spatially Quantifying the Flow of
Ecosystem Services from Landscapes to People' (Springer LNCS 2010 -
Johnson et al.)
----------------------------------------------------------------------"
  :dependencies     [[org.clojure/clojure "1.3.0"]
                     [incanter/incanter-core      "1.2.4"]
                     [incanter/incanter-charts    "1.2.4"]]
  :dev-dependencies [[clj-ns-browser "1.1.0"]] ; to use: (use 'clj-ns-browser.sdoc) (sdoc)
  :license          {:name "GNU General Public License v3"
                     :url "http://www.gnu.org/licenses/gpl.html"}
  :checksum-deps    false
  ;; :warn-on-reflection true
  ;; :omit-source true
  :main clj-span.commandline)
