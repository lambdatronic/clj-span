/*********************************************************
 ** How to use clj-span.core/java-span-bridge from Java **
 ** Author: Gary W. Johnson (lambdatronic@gmail.com)    **
 ** Date: 2012-11-13                                    **
 *********************************************************/

import clj-span.core;
import java.util.HashMap;

// Create the source, sink, and use layers as 2D arrays. All layers
// must have the same number of rows and columns. Only sinkLayer may
// be null.

// If you are passing in a deterministic layer, simply represent it as
// a 2D double array like so:
double[][] sourceLayer = ...;
double[][] sinkLayer = ...;
double[][] useLayer = ...;

// If you are passing in a probabilistic layer, represent it as a
// HashMap<String,Object> with keys "bounds" and "probs". The "bounds"
// value should be a double array containing the breakpoints which
// divide the state space into discrete ranges. The "probs" value
// should be a 3D double array containing the probability densities
// associated with these ranges in each cell in the layer. That is,
// with indeces i,j,k into this array, i is the row value, j is the
// column value, and k is the state range. The sum of the probability
// densities for all k at each location i,j should equal 1. Note that
// the length of the "bounds" array should be one larger than that of
// the 3rd level of the "probs" array.
HashMap<String,Object> probLayer = new HashMap<String,Object>();
double[] bounds = ...;
double[][][] probs = ...;
probLayer.put("bounds", bounds);
probLayer.put("probs", probs);

// Create the routing layers as 2D double arrays. All layers must have
// the same number of rows and columns (which must also match those of
// the source, sink, and use layers). All values in these arrays must
// be greater than or equal to 0.
double[][] altitudeLayer = ...;
double[][] streamsLayer = ...;

// Pack the routing layers into a HashMap by concept name. If no
// routing layers are needed, simply create an empty HashMap.
HashMap<String,double[][]> = new HashMap<String,double[][]>();
flowLayers.put("Altitude", altitudeLayer);
flowLayers.put("River", streamsLayer);

// Set the source, sink, and use thresholds in the same units as their
// corresponding layers. All values must be doubles greater than or
// equal to 0. During the input layer preprocessing phase, any values
// in these layers below their thresholds will be set to 0. If the
// sink layer is null, then the sink threshold may also be null.
double sourceThreshold = ...;
double sinkThreshold = ...;
double useThreshold = ...;

// Set the transition threshold in the same units as the source layer.
// During the flow simulation, any service carrier agents whose weight
// falls below this value will be removed from the simulation. This
// value must be strictly greater than 0. If set to 0, some flow
// models may never terminate!
double transThreshold = ...;

// Set the cell width and cell height in meters. These values must be
// strictly greater than 0.
double cellWidth = ...;
double cellHeight = ...;

// Set the rv max states value, which determines the maximum number of
// states which should be retained in a discrete random variable
// computed as an arithmetic function of two other discrete random
// variables. For example: X * Y = Z. If X has 3 states and Y has 5
// states, then Z will have up to 15 states. If rv max states is set
// to 10, then Z will be automatically resampled from 15 states to 10
// states as it is computed. Note that a low rv max states value will
// decrease the precision of our results substantially over the course
// of many such arithmetic operations. This controls the behavior of
// all source, sink, and use calculations during the flow model
// simulations. The rv max states value must always be expressed as an
// integer greater than or equal to 1. Note that rv max states will
// only be used if value-type="randvars" (see below). For any other
// values of value-type (e.g., "varprop"), rv max states will be
// ignored. Regardless, this value must be included in the SPAN
// parameters map or an error will be thrown.
int rvMaxStates = ...;

// The downscaling factor is used to reduce the resolution of the
// input source, sink, and use layers prior to running the flow
// simulation on them. The new resolution is determined with the
// following formulas:
//
//   newRows = floor(oldRows/downscalingFactor)
//   newCols = floor(oldCols/downscalingFactor)
//
// After the flow simulation completes, the result maps are resampled
// back to the original resolution of the input layers. This value
// must always be greater than or equal to 1. If set to 1, no
// resampling will occur.
double downscalingFactor = ...;

// Set the source, sink, and use types to be either "finite" or
// "infinite" to indicate whether these resources may be exhausted
// during the flow simulation. Here are some definitions:
//
// Finite Source: the total source value received by users must be
//                less than or equal to the full source value
//
// Infinite Source: multiple users may each receive up to the full
//                  source value thus allowing the total use to be
//                  potentially greater than the total source
//
// Finite Sink: the sink has a fixed capacity and saturates as it
//              absorbs the service medium from service carrier agents
//
// Infinite Sink: the sink may repeatedly impact every flow path
//                passing through it with up to its full sink value
//
// Finite Use: the user has a fixed demand or vulnerability and is no
//             longer affected by service medium amounts encountered
//             above this value
//
// Infinite Use: the user will be affected beneficially or
//               detrimentally by any amount of the service medium
//               which it encounters over the course of the flow
//               simulation
//
// Note that if the sink layer and sink threshold are both null, then
// the sink type may also be null.
String sourceType = ...;
String sinkType = ...;
String useType = ...;

// Set the benefit type to be either "rival" or "non-rival",
// indicating whether the user's use of the service medium is
// destructive (rival) or not (non-rival).
String benefitType = ...;

// Set the value type to be either "randvars", "varprop", or
// "numbers". This determines how all source, sink, use, and flow
// values will be represented both in the flow simulation and in the
// final results.
//
// randvars: as discrete distributions of numeric state -> probability
//
// varprop: as pairs of mean and standard deviation
//
// numbers: as deterministic double values
String valueType = ...;

// Choose the flow model to run from the following list:
//
//   "LineOfSight"
//   "Proximity"
//   "CO2Removed"
//   "FloodWaterMovement"
//   "SurfaceWaterMovement"
//   "SedimentTransport"
//   "CoastalStormMovement"
//   "SubsistenceFishAccessibility"
String flowModel = ...;

// Decide whether to run the simple flow animation visualizer. It's
// not very pretty, but sometimes it's informative.
boolean animation = ...;

// Pack all of the SPAN parameters into a HashMap.
HashMap<String,Object> spanParams = new HashMap<String,Object>;
spanParams.put("source-layer", sourceLayer);
spanParams.put("sink-layer", sinkLayer);
spanParams.put("use-layer", useLayer);
spanParams.put("flow-layers", flowLayers);
spanParams.put("source-threshold", sourceThreshold);
spanParams.put("sink-threshold", sinkThreshold);
spanParams.put("use-thresthold", useThreshold);
spanParams.put("trans-threshold", transThreshold);
spanParams.put("cell-width", cellWidth);
spanParams.put("cell-height", cellHeight);
spanParams.put("rv-max-states", rvMaxStates);
spanParams.put("downscaling-factor", downscalingFactor);
spanParams.put("source-type", sourceType);
spanParams.put("sink-type", sinkType);
spanParams.put("use-type", useType);
spanParams.put("benefit-type", benefitType);
spanParams.put("value-type", valueType);
spanParams.put("flow-model", flowModel);
spanParams.put("animation?", animation);

// Call clj-span.core's static java-span-bridge method with these parameters.
HashMap<String,Object> resultMap = clj-span.core.java-span-bridge(spanParams);

// The keys in the result map are as follows:
//
//   "theoretical-source"
//   "inaccessible-source"
//   "possible-source"
//   "blocked-source"
//   "actual-source"
//   "theoretical-sink"
//   "inaccessible-sink"
//   "actual-sink"
//   "theoretical-use"
//   "inaccessible-use"
//   "possible-use"
//   "blocked-use"
//   "actual-use"
//   "possible-flow"
//   "blocked-flow"
//   "actual-flow"
//
// The values in the result-map will depend on whether value-type was
// set to "randvars", "varprop", or "numbers" as described below:
//
// randvars: A 2D HashMap<Double,Double> array with keys representing
//           discrete numeric states and values representing the
//           probability distribution in each cell of the matrix.
//
// varprop: A 2D HashMap<String,Double> array with fields "mean" and
//          "var" containing the mean and standard deviation values in
//          each cell of the matrix.
//
// numbers: A 2D Double array containing the deterministic values in
//          each cell of the matrix.
