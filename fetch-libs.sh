#!/bin/bash

### create directories

mkdir -p ../puffinplot-lib/packages
cd ../puffinplot-lib/packages

### download packages

wget http://central.maven.org/maven2/org/python/jython-installer/2.5.3/jython-installer-2.5.3.jar

wget http://archive.apache.org/dist/xmlgraphics/batik/batik-1.7.zip

wget ftp://ftp.slac.stanford.edu/software/freehep/VectorGraphics/v2.1.1/vectorgraphics-2.1.1-bin.tar.gz

wget http://central.maven.org/maven2/com/lowagie/itext/2.1.7/itext-2.1.7.jar

wget https://java.net/projects/appbundler/downloads/download/appbundler-1.0.jar

wget http://central.maven.org/maven2/org/apache/commons/commons-compress/1.9/commons-compress-1.9.jar

### unpack the needed libraries

cd ..

# Pull the "bare" jython jar out of the installer package --
# the extra libraries aren't necessary.
jar xf packages/jython-installer-2.5.3.jar jython.jar

# Batik dependencies:
# batik-svggen batik-awt-util batik-util batik-dom
# (xml-apis part of Java since 1.6)
unzip -j packages/batik-1.7.zip \
  batik-1.7/lib/batik-svggen.jar \
  batik-1.7/lib/batik-awt-util.jar \
  batik-1.7/lib/batik-util.jar \
  batik-1.7/lib/batik-dom.jar

# Pull the requisite FreeHEP jars out of the archive,
# stripping the path prefix and version number.
tar -zxv --strip-components=2 --transform s/-[0-9.]*jar/.jar/ \
  -f packages/vectorgraphics-2.1.1-bin.tar.gz \
  vectorgraphics-2.1.1/lib/freehep-graphicsio-pdf-2.1.1.jar \
  vectorgraphics-2.1.1/lib/freehep-export-2.1.1.jar \
  vectorgraphics-2.1.1/lib/freehep-graphics2d-2.1.1.jar \
  vectorgraphics-2.1.1/lib/freehep-io-2.0.2.jar \
  vectorgraphics-2.1.1/lib/freehep-swing-2.0.3.jar \
  vectorgraphics-2.1.1/lib/freehep-util-2.0.2.jar \
  vectorgraphics-2.1.1/lib/freehep-graphicsio-svg-2.1.1.jar \
  vectorgraphics-2.1.1/lib/freehep-xml-2.1.1.jar \
  vectorgraphics-2.1.1/lib/freehep-graphicsio-2.1.1.jar

# The itext, appbundler, and commons-compress jars are not additionally
# packaged, so we can just link them.

ln -s packages/itext-2.1.7.jar itext.jar
ln -s packages/appbundler-1.0.jar appbundler.jar
ln -s packages/commons-compress-1.9.jar commons-compress.jar
