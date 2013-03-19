(ns clj-span.thinklab-monitor
  (:import (org.integratedmodelling.thinklab.api.listeners IMonitor)))

(defn monitor-info [^IMonitor monitor msg]
  (if monitor (.info monitor msg "SPAN")))

(defmacro with-error-monitor [monitor & body]
  `(try ~@body (catch Exception e# (.error ^IMonitor ~monitor e#))))
