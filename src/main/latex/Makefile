manual.pdf: manual.tex
	hg tip --template '\\def\\HgNode{{node|short}}\n\\def\\HgDate{{date|isodate}}\n\\def\\HgAuthor{{author|person}}\n' >hg.id
	pdflatex manual
	bibtex manual
	pdflatex manual
	pdflatex manual
