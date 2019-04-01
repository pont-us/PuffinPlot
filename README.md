# PuffinPlot development notes

PuffinPlot is a program for palaeomagnetic data plotting and analysis,
written by Pontus Lurcock. This readme describes the requirements for
building PuffinPlot and its documentation from source, and is intended
for developers rather than users. Users are advised to download the
latest PuffinPlot release from the project website at
http://talvi.net/puffinplot/ and consult the included user manual.

## Building PuffinPlot

PuffinPlot is developed as a NetBeans project (http://netbeans.org/),
but NetBeans is not necessary to build PuffinPlot: the build is
controlled by an Ant project file (build.xml), which can be used without
NetBeans provided that Apache Ant (http://ant.apache.org/) is installed.

### Dependencies: Java libraries

The PuffinPlot build process uses the Ivy dependency manager to
automatically fetch dependencies from the Internet if required.

PuffinPlot uses the following external Java libraries, or parts of them:

* appbundler https://java.net/projects/appbundler
* batik http://xmlgraphics.apache.org/batik/
* commons-cli https://commons.apache.org/proper/commons-cli/
* commons-compress http://commons.apache.org/proper/commons-compress/
* commons-math http://commons.apache.org/proper/commons-math/
* FreeHEP http://java.freehep.org/
* iText http://itextpdf.com/
* JAMA https://math.nist.gov/javanumerics/jama/doc/
* OrangeExtensions https://ymasory.github.io/OrangeExtensions/

appbundler is not used by PuffinPlot itself, but is used during the
build process to create a Mac application bundle containing PuffinPlot.
Note that there is more than one library called ‘appbundler’;
PuffinPlot uses the one provided by Oracle.

Jython can be dynamically downloaded, installed, and used by PuffinPlot
at runtime, but is not a build dependency.

### Dependencies: user manual

The manual is typeset as PDF and HTML using LaTeX. Some PDF and PNG
figures for the manual are automatically generated from SVG sources
using Inkscape when the manual is built.

On most Debian-based operating systems, all the TeX dependencies should
be installable with the command

```
sudo apt install tex4ht texlive-fonts-extra texlive-fonts-recommended texlive-latex-base texlive-latex-extra texlive-latex-recommended inkscape
```

The following programs and packages are required to build it. For each
program or package name, the name of the Debian package containing it is
also given.

#### Programs

* pdflatex : texlive-latex-base
* htlatex : tex4ht
* inkscape : inkscape

#### TeX Packages

* babel : texlive-latex-base
* geometry : texlive-latex-base
* ifpdf : texlive-latex-base
* calc : texlive-latex-base
* fontenc : texlive-latex-base
* mathdesign : texlive-fonts-extra
* natbib : texlive-latex-base
* inputenc : texlive-latex-base
* url : texlive-latex-recommended
* graphicx : texlive-latex-base
* placeins : texlive-latex-extra
* booktabs : texlive-latex-recommended
* listings : texlive-latex-recommended
* textcase : texlive-latex-recommended
* color : texlive-latex-base
* textcomp : texlive-latex-base
* hyperref : texlive-latex-base

#### Fonts

* Bitstream Charter : texlive-fonts-recommended

## Building with Ant

Main Ant build targets:

* `create-release` creates a zip file suitable for distribution.
It contains the PuffinPlot jar, a Mac application folder, the
user manual, and JavaDoc. This target depends on most of the
other targets.
* `combined-jar` creates a unified PuffinPlot jar, including
the external libraries as well as the PuffinPlot code itself.
* `build-manual` creates PDF and HTML manuals from the LaTeX sources.
* `macbundle` creates a Mac OS X application from a PuffinPlot jar.

## Bug tracking

PuffinPlot uses the Bugs Everywhere bug-tracking system; the bug
database lives within the repository itself, in the `.be` folder. Bugs
Everywhere is available from http://www.bugseverywhere.org/ .
