(TeX-add-style-hook "literate"
 (lambda ()
    (LaTeX-add-environments
     '("chunk" 1))
    (TeX-add-symbols
     '("getchunk" 1)
     "atcode"
     "longdash")
    (TeX-run-style-hooks
     "verbatim")))

