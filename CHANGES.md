PuffinPlot 1.4 release notes
============================

The release numbering scheme changed with the 1.4 release: 1.4 is
the immediate successor of 1.03.

Installation requirements
-------------------------

-   PuffinPlot now requires Java 8 or higher.


Data manipulation
-----------------

-   "Discrete to continuous" feature implemented, allowing conversion
    of discrete suites to continuous suites using a supplementary
    CSV file containing a sample-to-depth map.
-   Automatic realignment of declination data, allowing reconstruction of
    a continuous declination record for continuous suites in which the
    core sections are not azimuthally oriented with respected to each
    other.
-   Allow user to remove samples outside a specified depth range.
-   Allow user to remove samples with a specified treatment type.


Calculations
------------

-   RPI calculation implemented, using normalization to ARM, IRM, or
    magnetic susceptibility.

Scripting
---------

-   The Jython package is no longer bundled as part of PuffinPlot,
    dramatically reducing the download size. PuffinPlot still retains
    Python support: Jython is downloaded and installed automatically
    if and when it is required.
-   PuffinPlot now supports scripting in JavaScript as well as Python.
-   The user-defined great circle validity check now uses Javascript
    rather than Python. In practice, this only means a slight change
    in syntax.

User interface
--------------

-   Files can now be opened by dragging them to the main window.
-   Current suite name and "unsaved" indicator added to the title bar.
-   When saving files, PuffinPlot now defaults to the last-used
    folder.
-   Status bar added to main window. When the mouse pointer is over
    a data point representing a demagnetization step, the status bar
    shows details of this step.
-   The "Edit" menu has been divided into more submenus to more
    conveniently accommodate the newly added functions.

Graphing and data display
-------------------------

-   Per-sample Fisher statistics are now shown in the sample parameters
    legend.
-   R parameter now shown to 4 decimal places in site and suite parameter
    tables.

Data import
-----------

-   PuffinPlot can now import the text-based PMD format developed by
    R\. Enkin and supported by several palaeomagnetic programs including
    PMGSC, Paleomac, and Remasoft.
-   PuffinPlot can now import the JR6 format developed by AGICO and
    supported by programs including REMA6W, Remasoft, and Paleomac.
-   When importing from a custom file format, PuffinPlot now sets
    default values for the sample and formation corrections if these
	are not specified in the file.
-   When importing AMS data, PuffinPlot can now read a formation correction
    from the ASC file. It can also, optionally, overwrite existing sample
    and formation corrections with values read from the ASC file.
-   Import from Caltech (CIT) files has been improved: demagnetization
    levels for thermal treatment are now read correctly, as is the "NRM"
    treatment code.

Data export
-----------

-   Degree signs in exported data replaced with "deg", to accommodate
    software that has trouble with non-ASCII character sets.
-   Precision of exported parameters increased to four decimal places.
-   PuffinPlot can now export a bundle containing both data and a
    processing script, allowing analyses to be reproduced automatically.

Miscellaneous bug fixes
-----------------------

-   Custom flags and notes are now included in exported sample data.
-   PDF export no longer produces an extra page.
-   Display is now updated automatically when sites are edited.
-   Formatting of PCA equations in exported CSV files is now
    locale-independent, so will always use "." rather than ","
    as the decimal separator.
-   Custom file import used to ignore the sample volume field; it now
    makes use of it if present, and defaults to a volume of 1 cm³
    if it is not present.
-   In demagnetization / intensity plots with AF treatment type, data
	points for magnetic susceptibility measurements (if present) could
	sometimes be plotted with an incorrect x position. This has now been
	corrected.

Developer notes
---------------

-   The build process now uses the Ivy dependency manager to download
    required libraries automatically.
-   Several hundred unit tests have been added, mainly for the data
    and calculation classes. This helps to verify the correctness of
    PuffinPlot's data processing, and to avoid the introduction of
    bugs during future development.
-   PuffinPlot's version control has been migrated from Mercurcial
    to Git, and the main repository from Bitbucket to GitHub.
    

PuffinPlot 1.03 release notes
=============================

Calculations
------------

-   Virtual geomagnetic pole calculation.
-   Fisher-by-site calculations can be done on continuous data sets.
-   Fisher analysis of demagnetization steps.
-   PCA and GC fits can be cleared individually.
-   R added to the available Fisher statistical parameters.

Data plotting
-------------

-   Horizontal projection in Zplot supports west-upward orientation.
-   Data points can be labelled with treatment step.
-   Equal-area plots can be labelled to avoid confusion.
-   Site equal-area plots now distinguish PCAs, demag steps, GC poles,
    and site means.
-   Current site and sample are highlighted in the relevant data tables
    and plots.
-   Sample directions can be annotated with their names in the site
    equal-area plot.
-   a95 added to site parameter table.
-   More compact default plot layout.
-   Treatment steps can be labelled with the treatment level.
-   Added suite parameter table for mean directions and VGPs.
-   Site alpha-95s can be shown in the suite equal-area plot.

Data import
-----------

-   Selectable units for custom data import.
-   More variants of the AGICO AMS file can now be imported.
-   Direct import of sample directions.
-   IAPD file import.
-   Caltech file import.
-   Better guessing of measurement type in 2G files.
-   Site location data import (for use with VGP calculation).
-   More palaeomagnetic data can now be appended to an existing suite.

Data export
-----------

-   n and a95 are listed in sample parameter file.
-   Great circle strikes and dips are included in exported data.

Bug fixes
---------

-   Exporting FreeHEP SVG graphics no longer disables anti-aliasing.
-   PuffinPlot no longer crashes if an incorrect 2G protocol is
    specified.
-   PuffinPlot will not save its own file over the original data file.
-   Cleared PCA directions no longer reappear.
-   Fixed potential crash during suite parameter export.
-   Measurement types now checked for consistency when opening a file.

Documentation
-------------

-   All new features are fully documented in the updated user manual

Miscellaneous new features
--------------------------

-   Site calculations automatically update when sample calculations
    change.
-   Site directions cleared automatically when all their sample
    directions are cleared.
-   Warning when closing a file or quitting with unsaved data
-   Clearer error messages and warnings when reading corrupted files
-   Treatment type can be set manually
-   Site definitions can be cleared
-   Treatment steps can be deselected by dragging a box.
-   PuffinPlot will ask for confirmation before overwriting files.
-   Native file open dialog is now used on Mac OS X.
-   Added "invert sample moment" feature.
-   "Open file" dialogs now remember the last used folder.
-   Sample volume can be edited.

Other notes
-----------

-   PuffinPlot now requires Java 7 or higher.
-   PuffinPlot is now hosted on BitBucket, due to the imminent demise of
    Google Code.

