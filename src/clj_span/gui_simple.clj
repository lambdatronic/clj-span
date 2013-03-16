;;; Copyright 2010-2013 Gary Johnson
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

(ns clj-span.gui-simple
  (:import (javax.swing JFrame JLabel JTextField JButton ButtonGroup JRadioButton)
           (java.awt.event ActionListener)
           (java.awt GridLayout)))

(defn init []
  "Constructs a JFrame containing numerous widgets for controlling the
   SPAN system.  Returns the JFrame object."
  (let [source-layer-text  (JTextField.)
        sink-layer-text    (JTextField.)
        use-layer-text     (JTextField.)
        flow-layer-text    (JTextField.)
        source-layer-label (JLabel. "Source Layer")
        sink-layer-label   (JLabel. "Sink Layer")
        use-layer-label    (JLabel. "Use Layer")
        flow-layer-label   (JLabel. "Flow Layer")
	
        source-thresh-text  (JTextField.)
        sink-thresh-text    (JTextField.)
        use-thresh-text     (JTextField.)
        trans-thresh-text   (JTextField.)
        source-thresh-label (JLabel. "Source Threshold")
        sink-thresh-label   (JLabel. "Sink Threshold")
        use-thresh-label    (JLabel. "Use Threshold")
        trans-thresh-label  (JLabel. "Trans Threshold")
	
        rv-max-states-text       (JTextField.)
        downscaling-factor-text  (JTextField.)
        rv-max-states-label      (JLabel. "RV Max States")
        downscaling-factor-label (JLabel. "Downscaling Factor")
	
        sink-type-label       (JLabel. "Sink Type")
        sink-absolute-button  (JRadioButton. "Absolute" false)
        sink-relative-button  (JRadioButton. "Relative" false)
        sink-type-buttongroup (doto (ButtonGroup.)
                                (.add sink-absolute-button)
                                (.add sink-relative-button))
        
        use-type-label       (JLabel. "Use Type")
        use-absolute-button  (JRadioButton. "Absolute" false)
        use-relative-button  (JRadioButton. "Relative" false)
        use-type-buttongroup (doto (ButtonGroup.)
                               (.add use-absolute-button)
                               (.add use-relative-button))
        
        benefit-type-label       (JLabel. "Benefit Type")
        benefit-rival-button     (JRadioButton. "Rival" false)
        benefit-non-button       (JRadioButton. "Non-Rival" false)
        benefit-type-buttongroup (doto (ButtonGroup.)
                                   (.add benefit-rival-button)
                                   (.add benefit-non-button))
        
        flow-model-label        (JLabel. "Flow Model")
        flow-lineofsight-button (JRadioButton. "Line of Sight" false)
        flow-proximity-button   (JRadioButton. "Proximity" false)
        flow-carbon-button      (JRadioButton. "Carbon" false)
        flow-sediment-button    (JRadioButton. "Sediment" false)
        flow-model-buttongroup  (doto (ButtonGroup.)
                                  (.add flow-lineofsight-button)
                                  (.add flow-proximity-button)
                                  (.add flow-carbon-button)
                                  (.add flow-sediment-button))
	
        engage-button (doto (JButton. "Engage")
                        (.addActionListener
                         (proxy [ActionListener] []
                           (actionPerformed [evt]
                                            (let [c (Double/parseDouble (.getText source-layer-text))]
                                              (.setText source-layer-label
                                                        (str (+ 32 (* 1.8 c)) " Fahrenheit")))))))]

    (doto (JFrame. "clj-span-gui")
      ;;(.setDefaultCloseOperation (JFrame/EXIT_ON_CLOSE))
      (.setLayout (GridLayout. 2 2 3 3))
      (.add source-layer-label)
      (.add source-layer-text)
      (.add sink-layer-label)
      (.add sink-layer-text)
      (.add use-layer-label)
      (.add use-layer-text)
      (.add flow-layer-label)
      (.add flow-layer-text)
      (.add source-thresh-label)
      (.add source-thresh-text)

      (.add sink-thresh-label)
      (.add sink-thresh-text)
      (.add use-thresh-label)
      (.add use-thresh-text)
      (.add trans-thresh-label)
      (.add trans-thresh-text)

      (.add rv-max-states-label)
      (.add rv-max-states-text)
      (.add downscaling-factor-label)
      (.add downscaling-factor-text)

      (.add sink-type-label)
      (.add sink-absolute-button)
      (.add sink-relative-button)
    
      (.add use-type-label)
      (.add use-absolute-button)
      (.add use-relative-button)

      (.add benefit-type-label)
      (.add benefit-rival-button)
      (.add benefit-non-button)

      (.add flow-model-label)
      (.add flow-lineofsight-button)
      (.add flow-proximity-button)
      (.add flow-carbon-button)
      (.add flow-sediment-button)

      (.add engage-button)

      (.setVisible true))))

(init)
