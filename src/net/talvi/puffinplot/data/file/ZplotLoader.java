//package net.talvi.puffinplot.data.file;
//
//public class ZplotLoader {
//
//    final private static String[] ZPLOT_HEADERS =
//      {"Sample", "Project", "Demag", "Declin", "Inclin", "Intens", "Operation"};
//
//                        reader = new LineNumberReader(new FileReader(file));
//                    // Check first line for magic string
//                    if (!reader.readLine().startsWith("File Name:")) {
//                        addWarning("Ignoring unrecognized file %s", fileName);
//                        reader.close();
//                        break fileTypeSwitch;
//                    }
//                    // skip remaining header fields
//                    for (int i = 0; i < 5; i++) reader.readLine();
//                    String headerLine = reader.readLine();
//                    if (headerLine == null) {
//                        addWarning("Ignoring malformed ZPlot file %s", file.getName());
//                        reader.close();
//                        break fileTypeSwitch;
//                    }
//                    String[] headers = whitespace.split(headerLine);
//
//                    if (headers.length != 7) {
//                        addWarning("Wrong number of header fields in Zplot file %s:" +
//                                ": expected 7, got %s", fileName, headers.length);
//                        reader.close();
//                        break fileTypeSwitch;
//                    }
//                    for (int i = 0; i < ZPLOT_HEADERS.length; i++) {
//                        if (!ZPLOT_HEADERS[i].equals(headers[i])) {
//                            addWarning("Unknown header field %s in file %s " +
//                                    " -- aborting load.", headers[i], fileName);
//                            reader.close();
//                            break fileTypeSwitch;
//                        }
//                    }
//
//    public Datum(String zPlotLine) {
//        line = null;
//        Scanner s = new Scanner(zPlotLine);
//        s.useLocale(Locale.ENGLISH); // don't want to be using commas as decimal separators...
//        s.useDelimiter(delimPattern); // might have spaces within fields
//
//        String depthOrSample = s.next();
//        measType = (numberPattern.matcher(depthOrSample).matches())
//                ? MeasType.CONTINUOUS
//                : MeasType.DISCRETE;
//        switch (measType) {
//        case CONTINUOUS: depth = Double.parseDouble(depthOrSample);
//            break;
//        case DISCRETE: sampleId = depthOrSample;
//            break;
//        default: throw new Error("Unhandled measurement type "+measType);
//        }
//        String project = s.next();
//        double afOrThermalDemag = s.nextDouble();
//        decUc = s.nextDouble();
//        incUc = s.nextDouble();
//        intensity = s.nextDouble();
//        String operation = s.next();
//        magSus = Double.NaN;
//        uc = Vec3.fromPolarDegrees(intensity, incUc, decUc);
//        // fc = sc = uc;
//        treatType = TreatType.DEGAUSS_XYZ;
//        if (project.toLowerCase().contains("therm") ||
//                operation.toLowerCase().contains("therm"))
//            treatType = TreatType.THERMAL;
//        switch (treatType) {
//        case DEGAUSS_XYZ: afx = afy = afz = afOrThermalDemag;
//        break;
//        case THERMAL: temp = afOrThermalDemag;
//        break;
//        default: throw new Error("Can't happen.");
//        }
//    }
//
//}
