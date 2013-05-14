#!/bin/sh
lein doc
cd doc
git add -A
git commit -m "Documentation update"
git push origin gh-pages
