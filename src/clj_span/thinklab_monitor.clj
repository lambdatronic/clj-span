(ns clj-span.thinklab-monitor
  (:import (org.integratedmodelling.thinklab.api.listeners IMonitor)))

(defn monitor-info [^IMonitor monitor msg]
  (if monitor (.info monitor msg "SPAN")))

(defmacro with-error-monitor [^IMonitor monitor & body]
  `(try ~@body (catch Exception e# (if ~monitor (.error ~monitor e#) (throw e#)))))

(defmacro with-interrupt-checking [^IMonitor monitor & body]
  `(when-not (and ~monitor (.isStopped ~monitor))
     ~@body))
