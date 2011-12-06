all: hg.id
	pdflatex manual

hg.id:
	hg tip --template '\\def\\HgNode{{node|short}}\n\\def\\HgDate{{date|isodate}}\n\\def\\HgAuthor{{author|person}}\n' >$@
