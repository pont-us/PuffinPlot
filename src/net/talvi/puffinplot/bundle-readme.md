PuffinPlot data bundle
======================

This archive contains palaeomagnetic data processed using the PuffinPlot
application ( http://talvi.net/puffinplot ), as well as results of analysis
and a script to automatically run PuffinPlot and reproduce the results from
the raw data. The archive may optionally contain a copy of the PuffinPlot
program itself.

Archive contents
----------------

README.md
:   This file

data.ppl
:   PuffinPlot data file containing palaeomagnetic measurements and
    parameters for analysis

data-sample.csv
:   Sample-level analysis results such as PCA directions and median
    destructive field values, in comma-separated value format.

data-site.csv
:   Site-level analysis results, such as mean directions from multiple
    samples at a site, in comma-separated value format. This file may
    be absent if no sites were defined for the suite.

data-suite.csv
:   Suite-level analysis results, such as the mean direction across all
	sites, in comma-separated value format.

process-data.sh
:   A Unix shell script which runs PuffinPlot and reproduces the
    analyses from the data in the data.ppl file

process-data.bat
:   A Windows shell script which runs PuffinPlot and reproduces the
    analyses from the data in the data.ppl file

PuffinPlot.jar
:   The PuffinPlot program itself, as a Java archive file. This file
    may be omitted; see the next section for details.


Examining and reproducing the analyses
--------------------------------------

The PuffinPlot application itself may optionally be included in the
archive. If so, it is present as a file named `PuffinPlot.jar`. This is
a Java software archive file which can be run on any system with a Java
runtime environment (version 8 or later) installed; Java is freely
available at http://java.com . If the PuffinPlot application is not
present in the archive, it may be downloaded via the download links at
http://talvi.net/puffinplot .

PuffinPlot can be used to open and the data file `data.ppl`, allowing
the data and analyses to be interactively examined and modified.
PuffinPlot can also be run in non-interactive mode to automatically
recreate the analysis results from the data file. For convenience, two
scripts (`process-data.sh` for Unix, Linux, and Mac systems, and
`process-data.bat` for Windows systems) are provided for this purpose.
The scripts assume that Java is installed (with the `java` executable on
the search path) and that the `PuffinPlot.jar` file is in the same
folder.

