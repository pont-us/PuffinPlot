#!/bin/bash

### create directories

mkdir -p lib/packages
cd lib/packages

### download packages

wget http://archive.apache.org/dist/xmlgraphics/batik/batik-1.7.zip
# TODO: use the Maven repository to get individual jars instead:
# http://central.maven.org/maven2/org/apache/xmlgraphics/batik-svggen/1.7/batik-svggen-1.7.jar
# http://central.maven.org/maven2/org/apache/xmlgraphics/batik-awt-util/1.7/batik-awt-util-1.7.jar
# http://central.maven.org/maven2/org/apache/xmlgraphics/batik-util/1.7/batik-util-1.7.jar
# http://central.maven.org/maven2/org/apache/xmlgraphics/batik-dom/1.7/batik-dom-1.7.jar

wget ftp://ftp.slac.stanford.edu/software/freehep/VectorGraphics/v2.1.1/vectorgraphics-2.1.1-bin.tar.gz
# TODO: Get individual jars from http://java.freehep.org/maven/org.freehep/jars/

wget http://central.maven.org/maven2/com/lowagie/itext/2.1.7/itext-2.1.7.jar

wget https://java.net/projects/appbundler/downloads/download/appbundler-1.0.jar

wget http://central.maven.org/maven2/org/apache/commons/commons-compress/1.9/commons-compress-1.9.jar

wget http://central.maven.org/maven2/commons-cli/commons-cli/1.2/commons-cli-1.2.jar

wget http://central.maven.org/maven2/org/apache/commons/commons-math3/3.5/commons-math3-3.5.jar

### unpack the needed libraries

cd ..

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

# The itext, appbundler, commons-compress, commons-cli, and commons-math
# jars are not additionally packaged, so we can just link them.

ln -s packages/itext-2.1.7.jar itext.jar
ln -s packages/appbundler-1.0.jar appbundler.jar
ln -s packages/commons-compress-1.9.jar commons-compress.jar
ln -s packages/commons-cli-1.2.jar commons-cli.jar
ln -s packages/commons-math3-3.5.jar commons-math.jar
