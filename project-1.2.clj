(defproject org.clojars.lambdatronic/clj-span "1.0.0-alpha"
  :description "clj-span is an implementation of the Service Path
                Attribution Networks (Springer LNCS 2010 - Johnson et
                al.) framework for Ecosystem Service Assessment."
  :dependencies     [[clojure "1.2.0"]
                     [clojure-contrib "1.2.0"]]
  :dev-dependencies [[swank-clojure "1.4.0-SNAPSHOT"]
                     [clojure-source "1.2.0"]
                     [lein-clojars "0.6.0"]
                     [org.clojars.rayne/autodoc "0.8.0-SNAPSHOT"]]
  :jvm-opts         ["-agentlib:jdwp=transport=dt_socket,server=y,suspend=n"]
  :main             clj-span.commandline)

;; For more options to defproject, see:
;;   https://github.com/technomancy/leiningen/blob/master/sample.project.clj
