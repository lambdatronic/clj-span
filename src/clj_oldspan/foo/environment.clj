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

(ns clj-span.environment)

(defstruct cell :id :neighbors :source :sink :use :flow-features :carrier-cache)

world is 2d grid of cells
each cell has a source, sink, use vector (rate, limit, type)
each timestep

Questions I want to answer with my model M about the system S:

1) How much service does each cell provide to each other cell?
   a) How much does each cell contribute to bringing the carrier to each other cell?
   b) How much does each cell contribute to blocking the carrier from each other cell?
2) Which cells are the greatest carrier producers?
3) Which cells have the greatest capacity for carrier blockage?
4) Which cells have the greatest capacity for carrier use?
5) Where does each cell actually provide service?
6) How much carrier weight does each cell actually block and who does this affect downstream?
7) How much carrier weight does each cell actually rivally use and who does this affect downstream?
8) How much carrier weight does each cell actually nonrivally use?


Theoretical/Inacessible/Possible/Blocked/Actual Source/Sink/Use/Flow

;; Carbon Sequestration

Value 1: Money to Landowners for Carbon Credits
Algorithm:
  1) Calculate Source (sequestration) value for all cells.
  2) Overlay land parcel map and add up all cells per parcel.
  3) Multiply sequestration value per parcel by $/ton sequestered.
  4) Return $s per parcel

Value 2: CO2 Absorption for Polluters
Algorithm:
  1) Calculate Source (sequestration) value for all cells in study area.
  2) Calculate Use (emissions) value for all cells in study area.
  3) Subtract Use from Source (Source - Use) to get amount either unused or overused.
  4) If Source = Use, we are carbon neutral.
  5) If Source > Use:
    a) Map out (Source - Use)*(Source_i / Source) for each cell i to show the distribution of overproduction.
    b) Map out (Use - Source)*(Use_i / Use) for each cell i to show the distribution of overuse.

;; Aesthetic Viewsheds

Value 1: 