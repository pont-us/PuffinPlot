#!/bin/sh

set -e

for ext in aux log 4ct 4tc bbl blg tmp xref out idv lg css dvi html pdf toc; do
  rm -f manual.$ext;
done

rm -f missfont.log

./create-hg-cmds.py

pdflatex manual
bibtex manual
pdflatex manual
pdflatex manual
htlatex manual
bibtex manual
htlatex manual
htlatex manual
