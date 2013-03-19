(ns clj-span.thinklab-monitor
  (:import (org.integratedmodelling.thinklab.api.listeners IMonitor)))

(defn monitor-info [^IMonitor monitor msg]
  (if monitor (.info monitor msg "SPAN")))

(defmacro with-error-monitor [^IMonitor monitor & body]
  `(try ~@body (catch Exception e# (if ~monitor (.error ~monitor e#) (throw e#)))))

;; NOTE: This macro should always be called inside with-error-monitor.
(defmacro with-interrupt-checking [^IMonitor monitor & body]
  `(if (and ~monitor (.isStopped ~monitor))
     (throw (Exception. "Early termination requested by Thinklab IMonitor."))
     (do ~@body)))
