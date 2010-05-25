(defproject org.clojars.lambdatronic/clj-span "1.0.0-alpha"
  :description "clj-span is an implementation of the Service Path
                Attribution Networks (Springer LNCS 2010 - Johnson et
                al.) framework for Ecosystem Service Assessment."
  :dependencies [[org.clojure/clojure "1.0.0"]
		 [org.clojure/clojure-contrib "1.0.0"]]
  :dev-dependencies [[leiningen/lein-swank "1.2.0-SNAPSHOT"]
		     [lein-clojars "0.5.0-SNAPSHOT"]]
  :namespaces   [clj-span.commandline])
