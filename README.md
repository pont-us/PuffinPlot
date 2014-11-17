# PuffinPlot development notes

PuffinPlot is a program for palaeomagnetic data plotting and analysis,
written by Pontus Lurcock. This readme describes the requirements for
building PuffinPlot and its documentation from source, and is intended
for developers rather than users. Users are advised to download the
latest PuffinPlot release and consult the included user manual instead.

## Building PuffinPlot

PuffinPlot is developed as a NetBeans project (http://netbeans.org/),
but NetBeans is not necessary to build PuffinPlot: the build is
controlled by an Ant project file (build.xml), which can be used without
NetBeans provided that Apache Ant (http://ant.apache.org/) is installed.

### Required Java libraries

PuffinPlot uses the following external Java libraries, or parts of them:

* iText http://itextpdf.com/
* batik http://xmlgraphics.apache.org/batik/
* FreeHEP http://java.freehep.org/
* jython http://www.jython.org/
* appbundler https://java.net/projects/appbundler

These libraries are not included in the PuffinPlot repository itself.
The NetBeans project and build script look for the required jar files
in a directory called `puffinplot-lib`, which should be in the parent
directory of the PuffinPlot directory itself. The `puffinplot-lib`
directory should contain the following jar files:

| Filename                    | Version                  |
| --------------------------- | ------------------------ |
| batik-awt-util.jar          | 1.7                      |
| batik-dom.jar               | 1.7                      |
| batik-svggen.jar            | 1.7                      |
| batik-util.jar              | 1.7                      |
| freehep-export.jar          | 2.1.1                    |
| freehep-graphics2d.jar      | 2.1.1                    |
| freehep-graphicsio.jar      | 2.1.1                    |
| freehep-graphicsio-pdf.jar  | 2.1.1                    |
| freehep-graphicsio-svg.jar  | 2.1.1                    |
| freehep-io.jar              | 2.0.2                    |
| freehep-swing.jar           | 2.0.3                    |
| freehep-util.jar            | 2.0.2                    |
| freehep-xml.jar             | 2.1.1                    |
| itext.jar                   | 2.1.7                    |
| jython.jar                  | 2.5.3                    |
| appbundler.jar              | 1.0                      |

The script `fetch-libs.sh` in the PuffinPlot directory will
automatically create the `puffinplot-lib` directory, download the
required software packages, and extract the jar files. (Download
locations correct as of 2014-10-16.)

appbundler is not used by PuffinPlot itself, but it used during the
build process to create a Mac application bundle containing PuffinPlot.
Note that there is more than one library called ‘appbundler’;
PuffinPlot uses the one provided by Oracle.

### TeX dependencies

The manual is produced using LaTeX. The following programs and packages are
required to build it. For each package name, the name of the Debian package
containing it is also given.

On most Debian-based operating systems, all the TeX dependencies should
be installable with the command

```
sudo apt-get install tex4ht texlive-fonts-extra texlive-fonts-recommended texlive-latex-base texlive-latex-extra texlive-latex-recommended
```

#### Programs

* pdflatex : texlive-latex-base
* htlatex : tex4ht

#### Packages

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

Ant build targets:

* `create-release` creates a zip file suitable for distribution.
It contains the PuffinPlot jar, a Mac application folder, the
user manual, and JavaDoc. This target depends on most of the
other targets.
* `package-for-store` creates a unified PuffinPlot jar, including
the external libraries as well as the PuffinPlot code itself.
* `build-manual` creates PDF and HTML manuals from the LaTeX sources.
* `macbundle` creates a Mac OS X application from a PuffinPlot jar.
* `upload-release` uploads a release to Google Code project hosting.

For the `upload-release` target, which uploads binaries and archives to
Google project hosting, a valid user name and password must be supplied
in a file named build.credentials.properties. The file
build.credentials.example shows the correct format for the credentials
file.

It is possible for large uploads to exceed Ant's available memory. At the
time of writing, this is not a problem, but if it becomes a problem
the solution is to increase the memory allocation pool size using
the ANT_OPTS environment variable, thus:

```
ANT_OPTS=-Xmx512m
```

(or some other suitably large value).

## Bug tracking

PuffinPlot uses the Bugs Everywhere bug-tracking system; the bug
database lives within the repository itself, in the `.be` folder. Bugs
Everywhere is available from http://www.bugseverywhere.org/ .

