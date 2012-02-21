{:namespaces
 ({:source-url nil,
   :wiki-url "clj-misc.matrix-ops-api.html",
   :name "clj-misc.matrix-ops",
   :doc nil}
  {:source-url nil,
   :wiki-url "clj-misc.memtest-api.html",
   :name "clj-misc.memtest",
   :doc nil}
  {:source-url nil,
   :wiki-url "clj-misc.point-algebra-api.html",
   :name "clj-misc.point-algebra",
   :doc nil}
  {:source-url nil,
   :wiki-url "clj-misc.randvars-api.html",
   :name "clj-misc.randvars",
   :doc nil}
  {:source-url nil,
   :wiki-url "clj-misc.stats-api.html",
   :name "clj-misc.stats",
   :doc nil}
  {:source-url nil,
   :wiki-url "clj-misc.utils-api.html",
   :name "clj-misc.utils",
   :doc nil}
  {:source-url nil,
   :wiki-url "clj-misc.varprop-api.html",
   :name "clj-misc.varprop",
   :doc nil}
  {:source-url nil,
   :wiki-url "clj-span.agents-api.html",
   :name "clj-span.agents",
   :doc nil}
  {:source-url nil,
   :wiki-url "clj-span.analyzer-api.html",
   :name "clj-span.analyzer",
   :doc nil}
  {:source-url nil,
   :wiki-url "clj-span.aries-span-bridge-api.html",
   :name "clj-span.aries-span-bridge",
   :doc nil}
  {:source-url nil,
   :wiki-url "clj-span.commandline-api.html",
   :name "clj-span.commandline",
   :doc nil}
  {:source-url nil,
   :wiki-url "clj-span.core-api.html",
   :name "clj-span.core",
   :doc nil}
  {:source-url nil,
   :wiki-url "clj-span.gui-api.html",
   :name "clj-span.gui",
   :doc nil}
  {:source-url nil,
   :wiki-url "clj-span.interface-api.html",
   :name "clj-span.interface",
   :doc nil}
  {:source-url nil,
   :wiki-url "clj-span.model-lang-api.html",
   :name "clj-span.model-lang",
   :doc nil}
  {:source-url nil,
   :wiki-url "clj-span.models.carbon-api.html",
   :name "clj-span.models.carbon",
   :doc nil}
  {:source-url nil,
   :wiki-url "clj-span.models.coastal-storm-protection-api.html",
   :name "clj-span.models.coastal-storm-protection",
   :doc nil}
  {:source-url nil,
   :wiki-url "clj-span.models.flood-water-api.html",
   :name "clj-span.models.flood-water",
   :doc nil}
  {:source-url nil,
   :wiki-url "clj-span.models.line-of-sight-api.html",
   :name "clj-span.models.line-of-sight",
   :doc nil}
  {:source-url nil,
   :wiki-url "clj-span.models.proximity-api.html",
   :name "clj-span.models.proximity",
   :doc nil}
  {:source-url nil,
   :wiki-url "clj-span.models.sediment-api.html",
   :name "clj-span.models.sediment",
   :doc nil}
  {:source-url nil,
   :wiki-url "clj-span.models.subsistence-fisheries-api.html",
   :name "clj-span.models.subsistence-fisheries",
   :doc nil}
  {:source-url nil,
   :wiki-url "clj-span.models.surface-water-api.html",
   :name "clj-span.models.surface-water",
   :doc nil}
  {:source-url nil,
   :wiki-url "clj-span.models.surface-water-topologic-api.html",
   :name "clj-span.models.surface-water-topologic",
   :doc nil}
  {:source-url nil,
   :wiki-url "clj-span.params-api.html",
   :name "clj-span.params",
   :doc nil}
  {:source-url nil,
   :wiki-url "clj-span.repl-utils-api.html",
   :name "clj-span.repl-utils",
   :doc nil}
  {:source-url nil,
   :wiki-url "clj-span.worldgen-api.html",
   :name "clj-span.worldgen",
   :doc nil}),
 :vars
 ({:arglists ([x y]),
   :name "divides?",
   :namespace "clj-misc.matrix-ops",
   :source-url nil,
   :raw-source-url nil,
   :wiki-url
   "/clj-misc.matrix-ops-api.html#clj-misc.matrix-ops/divides?",
   :doc "Is y divisible by x? (i.e. x is the denominator)",
   :var-type "function",
   :line 219,
   :file "src/clj_misc/matrix_ops.clj"}
  {:arglists ([rows cols points]),
   :name "find-bounding-box",
   :namespace "clj-misc.matrix-ops",
   :source-url nil,
   :raw-source-url nil,
   :wiki-url
   "/clj-misc.matrix-ops-api.html#clj-misc.matrix-ops/find-bounding-box",
   :doc
   "Returns a new list of points which completely bounds the\nrectangular region defined by points and remains within the bounds\n[0-rows],[0-cols], inclusive below, exclusive above.",
   :var-type "function",
   :line 406,
   :file "src/clj_misc/matrix_ops.clj"}
  {:arglists ([[pi pj] [bi bj]]),
   :name "find-line-between",
   :namespace "clj-misc.matrix-ops",
   :source-url nil,
   :raw-source-url nil,
   :wiki-url
   "/clj-misc.matrix-ops-api.html#clj-misc.matrix-ops/find-line-between",
   :doc
   "Returns the sequence of all points [i j] intersected by the line\nfrom provider to beneficiary.  Since this is calculated over a\nregular integer-indexed grid, diagonal lines will be approximated\nby lines bending at right angles along the p-to-b line.  This\ncalculation imagines the indeces of each point to be located at the\ncenter of a square of side length 1.  Note that the first point in\neach path will be the provider id, and the last will be the\nbeneficiary id.  If provider=beneficiary, the path will contain\nonly this one point.",
   :var-type "function",
   :line 357,
   :file "src/clj_misc/matrix_ops.clj"}
  {:arglists
   ([[y1 x1 :as A] [y2 x2 :as B] [y3 x3 :as C] [y4 x4 :as D]]),
   :name "find-points-within-box",
   :namespace "clj-misc.matrix-ops",
   :source-url nil,
   :raw-source-url nil,
   :wiki-url
   "/clj-misc.matrix-ops-api.html#clj-misc.matrix-ops/find-points-within-box",
   :doc
   "Points must be specified in either clockwise or counterclockwise order.",
   :var-type "function",
   :line 480,
   :file "src/clj_misc/matrix_ops.clj"}
  {:arglists ([rows cols [i j]]),
   :name "get-neighbors",
   :namespace "clj-misc.matrix-ops",
   :source-url nil,
   :raw-source-url nil,
   :wiki-url
   "/clj-misc.matrix-ops-api.html#clj-misc.matrix-ops/get-neighbors",
   :doc
   "Return a sequence of neighboring points within the map bounds.",
   :var-type "function",
   :line 283,
   :file "src/clj_misc/matrix_ops.clj"}
  {:arglists ([& matrices]),
   :name "grids-align?",
   :namespace "clj-misc.matrix-ops",
   :source-url nil,
   :raw-source-url nil,
   :wiki-url
   "/clj-misc.matrix-ops-api.html#clj-misc.matrix-ops/grids-align?",
   :doc
   "Verifies that all matrices have the same number of rows and\ncolumns.",
   :var-type "function",
   :line 120,
   :file "src/clj_misc/matrix_ops.clj"}
  {:arglists ([rows cols [i j]]),
   :name "in-bounds?",
   :namespace "clj-misc.matrix-ops",
   :source-url nil,
   :raw-source-url nil,
   :wiki-url
   "/clj-misc.matrix-ops-api.html#clj-misc.matrix-ops/in-bounds?",
   :doc
   "Returns true if the point is within the map bounds defined by\n[0 rows] and [0 cols], inclusive below and exclusive above.",
   :var-type "function",
   :line 268,
   :file "src/clj_misc/matrix_ops.clj"}
  {:arglists ([rows cols val-fn]),
   :name "make-matrix",
   :namespace "clj-misc.matrix-ops",
   :source-url nil,
   :raw-source-url nil,
   :wiki-url
   "/clj-misc.matrix-ops-api.html#clj-misc.matrix-ops/make-matrix",
   :doc
   "Creates a rows x cols vector of vectors whose states are generated\nby calling val-fn on the [i j] coordinate pair.",
   :var-type "function",
   :line 37,
   :file "src/clj_misc/matrix_ops.clj"}
  {:arglists ([f matrix] [f matrix & matrices]),
   :name "map-matrix",
   :namespace "clj-misc.matrix-ops",
   :source-url nil,
   :raw-source-url nil,
   :wiki-url
   "/clj-misc.matrix-ops-api.html#clj-misc.matrix-ops/map-matrix",
   :doc
   "Maps a function f over the values in matrix, returning a new\nmatrix.",
   :var-type "function",
   :line 133,
   :file "src/clj_misc/matrix_ops.clj"}
  {:arglists ([matrix]),
   :name "matrix-max",
   :namespace "clj-misc.matrix-ops",
   :source-url nil,
   :raw-source-url nil,
   :wiki-url
   "/clj-misc.matrix-ops-api.html#clj-misc.matrix-ops/matrix-max",
   :doc "Returns the maximum value in the matrix.",
   :var-type "function",
   :line 334,
   :file "src/clj_misc/matrix_ops.clj"}
  {:arglists ([A B]),
   :name "matrix-mult",
   :namespace "clj-misc.matrix-ops",
   :source-url nil,
   :raw-source-url nil,
   :wiki-url
   "/clj-misc.matrix-ops-api.html#clj-misc.matrix-ops/matrix-mult",
   :doc
   "Returns a new matrix whose values are the element-by-element\nproducts of the values in A and B.",
   :var-type "function",
   :line 328,
   :file "src/clj_misc/matrix_ops.clj"}
  {:arglists ([matrix]),
   :name "matrix2seq",
   :namespace "clj-misc.matrix-ops",
   :source-url nil,
   :raw-source-url nil,
   :wiki-url
   "/clj-misc.matrix-ops-api.html#clj-misc.matrix-ops/matrix2seq",
   :doc
   "Returns the contents of a matrix as a single sequence by\nconcatenating all of its rows.",
   :var-type "function",
   :line 96,
   :file "src/clj_misc/matrix_ops.clj"}
  {:arglists ([matrix]),
   :name "normalize-matrix",
   :namespace "clj-misc.matrix-ops",
   :source-url nil,
   :raw-source-url nil,
   :wiki-url
   "/clj-misc.matrix-ops-api.html#clj-misc.matrix-ops/normalize-matrix",
   :doc "Normalizes the values in the matrix to the interval [0,1].",
   :var-type "function",
   :line 339,
   :file "src/clj_misc/matrix_ops.clj"}
  {:arglists ([rows cols [i j]]),
   :name "on-bounds?",
   :namespace "clj-misc.matrix-ops",
   :source-url nil,
   :raw-source-url nil,
   :wiki-url
   "/clj-misc.matrix-ops-api.html#clj-misc.matrix-ops/on-bounds?",
   :doc
   "Returns true if the point occurs anywhere on the bounds\n[[0 rows][0 cols]].",
   :var-type "function",
   :line 274,
   :file "src/clj_misc/matrix_ops.clj"}
  {:arglists ([matrix] [matrix format-string]),
   :name "printf-matrix",
   :namespace "clj-misc.matrix-ops",
   :source-url nil,
   :raw-source-url nil,
   :wiki-url
   "/clj-misc.matrix-ops-api.html#clj-misc.matrix-ops/printf-matrix",
   :doc
   "Pretty prints a matrix to *out* according to format-string. Index\n[0,0] will be on the bottom left corner.",
   :var-type "function",
   :line 302,
   :file "src/clj_misc/matrix_ops.clj"}
  {:arglists ([rows cols aseq]),
   :name "seq2matrix",
   :namespace "clj-misc.matrix-ops",
   :source-url nil,
   :raw-source-url nil,
   :wiki-url
   "/clj-misc.matrix-ops-api.html#clj-misc.matrix-ops/seq2matrix",
   :doc
   "Creates a rows x cols vector of vectors whose states are\nthe successive elements of aseq.",
   :var-type "function",
   :line 89,
   :file "src/clj_misc/matrix_ops.clj"}
  {:arglists ([[x y & more :as point] bounds]),
   :name "in-bounds-full?",
   :namespace "clj-misc.point-algebra",
   :source-url nil,
   :raw-source-url nil,
   :wiki-url
   "/clj-misc.point-algebra-api.html#clj-misc.point-algebra/in-bounds-full?",
   :doc
   "Returns true if point is within bounds [[min-x max-x][min-y\nmax-y]...] and false otherwise.  Works for any dimension.",
   :var-type "function",
   :line 37,
   :file "src/clj_misc/point_algebra.clj"}
  {:arglists ([[x y] [[min-x max-x] [min-y max-y]]]),
   :name "in-bounds?",
   :namespace "clj-misc.point-algebra",
   :source-url nil,
   :raw-source-url nil,
   :wiki-url
   "/clj-misc.point-algebra-api.html#clj-misc.point-algebra/in-bounds?",
   :doc
   "Returns true if point is within bounds [[min-x max-x][min-y max-y]]\nand false otherwise.  Bounds are inclusive below and exclusive\nabove.",
   :var-type "function",
   :line 27,
   :file "src/clj_misc/point_algebra.clj"}
  {:arglists ([bounds]),
   :name "make-bounded-neighbor-generator",
   :namespace "clj-misc.point-algebra",
   :source-url nil,
   :raw-source-url nil,
   :wiki-url
   "/clj-misc.point-algebra-api.html#clj-misc.point-algebra/make-bounded-neighbor-generator",
   :doc
   "Returns a function that, given a point, generates a sequence of all\nneighboring points whose values are within the bounds, specified as\nfollows: [[min-rows max-rows][min-cols max-cols]].",
   :var-type "function",
   :line 46,
   :file "src/clj_misc/point_algebra.clj"}
  {:arglists ([rows cols num-points]),
   :name "make-point-list",
   :namespace "clj-misc.point-algebra",
   :source-url nil,
   :raw-source-url nil,
   :wiki-url
   "/clj-misc.point-algebra-api.html#clj-misc.point-algebra/make-point-list",
   :doc "Return a list of [x y] points.",
   :var-type "function",
   :line 67,
   :file "src/clj_misc/point_algebra.clj"}
  {:arglists ([test bounds origin-point]),
   :name "nearest-point-where",
   :namespace "clj-misc.point-algebra",
   :source-url nil,
   :raw-source-url nil,
   :wiki-url
   "/clj-misc.point-algebra-api.html#clj-misc.point-algebra/nearest-point-where",
   :doc
   "Returns the nearest point to origin-point which satisfies the test\ncriteria or nil if no such point can be found within the bounds\n[[min-x max-x] [min-y max-y]].",
   :var-type "function",
   :line 57,
   :file "src/clj_misc/point_algebra.clj"}
  {:arglists ([X] [n X]),
   :name "draw-repeatedly",
   :namespace "clj-misc.randvars",
   :source-url nil,
   :raw-source-url nil,
   :wiki-url
   "/clj-misc.randvars-api.html#clj-misc.randvars/draw-repeatedly",
   :doc "Extracts values from X using a uniform distribution.",
   :var-type "function",
   :line 462,
   :file "src/clj_misc/randvars.clj"}
  {:arglists ([values] [f values]),
   :name "minimum-discrepancy-partition",
   :namespace "clj-misc.randvars",
   :source-url nil,
   :raw-source-url nil,
   :wiki-url
   "/clj-misc.randvars-api.html#clj-misc.randvars/minimum-discrepancy-partition",
   :doc
   "Given a sequence of sorted values, partition them into two\nsequences (preserving their order), so as to minimize the\ndifference between their sums.  If an optional function f is passed\nit will be applied to the values before they are summed.",
   :var-type "function",
   :line 89,
   :file "src/clj_misc/randvars.clj"}
  {:arglists ([max-partitions X]),
   :name "partition-by-probs",
   :namespace "clj-misc.randvars",
   :source-url nil,
   :raw-source-url nil,
   :wiki-url
   "/clj-misc.randvars-api.html#clj-misc.randvars/partition-by-probs",
   :doc
   "Given a random variable X, returns a partition of its states which\nattempts to minimize the difference between each partition's total\nprobability.",
   :var-type "function",
   :line 111,
   :file "src/clj_misc/randvars.clj"}
  {:arglists ([X]),
   :name "rv-pos",
   :namespace "clj-misc.randvars",
   :source-url nil,
   :raw-source-url nil,
   :wiki-url "/clj-misc.randvars-api.html#clj-misc.randvars/rv-pos",
   :doc "Sets all negative values in X to 0.",
   :var-type "function",
   :line 501,
   :file "src/clj_misc/randvars.clj"}
  {:arglists ([X y]),
   :name "rv-zero-above-scalar",
   :namespace "clj-misc.randvars",
   :source-url nil,
   :raw-source-url nil,
   :wiki-url
   "/clj-misc.randvars-api.html#clj-misc.randvars/rv-zero-above-scalar",
   :doc
   "Sets all values greater than y in the random variable X to 0.",
   :var-type "function",
   :line 491,
   :file "src/clj_misc/randvars.clj"}
  {:arglists ([X y]),
   :name "rv-zero-below-scalar",
   :namespace "clj-misc.randvars",
   :source-url nil,
   :raw-source-url nil,
   :wiki-url
   "/clj-misc.randvars-api.html#clj-misc.randvars/rv-zero-below-scalar",
   :doc "Sets all values less than y in the random variable X to 0.",
   :var-type "function",
   :line 496,
   :file "src/clj_misc/randvars.clj"}
  {:arglists ([x mu sigma]),
   :name "normalize",
   :namespace "clj-misc.stats",
   :source-url nil,
   :raw-source-url nil,
   :wiki-url "/clj-misc.stats-api.html#clj-misc.stats/normalize",
   :doc
   "Return x normalized in the range [0,1].\nx is assumed to be drawn from N(mu,sigma).",
   :var-type "function",
   :line 37,
   :file "src/clj_misc/stats.clj"}
  {:arglists ([x y]),
   :name "add-anyway",
   :namespace "clj-misc.utils",
   :source-url nil,
   :raw-source-url nil,
   :wiki-url "/clj-misc.utils-api.html#clj-misc.utils/add-anyway",
   :doc "Sums the non-nil argument values.",
   :var-type "function",
   :line 96,
   :file "src/clj_misc/utils.clj"}
  {:arglists ([[dy1 dx1 :as A] [dy2 dx2 :as B]]),
   :name "angular-distance",
   :namespace "clj-misc.utils",
   :source-url nil,
   :raw-source-url nil,
   :wiki-url
   "/clj-misc.utils-api.html#clj-misc.utils/angular-distance",
   :doc "Given two vectors A and B, returns the angle between them.",
   :var-type "function",
   :line 375,
   :file "src/clj_misc/utils.clj"}
  {:arglists ([[dy1 dx1 :as A] [dy2 dx2 :as B]]),
   :name "angular-distance2",
   :namespace "clj-misc.utils",
   :source-url nil,
   :raw-source-url nil,
   :wiki-url
   "/clj-misc.utils-api.html#clj-misc.utils/angular-distance2",
   :doc "Given two vectors A and B, returns the angle between them.",
   :var-type "function",
   :line 385,
   :file "src/clj_misc/utils.clj"}
  {:arglists ([[dy1 dx1 :as A] [dy2 dx2 :as B]]),
   :name "angular-rotation",
   :namespace "clj-misc.utils",
   :source-url nil,
   :raw-source-url nil,
   :wiki-url
   "/clj-misc.utils-api.html#clj-misc.utils/angular-rotation",
   :doc
   "Given two vectors A and B, returns the directed angle between them.",
   :var-type "function",
   :line 370,
   :file "src/clj_misc/utils.clj"}
  {:arglists ([vect-of-vects]),
   :name "arrayify",
   :namespace "clj-misc.utils",
   :source-url nil,
   :raw-source-url nil,
   :wiki-url "/clj-misc.utils-api.html#clj-misc.utils/arrayify",
   :doc "Creates a 2D Java array (of Objects) from a vect of vects.",
   :var-type "function",
   :line 165,
   :file "src/clj_misc/utils.clj"}
  {:arglists ([clojure-map]),
   :name "arrayify-map",
   :namespace "clj-misc.utils",
   :source-url nil,
   :raw-source-url nil,
   :wiki-url "/clj-misc.utils-api.html#clj-misc.utils/arrayify-map",
   :doc
   "Creates a Java HashMap<String,Array[]> from a map of {keywords ->\nvect-of-vects}.",
   :var-type "function",
   :line 176,
   :file "src/clj_misc/utils.clj"}
  {:arglists ([open-list closed-set successors goal?]),
   :name "breadth-first-search",
   :namespace "clj-misc.utils",
   :source-url nil,
   :raw-source-url nil,
   :wiki-url
   "/clj-misc.utils-api.html#clj-misc.utils/breadth-first-search",
   :doc
   "The classic breadth-first-search.  Bread and butter of computer\nscience.  Implemented using tail recursion, of course! ;)",
   :var-type "function",
   :line 204,
   :file "src/clj_misc/utils.clj"}
  {:arglists ([sequence item]),
   :name "contains-item?",
   :namespace "clj-misc.utils",
   :source-url nil,
   :raw-source-url nil,
   :wiki-url "/clj-misc.utils-api.html#clj-misc.utils/contains-item?",
   :doc "Returns true if sequence contains item.  Otherwise nil.",
   :var-type "function",
   :line 199,
   :file "src/clj_misc/utils.clj"}
  {:arglists ([vals] [vals n]),
   :name "count-distinct",
   :namespace "clj-misc.utils",
   :source-url nil,
   :raw-source-url nil,
   :wiki-url "/clj-misc.utils-api.html#clj-misc.utils/count-distinct",
   :doc
   "Returns a map of {distinct-val -> num-instances, ...} for all the\ndistinct values in a sequence.  If n is given, only count the first\nn distinct values and append {... -> num-distinct - n} to the map\nto indicate that more values were not examined.",
   :var-type "function",
   :line 399,
   :file "src/clj_misc/utils.clj"}
  {:arglists ([root successors goal?]),
   :name "depth-first-graph-search",
   :namespace "clj-misc.utils",
   :source-url nil,
   :raw-source-url nil,
   :wiki-url
   "/clj-misc.utils-api.html#clj-misc.utils/depth-first-graph-search",
   :doc
   "The classic depth-first-graph-search. Bread and butter of computer\nscience. Implemented using tail recursion, of course! ;)",
   :var-type "function",
   :line 243,
   :file "src/clj_misc/utils.clj"}
  {:arglists ([root successors goal?]),
   :name "depth-first-tree-search",
   :namespace "clj-misc.utils",
   :source-url nil,
   :raw-source-url nil,
   :wiki-url
   "/clj-misc.utils-api.html#clj-misc.utils/depth-first-tree-search",
   :doc
   "The classic depth-first-tree-search. Bread and butter of computer\nscience. Implemented using tail recursion, of course! ;)",
   :var-type "function",
   :line 233,
   :file "src/clj_misc/utils.clj"}
  {:arglists ([pointA pointB]),
   :name "euclidean-distance",
   :namespace "clj-misc.utils",
   :source-url nil,
   :raw-source-url nil,
   :wiki-url
   "/clj-misc.utils-api.html#clj-misc.utils/euclidean-distance",
   :doc
   "Returns the euclidean distance between two n-dimensional points.",
   :var-type "function",
   :line 347,
   :file "src/clj_misc/utils.clj"}
  {:arglists ([avec]),
   :name "expand-runtime-encoded-vector",
   :namespace "clj-misc.utils",
   :source-url nil,
   :raw-source-url nil,
   :wiki-url
   "/clj-misc.utils-api.html#clj-misc.utils/expand-runtime-encoded-vector",
   :doc
   "Expands a vector of the form [:foo 2 :bar 1 :baz 3] into\n[:foo :foo :bar :baz :baz :baz].",
   :var-type "function",
   :line 187,
   :file "src/clj_misc/utils.clj"}
  {:arglists ([m v]),
   :name "key-by-val",
   :namespace "clj-misc.utils",
   :source-url nil,
   :raw-source-url nil,
   :wiki-url "/clj-misc.utils-api.html#clj-misc.utils/key-by-val",
   :doc
   "Returns the key from a map m whose corresponding value field is a\nsequence containing v.",
   :var-type "function",
   :line 149,
   :file "src/clj_misc/utils.clj"}
  {:arglists ([matrix]),
   :name "linearize",
   :namespace "clj-misc.utils",
   :source-url nil,
   :raw-source-url nil,
   :wiki-url "/clj-misc.utils-api.html#clj-misc.utils/linearize",
   :doc "Transforms a 2D matrix into a 1D vector.",
   :var-type "function",
   :line 155,
   :file "src/clj_misc/utils.clj"}
  {:arglists ([pointA pointB]),
   :name "manhattan-distance",
   :namespace "clj-misc.utils",
   :source-url nil,
   :raw-source-url nil,
   :wiki-url
   "/clj-misc.utils-api.html#clj-misc.utils/manhattan-distance",
   :doc
   "Returns the manhattan distance between two n-dimensional points.",
   :var-type "function",
   :line 336,
   :file "src/clj_misc/utils.clj"}
  {:arglists ([[i1 j1] [i2 j2]]),
   :name "manhattan-distance-2",
   :namespace "clj-misc.utils",
   :source-url nil,
   :raw-source-url nil,
   :wiki-url
   "/clj-misc.utils-api.html#clj-misc.utils/manhattan-distance-2",
   :doc
   "Returns the manhattan distance between two 2-dimensional points.",
   :var-type "function",
   :line 331,
   :file "src/clj_misc/utils.clj"}
  {:arglists ([keyfn valfn in-map]),
   :name "mapmap",
   :namespace "clj-misc.utils",
   :source-url nil,
   :raw-source-url nil,
   :wiki-url "/clj-misc.utils-api.html#clj-misc.utils/mapmap",
   :doc
   "Creates a new map by applying keyfn to every key of in-map and\nvalfn to every corresponding val.",
   :var-type "function",
   :line 125,
   :file "src/clj_misc/utils.clj"}
  {:arglists ([keyfn valfn in-map]),
   :name "mapmap-java",
   :namespace "clj-misc.utils",
   :source-url nil,
   :raw-source-url nil,
   :wiki-url "/clj-misc.utils-api.html#clj-misc.utils/mapmap-java",
   :doc
   "Creates a new Java map by applying keyfn to every key of in-map and\nvalfn to every corresponding val.",
   :var-type "function",
   :line 131,
   :file "src/clj_misc/utils.clj"}
  {:arglists ([element times base-coll]),
   :name "multi-conj",
   :namespace "clj-misc.utils",
   :source-url nil,
   :raw-source-url nil,
   :wiki-url "/clj-misc.utils-api.html#clj-misc.utils/multi-conj",
   :doc "Conjoins an element multiple times onto a base-coll.",
   :var-type "function",
   :line 182,
   :file "src/clj_misc/utils.clj"}
  {:arglists ([x form] [x form & more]),
   :name "my->>",
   :namespace "clj-misc.utils",
   :source-url nil,
   :added "1.1",
   :raw-source-url nil,
   :wiki-url "/clj-misc.utils-api.html#clj-misc.utils/my->>",
   :doc
   "Threads the expr through the forms. Inserts x as the\nlast item in the first form, making a list of it if it is not a\nlist already. If there are more forms, inserts the first form as the\nlast item in second form, etc.",
   :var-type "macro",
   :line 85,
   :file "src/clj_misc/utils.clj"}
  {:arglists ([]),
   :name "print-sysprops",
   :namespace "clj-misc.utils",
   :source-url nil,
   :raw-source-url nil,
   :wiki-url "/clj-misc.utils-api.html#clj-misc.utils/print-sysprops",
   :doc "Print out the result of System.getProperties()",
   :var-type "function",
   :line 34,
   :file "src/clj_misc/utils.clj"}
  {:arglists ([f val coll]),
   :name "reduce-true",
   :namespace "clj-misc.utils",
   :source-url nil,
   :raw-source-url nil,
   :wiki-url "/clj-misc.utils-api.html#clj-misc.utils/reduce-true",
   :doc "Like reduce but short-circuits upon logical false",
   :var-type "function",
   :line 505,
   :file "src/clj_misc/utils.clj"}
  {:arglists ([n total min-value]),
   :name "select-n-summands",
   :namespace "clj-misc.utils",
   :source-url nil,
   :raw-source-url nil,
   :wiki-url
   "/clj-misc.utils-api.html#clj-misc.utils/select-n-summands",
   :doc
   "Returns a list of n numbers >= min-value, which add up to total.\nIf total is a double, the summands will be doubles.  The same goes\nfor integers.",
   :var-type "function",
   :line 553,
   :file "src/clj_misc/utils.clj"}
  {:arglists ([aseq keyvalfn]),
   :name "seq2map",
   :namespace "clj-misc.utils",
   :source-url nil,
   :raw-source-url nil,
   :wiki-url "/clj-misc.utils-api.html#clj-misc.utils/seq2map",
   :doc
   "Constructs a map from a sequence by applying keyvalfn to each\nelement of the sequence.  keyvalfn should return a pair [key val]\nto be added to the map for each input sequence element.",
   :var-type "function",
   :line 103,
   :file "src/clj_misc/utils.clj"}
  {:arglists ([aseq keyvalfn mergefn]),
   :name "seq2redundant-map",
   :namespace "clj-misc.utils",
   :source-url nil,
   :raw-source-url nil,
   :wiki-url
   "/clj-misc.utils-api.html#clj-misc.utils/seq2redundant-map",
   :doc
   "Constructs a map from a sequence by applying keyvalfn to each\nelement of the sequence.  keyvalfn should return a pair [key val]\nto be added to the map for each input sequence element.  If key is\nalready in the map, its current value will be combined with the new\nval using (mergefn curval val).",
   :var-type "function",
   :line 110,
   :file "src/clj_misc/utils.clj"}
  {:arglists ([root successors goal? heuristic-filter]),
   :name "shortest-path-bfgs",
   :namespace "clj-misc.utils",
   :source-url nil,
   :raw-source-url nil,
   :wiki-url
   "/clj-misc.utils-api.html#clj-misc.utils/shortest-path-bfgs",
   :doc
   "The classic breadth-first-graph-search.  Bread and butter of computer\nscience.  Implemented using tail recursion, of course! ;)",
   :var-type "function",
   :line 219,
   :file "src/clj_misc/utils.clj"}
  {:arglists ([java-array]),
   :name "vectorize",
   :namespace "clj-misc.utils",
   :source-url nil,
   :raw-source-url nil,
   :wiki-url "/clj-misc.utils-api.html#clj-misc.utils/vectorize",
   :doc "Creates a vect of vects from a 2D Java array.",
   :var-type "function",
   :line 160,
   :file "src/clj_misc/utils.clj"}
  {:arglists ([java-map]),
   :name "vectorize-map",
   :namespace "clj-misc.utils",
   :source-url nil,
   :raw-source-url nil,
   :wiki-url "/clj-misc.utils-api.html#clj-misc.utils/vectorize-map",
   :doc
   "Creates a map of {keywords -> vect-of-vects} from a Java\nHashMap<String,Array[]>.",
   :var-type "function",
   :line 170,
   :file "src/clj_misc/utils.clj"}
  {:arglists ([x Y] [x Y & more]),
   :name "*_",
   :namespace "clj-misc.varprop",
   :source-url nil,
   :raw-source-url nil,
   :wiki-url "/clj-misc.varprop-api.html#clj-misc.varprop/*_",
   :doc
   "Returns the product of a scalar and one or more FuzzyNumbers.",
   :var-type "function",
   :line 197,
   :file "src/clj_misc/varprop.clj"}
  {:arglists ([x Y] [x Y & more]),
   :name "+_",
   :namespace "clj-misc.varprop",
   :source-url nil,
   :raw-source-url nil,
   :wiki-url "/clj-misc.varprop-api.html#clj-misc.varprop/+_",
   :doc "Returns the sum of a scalar and one or more FuzzyNumbers.",
   :var-type "function",
   :line 183,
   :file "src/clj_misc/varprop.clj"}
  {:arglists ([x Y] [x Y & more]),
   :name "-_",
   :namespace "clj-misc.varprop",
   :source-url nil,
   :raw-source-url nil,
   :wiki-url "/clj-misc.varprop-api.html#clj-misc.varprop/-_",
   :doc
   "Returns the difference of a scalar and one or more FuzzyNumbers.",
   :var-type "function",
   :line 190,
   :file "src/clj_misc/varprop.clj"}
  {:arglists ([x Y] [x Y & more]),
   :name "<_",
   :namespace "clj-misc.varprop",
   :source-url nil,
   :raw-source-url nil,
   :wiki-url "/clj-misc.varprop-api.html#clj-misc.varprop/<_",
   :doc
   "Compares a scalar and one or more FuzzyNumbers and returns true if\nP(Y > x) > 0.5 and all Ys are in monotonically increasing order by\n_<_.",
   :var-type "function",
   :line 211,
   :file "src/clj_misc/varprop.clj"}
  {:arglists ([x Y] [x Y & more]),
   :name ">_",
   :namespace "clj-misc.varprop",
   :source-url nil,
   :raw-source-url nil,
   :wiki-url "/clj-misc.varprop-api.html#clj-misc.varprop/>_",
   :doc
   "Compares a scalar and one or more FuzzyNumbers and returns true if\nP(Y < x) > 0.5 and all Ys are in monotonically decreasing order by\n_>_.",
   :var-type "function",
   :line 221,
   :file "src/clj_misc/varprop.clj"}
  {:arglists ([X y] [X y & more]),
   :name "_*",
   :namespace "clj-misc.varprop",
   :source-url nil,
   :raw-source-url nil,
   :wiki-url "/clj-misc.varprop-api.html#clj-misc.varprop/_*",
   :doc
   "Returns the product of a FuzzyNumber and one or more scalars.",
   :var-type "function",
   :line 137,
   :file "src/clj_misc/varprop.clj"}
  {:arglists ([{mx :mean, vx :var} {my :mean, vy :var}] [X Y & more]),
   :name "_*_",
   :namespace "clj-misc.varprop",
   :source-url nil,
   :raw-source-url nil,
   :wiki-url "/clj-misc.varprop-api.html#clj-misc.varprop/_*_",
   :doc "Returns the product of two or more FuzzyNumbers.",
   :var-type "function",
   :line 79,
   :file "src/clj_misc/varprop.clj"}
  {:arglists ([X y] [X y & more]),
   :name "_+",
   :namespace "clj-misc.varprop",
   :source-url nil,
   :raw-source-url nil,
   :wiki-url "/clj-misc.varprop-api.html#clj-misc.varprop/_+",
   :doc "Returns the sum of a FuzzyNumber and one or more scalars.",
   :var-type "function",
   :line 123,
   :file "src/clj_misc/varprop.clj"}
  {:arglists ([X Y] [X Y & more]),
   :name "_+_",
   :namespace "clj-misc.varprop",
   :source-url nil,
   :raw-source-url nil,
   :wiki-url "/clj-misc.varprop-api.html#clj-misc.varprop/_+_",
   :doc "Returns the sum of two or more FuzzyNumbers.",
   :var-type "function",
   :line 65,
   :file "src/clj_misc/varprop.clj"}
  {:arglists ([X y] [X y & more]),
   :name "_-",
   :namespace "clj-misc.varprop",
   :source-url nil,
   :raw-source-url nil,
   :wiki-url "/clj-misc.varprop-api.html#clj-misc.varprop/_-",
   :doc
   "Returns the difference of a FuzzyNumber and one or more scalars.",
   :var-type "function",
   :line 130,
   :file "src/clj_misc/varprop.clj"}
  {:arglists ([X Y] [X Y & more]),
   :name "_-_",
   :namespace "clj-misc.varprop",
   :source-url nil,
   :raw-source-url nil,
   :wiki-url "/clj-misc.varprop-api.html#clj-misc.varprop/_-_",
   :doc "Returns the difference of two or more FuzzyNumbers.",
   :var-type "function",
   :line 72,
   :file "src/clj_misc/varprop.clj"}
  {:file "src/clj_misc/varprop.clj",
   :raw-source-url nil,
   :source-url nil,
   :wiki-url "/clj-misc.varprop-api.html#clj-misc.varprop/_0_",
   :namespace "clj-misc.varprop",
   :line 63,
   :var-type "var",
   :doc "A fuzzy number with mean and variance of 0.",
   :name "_0_"}
  {:arglists ([X y] [X y & more]),
   :name "_<",
   :namespace "clj-misc.varprop",
   :source-url nil,
   :raw-source-url nil,
   :wiki-url "/clj-misc.varprop-api.html#clj-misc.varprop/_<",
   :doc
   "Compares a FuzzyNumber and one or more scalars and returns true if\nP(X < y_1) > 0.5 and all ys are in monotonically increasing order.",
   :var-type "function",
   :line 151,
   :file "src/clj_misc/varprop.clj"}
  {:arglists ([X Y] [X Y & more]),
   :name "_<_",
   :namespace "clj-misc.varprop",
   :source-url nil,
   :raw-source-url nil,
   :wiki-url "/clj-misc.varprop-api.html#clj-misc.varprop/_<_",
   :doc
   "Compares two or more FuzzyNumbers and returns true if P(X_i < X_i+1) > 0.5 for all i in [1,n].",
   :var-type "function",
   :line 95,
   :file "src/clj_misc/varprop.clj"}
  {:arglists ([X y] [X y & more]),
   :name "_>",
   :namespace "clj-misc.varprop",
   :source-url nil,
   :raw-source-url nil,
   :wiki-url "/clj-misc.varprop-api.html#clj-misc.varprop/_>",
   :doc
   "Compares a FuzzyNumber and one or more scalars and returns true if\nP(X > y_1) > 0.5 and all ys are in monotonically decreasing order.",
   :var-type "function",
   :line 160,
   :file "src/clj_misc/varprop.clj"}
  {:arglists ([X Y] [X Y & more]),
   :name "_>_",
   :namespace "clj-misc.varprop",
   :source-url nil,
   :raw-source-url nil,
   :wiki-url "/clj-misc.varprop-api.html#clj-misc.varprop/_>_",
   :doc
   "Compares two or more FuzzyNumbers and returns true if P(X_i > X_i+1) > 0.5 for all i in [1,n].",
   :var-type "function",
   :line 102,
   :file "src/clj_misc/varprop.clj"}
  {:arglists ([X y] [X y & more]),
   :name "_d",
   :namespace "clj-misc.varprop",
   :source-url nil,
   :raw-source-url nil,
   :wiki-url "/clj-misc.varprop-api.html#clj-misc.varprop/_d",
   :doc
   "Returns the quotient of a FuzzyNumber and one or more scalars.",
   :var-type "function",
   :line 144,
   :file "src/clj_misc/varprop.clj"}
  {:arglists ([X Y] [X Y & more]),
   :name "_d_",
   :namespace "clj-misc.varprop",
   :source-url nil,
   :raw-source-url nil,
   :wiki-url "/clj-misc.varprop-api.html#clj-misc.varprop/_d_",
   :doc "Returns the quotient of two or more FuzzyNumbers.",
   :var-type "function",
   :line 88,
   :file "src/clj_misc/varprop.clj"}
  {:arglists ([X y] [X y & more]),
   :name "_max",
   :namespace "clj-misc.varprop",
   :source-url nil,
   :raw-source-url nil,
   :wiki-url "/clj-misc.varprop-api.html#clj-misc.varprop/_max",
   :doc
   "Returns the greatest of a FuzzyNumber and one or more scalars using _>.",
   :var-type "function",
   :line 176,
   :file "src/clj_misc/varprop.clj"}
  {:arglists ([X Y] [X Y & more]),
   :name "_max_",
   :namespace "clj-misc.varprop",
   :source-url nil,
   :raw-source-url nil,
   :wiki-url "/clj-misc.varprop-api.html#clj-misc.varprop/_max_",
   :doc "Returns the greatest of two or more FuzzyNumbers using _>_.",
   :var-type "function",
   :line 116,
   :file "src/clj_misc/varprop.clj"}
  {:arglists ([X y] [X y & more]),
   :name "_min",
   :namespace "clj-misc.varprop",
   :source-url nil,
   :raw-source-url nil,
   :wiki-url "/clj-misc.varprop-api.html#clj-misc.varprop/_min",
   :doc
   "Returns the smallest of a FuzzyNumber and one or more scalars using _<.",
   :var-type "function",
   :line 169,
   :file "src/clj_misc/varprop.clj"}
  {:arglists ([X Y] [X Y & more]),
   :name "_min_",
   :namespace "clj-misc.varprop",
   :source-url nil,
   :raw-source-url nil,
   :wiki-url "/clj-misc.varprop-api.html#clj-misc.varprop/_min_",
   :doc "Returns the smallest of two or more FuzzyNumbers using _<_.",
   :var-type "function",
   :line 109,
   :file "src/clj_misc/varprop.clj"}
  {:arglists ([]),
   :name "box-muller-normal",
   :namespace "clj-misc.varprop",
   :source-url nil,
   :raw-source-url nil,
   :wiki-url
   "/clj-misc.varprop-api.html#clj-misc.varprop/box-muller-normal",
   :doc
   "Returns a value from X~N(0,1). Uses the Box-Muller\ntransform. Memoizes extra computed values for quicker lookups on\neven calls.",
   :var-type "function",
   :line 436,
   :file "src/clj_misc/varprop.clj"}
  {:arglists ([x {:keys [mean var]}] [x Y & more]),
   :name "d_",
   :namespace "clj-misc.varprop",
   :source-url nil,
   :raw-source-url nil,
   :wiki-url "/clj-misc.varprop-api.html#clj-misc.varprop/d_",
   :doc
   "Returns the quotient of a scalar and one or more FuzzyNumbers.",
   :var-type "function",
   :line 204,
   :file "src/clj_misc/varprop.clj"}
  {:arglists ([X]),
   :name "draw",
   :namespace "clj-misc.varprop",
   :source-url nil,
   :raw-source-url nil,
   :wiki-url "/clj-misc.varprop-api.html#clj-misc.varprop/draw",
   :doc
   "Extracts a deterministic value from a FuzzyNumber by modelling it\nas a normal distribution.",
   :var-type "function",
   :line 453,
   :file "src/clj_misc/varprop.clj"}
  {:arglists ([{:keys [mean var]}] [n X]),
   :name "draw-repeatedly",
   :namespace "clj-misc.varprop",
   :source-url nil,
   :raw-source-url nil,
   :wiki-url
   "/clj-misc.varprop-api.html#clj-misc.varprop/draw-repeatedly",
   :doc
   "Takes a fuzzy number X, and returns an infinite lazy sequence of\nnormally-distributed, pseudo-random numbers that match the\nparameters of X, (or a finite sequence of length n, if an integer n\nis provided).",
   :var-type "function",
   :line 459,
   :file "src/clj_misc/varprop.clj"}
  {:arglists ([f]),
   :name "fuzzify-fn",
   :namespace "clj-misc.varprop",
   :source-url nil,
   :raw-source-url nil,
   :wiki-url "/clj-misc.varprop-api.html#clj-misc.varprop/fuzzify-fn",
   :doc
   "Transforms f into its fuzzy arithmetic equivalent, using the\nmappings defined in fuzzy-arithmetic-mapping.",
   :var-type "function",
   :line 361,
   :file "src/clj_misc/varprop.clj"}
  {:arglists ([mean var]),
   :name "fuzzy-number",
   :namespace "clj-misc.varprop",
   :source-url nil,
   :raw-source-url nil,
   :wiki-url
   "/clj-misc.varprop-api.html#clj-misc.varprop/fuzzy-number",
   :doc "Constructs a FuzzyNumber.",
   :var-type "function",
   :line 35,
   :file "src/clj_misc/varprop.clj"}
  {:arglists ([bounds probs]),
   :name "fuzzy-number-from-ranges",
   :namespace "clj-misc.varprop",
   :source-url nil,
   :raw-source-url nil,
   :wiki-url
   "/clj-misc.varprop-api.html#clj-misc.varprop/fuzzy-number-from-ranges",
   :doc
   "Constructs a FuzzyNumber from n bounds and n-1 probs corresponding\nto a piecewise continuous uniform distribution with\ndiscontinuities (i.e. jumps) at the bounds. prob i represents the\nprobability of being between bound i and bound i+1.",
   :var-type "function",
   :line 48,
   :file "src/clj_misc/varprop.clj"}
  {:arglists ([states probs]),
   :name "fuzzy-number-from-states",
   :namespace "clj-misc.varprop",
   :source-url nil,
   :raw-source-url nil,
   :wiki-url
   "/clj-misc.varprop-api.html#clj-misc.varprop/fuzzy-number-from-states",
   :doc
   "Constructs a FuzzyNumber from n states and n probs, corresponding\nto a finite discrete distribution.",
   :var-type "function",
   :line 40,
   :file "src/clj_misc/varprop.clj"}
  {:arglists ([]),
   :name "marsaglia-normal",
   :namespace "clj-misc.varprop",
   :source-url nil,
   :raw-source-url nil,
   :wiki-url
   "/clj-misc.varprop-api.html#clj-misc.varprop/marsaglia-normal",
   :doc
   "Returns a value from X~N(0,1). Uses the Marsaglia polar\nmethod. Memoizes extra computed values for quicker lookups on\neven calls.",
   :var-type "function",
   :line 418,
   :file "src/clj_misc/varprop.clj"}
  {:arglists ([x Y] [x Y & more]),
   :name "max_",
   :namespace "clj-misc.varprop",
   :source-url nil,
   :raw-source-url nil,
   :wiki-url "/clj-misc.varprop-api.html#clj-misc.varprop/max_",
   :doc
   "Returns the greatest of a scalar and one or more FuzzyNumbers using >_.",
   :var-type "function",
   :line 238,
   :file "src/clj_misc/varprop.clj"}
  {:arglists ([x Y] [x Y & more]),
   :name "min_",
   :namespace "clj-misc.varprop",
   :source-url nil,
   :raw-source-url nil,
   :wiki-url "/clj-misc.varprop-api.html#clj-misc.varprop/min_",
   :doc
   "Returns the smallest of a scalar and one or more FuzzyNumbers using <_.",
   :var-type "function",
   :line 231,
   :file "src/clj_misc/varprop.clj"}
  {:arglists ([coverage]),
   :name "rv-extensive-sampler",
   :namespace "clj-misc.varprop",
   :source-url nil,
   :raw-source-url nil,
   :wiki-url
   "/clj-misc.varprop-api.html#clj-misc.varprop/rv-extensive-sampler",
   :doc
   "Returns the extensive weighted sum of a coverage (i.e. a sequence\nof pairs of [value fraction-covered]). For use with\nclj-misc.matrix-ops/resample-matrix.",
   :var-type "function",
   :line 402,
   :file "src/clj_misc/varprop.clj"}
  {:arglists ([f & Xs]),
   :name "rv-fn",
   :namespace "clj-misc.varprop",
   :source-url nil,
   :raw-source-url nil,
   :wiki-url "/clj-misc.varprop-api.html#clj-misc.varprop/rv-fn",
   :doc
   "Transforms f into its fuzzy arithmetic equivalent, fuzzy-f, and\ncalls (apply fuzzy-f Xs). Uses reflection on the types of Xs as\nwell as any numeric literal values used in f.",
   :var-type "macro",
   :line 374,
   :file "src/clj_misc/varprop.clj"}
  {:arglists ([coverage]),
   :name "rv-intensive-sampler",
   :namespace "clj-misc.varprop",
   :source-url nil,
   :raw-source-url nil,
   :wiki-url
   "/clj-misc.varprop-api.html#clj-misc.varprop/rv-intensive-sampler",
   :doc
   "Returns the intensive weighted sum of a coverage (i.e. a sequence\nof pairs of [value fraction-covered]). For use with\nclj-misc.matrix-ops/resample-matrix.",
   :var-type "function",
   :line 409,
   :file "src/clj_misc/varprop.clj"}
  {:arglists ([X]),
   :name "rv-mean",
   :namespace "clj-misc.varprop",
   :source-url nil,
   :raw-source-url nil,
   :wiki-url "/clj-misc.varprop-api.html#clj-misc.varprop/rv-mean",
   :doc "Returns the mean of a FuzzyNumber.",
   :var-type "function",
   :line 381,
   :file "src/clj_misc/varprop.clj"}
  {:arglists ([Xs]),
   :name "rv-sum",
   :namespace "clj-misc.varprop",
   :source-url nil,
   :raw-source-url nil,
   :wiki-url "/clj-misc.varprop-api.html#clj-misc.varprop/rv-sum",
   :doc "Returns the sum of a sequence of FuzzyNumbers using _+_.",
   :var-type "function",
   :line 386,
   :file "src/clj_misc/varprop.clj"}
  {:arglists ([possible-flow-layer actual-flow-layer]),
   :name "blocked-flow",
   :namespace "clj-span.analyzer",
   :source-url nil,
   :raw-source-url nil,
   :wiki-url
   "/clj-span.analyzer-api.html#clj-span.analyzer/blocked-flow",
   :doc
   "Returns a map of {location-id -> blocked-flow}.\nBlocked-flow is the amount of the possible-flow which cannot be\nrealized due to upstream sinks or uses.",
   :var-type "function",
   :line 237,
   :file "src/clj_span/analyzer.clj"}
  {:arglists ([cache-layer]),
   :name "blocked-source",
   :namespace "clj-span.analyzer",
   :source-url nil,
   :raw-source-url nil,
   :wiki-url
   "/clj-span.analyzer-api.html#clj-span.analyzer/blocked-source",
   :doc
   "Returns a map of {location-id -> blocked-source}.\nBlocked-source is the amount of the possible-source which cannot be\nused by any location due to upstream sinks or uses.",
   :var-type "function",
   :line 219,
   :file "src/clj_span/analyzer.clj"}
  {:arglists ([cache-layer]),
   :name "blocked-use",
   :namespace "clj-span.analyzer",
   :source-url nil,
   :raw-source-url nil,
   :wiki-url
   "/clj-span.analyzer-api.html#clj-span.analyzer/blocked-use",
   :doc
   "Returns a map of {location-id -> blocked-use}.\nBlocked-use is the amount of the possible-use which cannot be\nrealized due to upstream sinks or uses.",
   :var-type "function",
   :line 228,
   :file "src/clj_span/analyzer.clj"}
  {:arglists ([source-layer sink-layer use-layer cache-layer]),
   :name "inaccessible-sink",
   :namespace "clj-span.analyzer",
   :source-url nil,
   :raw-source-url nil,
   :wiki-url
   "/clj-span.analyzer-api.html#clj-span.analyzer/inaccessible-sink",
   :doc
   "Returns a map of {location-id -> inaccessible-sink}.\nInaccessible-sink is the amount of the theoretical-sink which\ncannot be utilized by any location either due to propagation decay\nof the asset or lack of flow pathways through the sink locations.",
   :var-type "function",
   :line 199,
   :file "src/clj_span/analyzer.clj"}
  {:arglists ([source-layer use-layer cache-layer]),
   :name "inaccessible-source",
   :namespace "clj-span.analyzer",
   :source-url nil,
   :raw-source-url nil,
   :wiki-url
   "/clj-span.analyzer-api.html#clj-span.analyzer/inaccessible-source",
   :doc
   "Returns a map of {location-id -> inaccessible-source}.\nInaccessible-source is the amount of the theoretical-source which\ncannot be used by any location either due to propagation decay,\nlack of use capacity, or lack of flow pathways to use locations.",
   :var-type "function",
   :line 189,
   :file "src/clj_span/analyzer.clj"}
  {:arglists ([source-layer use-layer cache-layer]),
   :name "inaccessible-use",
   :namespace "clj-span.analyzer",
   :source-url nil,
   :raw-source-url nil,
   :wiki-url
   "/clj-span.analyzer-api.html#clj-span.analyzer/inaccessible-use",
   :doc
   "Returns a map of {location-id -> inaccessible-use}.\nInaccessible-use is the amount of the theoretical-use which cannot\nbe utilized by each location either due to propagation decay of the\nasset or lack of flow pathways to use locations.",
   :var-type "function",
   :line 209,
   :file "src/clj_span/analyzer.clj"}
  {:arglists
   ([observation-or-model-spec
     source-concept
     sink-concept
     use-concept
     flow-concepts
     {:keys
      [source-threshold
       sink-threshold
       use-threshold
       trans-threshold
       rv-max-states
       downscaling-factor
       source-type
       sink-type
       use-type
       benefit-type
       result-type
       value-type
       animation?
       save-file],
      :or {result-type :closure-map, value-type :varprop}}]),
   :name "span-driver",
   :namespace "clj-span.aries-span-bridge",
   :source-url nil,
   :raw-source-url nil,
   :wiki-url
   "/clj-span.aries-span-bridge-api.html#clj-span.aries-span-bridge/span-driver",
   :doc
   "Takes the source, sink, use, and flow concepts along with the\nflow-params map and an observation containing the concepts'\ndependent features (or model-spec [model-name location resolution]\nwhich produces this observation), calculates the SPAN flows, and\nreturns the results using one of the following result-types:\n(:cli-menu :closure-map). If the :save-file parameter is set in the\nflow-params map, the SPAN model will not be run, and instead the\nsource, sink, use, and flow layers will be extracted from the\nobservation and written to :save-file.",
   :var-type "function",
   :line 262,
   :file "src/clj_span/aries_span_bridge.clj"}
  {:arglists ([& args]),
   :name "-main",
   :namespace "clj-span.commandline",
   :source-url nil,
   :raw-source-url nil,
   :wiki-url
   "/clj-span.commandline-api.html#clj-span.commandline/-main",
   :doc
   "The compiled Java class' main method.  Pass it all the SPAN inputs\nas -key value argument pairs from the command line.  After\nvalidating your inputs and running the flow simulation, it will\npresent a text-based menu to view the model results.",
   :var-type "function",
   :line 145,
   :file "src/clj_span/commandline.clj"}
  {:arglists
   ([flow-model
     animation?
     orig-rows
     orig-cols
     cell-width
     cell-height
     scaled-source-layer
     scaled-sink-layer
     scaled-use-layer
     scaled-flow-layers
     row-scale-factor
     col-scale-factor]),
   :name "generate-results-map",
   :namespace "clj-span.core",
   :source-url nil,
   :raw-source-url nil,
   :wiki-url
   "/clj-span.core-api.html#clj-span.core/generate-results-map",
   :doc
   "Run flow model and return the results as a map of layer names to closures.",
   :var-type "function",
   :line 183,
   :file "src/clj_span/core.clj"}
  {:arglists
   ([source-layer
     source-threshold
     sink-layer
     sink-threshold
     use-layer
     use-threshold
     flow-layers
     downscaling-factor]),
   :name "preprocess-data-layers",
   :namespace "clj-span.core",
   :source-url nil,
   :raw-source-url nil,
   :wiki-url
   "/clj-span.core-api.html#clj-span.core/preprocess-data-layers",
   :doc
   "Preprocess data layers (downsampling and zeroing below their thresholds).",
   :var-type "function",
   :line 66,
   :file "src/clj_span/core.clj"}
  {:arglists ([threshold layer]),
   :name "zero-layer-below-threshold",
   :namespace "clj-span.core",
   :source-url nil,
   :raw-source-url nil,
   :wiki-url
   "/clj-span.core-api.html#clj-span.core/zero-layer-below-threshold",
   :doc
   "Takes a two dimensional array of RVs and replaces all values which\nhave a >50% likelihood of being below the threshold with _0_.",
   :var-type "function",
   :line 55,
   :file "src/clj_span/core.clj"}
  {:arglists ([type varname & forms]),
   :name "defspan",
   :namespace "clj-span.model-lang",
   :source-url nil,
   :raw-source-url nil,
   :wiki-url
   "/clj-span.model-lang-api.html#clj-span.model-lang/defspan",
   :doc "Define SPAN model components.",
   :var-type "macro",
   :line 132,
   :file "src/clj_span/model_lang.clj"}
  {:arglists ([path? fishing-spot? rows cols use-points]),
   :name "find-shortest-paths-to-coast",
   :namespace "clj-span.models.subsistence-fisheries",
   :source-url nil,
   :raw-source-url nil,
   :wiki-url
   "/clj-span.models.subsistence-fisheries-api.html#clj-span.models.subsistence-fisheries/find-shortest-paths-to-coast",
   :doc "Fuck it. I'm just drawing a line and going to bed.",
   :var-type "function",
   :line 187,
   :file "src/clj_span/models/subsistence_fisheries.clj"})}
