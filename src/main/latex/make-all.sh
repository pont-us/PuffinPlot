#!/bin/sh

# Abort on error

set -e

# Remove various automatically generated auxiliary files

for ext in aux log 4ct 4tc bbl blg tmp xref out idv lg css dvi html pdf toc; do
  rm -f manual.$ext;
done
rm -f manual?x.png
rm -f missfont.log
rm -f changes.tex

# Create commands to insert version control information

./create-git-cmds.py

# Convert SVG figures to PNG and PDF. DPI values for PNG conversion
# are empirically chosen to produce output with a width of 800 pixels.

inkscape --export-area-page --without-gui \
	 --export-png=figures/data-model.png \
	 --export-dpi=135.5 fig-src/data-model.svg

inkscape --export-area-page --without-gui \
	 --export-pdf=figures/data-model.pdf \
	 fig-src/data-model.svg

inkscape --export-area-page --without-gui \
	 --export-png=figures/annot-scrnshot.png \
	 --export-dpi=134.5 fig-src/annot-screenshot/annot-screenshot.svg

inkscape --export-area-page --without-gui \
	 --export-png=figures/annot-scrnshot.pdf \
	 fig-src/annot-screenshot/annot-screenshot.svg

# Convert Markdown changelog in parent directory to LaTeX for inclusion
# in the manual

pandoc -r markdown-auto_identifiers \
       ../../../CHANGES.md \
       -o changes.tex

# Demote and denumber the sections in the changelog

sed -i \
    -e 's/^\\subsection/\\subsubsection*/' \
    -e 's/^\\section/\\subsection*/' \
    changes.tex 

# Generate PDF manual

pdflatex manual
bibtex manual
pdflatex manual
pdflatex manual

# Generate HTML manual

htlatex manual
bibtex manual
htlatex manual
htlatex manual
