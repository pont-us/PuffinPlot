#!/bin/sh

for ext in aux log 4ct 4tc bbl blg tmp xref out idv lg css dvi html pdf toc; do
  rm manual.$ext;
done
hg tip --template '\\def\\HgNode{{node|short}}\n\\def\\HgDate{{date|isodate}}\n\\def\\HgAuthor{{author|person}}\n' >hg.id
pdflatex manual
bibtex manual
pdflatex manual
pdflatex manual
htlatex manual
bibtex manual
htlatex manual
htlatex manual
