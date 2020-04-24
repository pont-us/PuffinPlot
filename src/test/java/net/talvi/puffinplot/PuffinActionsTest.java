/* This file is part of PuffinPlot, a program for palaeomagnetic
 * data plotting and analysis. Copyright 2012-2019 Pontus Lurcock.
 *
 * PuffinPlot is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * PuffinPlot is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with PuffinPlot. If not, see <http://www.gnu.org/licenses/>.
 */
package net.talvi.puffinplot;

import java.awt.GraphicsEnvironment;
import java.util.function.Consumer;
import net.talvi.puffinplot.data.AmsCalculationType;
import net.talvi.puffinplot.data.MeasurementAxis;
import net.talvi.puffinplot.data.Sample;
import net.talvi.puffinplot.data.Suite;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

public class PuffinActionsTest {

    final PuffinApp app = Mockito.mock(PuffinApp.class);
    final PuffinActions actions = new PuffinActions(app);

    @BeforeClass
    public static void assumeNotHeadless() {
        org.junit.Assume.assumeFalse(
                "Can't run PuffinActions tests in a headless environment.",
                GraphicsEnvironment.isHeadless());
    }
    
    @Test
    public void testAbout() {
        actions.about.actionPerformed(null);
        Mockito.verify(app).about();
    }
    
    @Test
    public void testOpen() {
        actions.open.actionPerformed(null);
        Mockito.verify(app).showOpenFilesDialog(true);
    }
    
    @Test
    public void testAppendFiles() {
        actions.appendFiles.actionPerformed(null);
        Mockito.verify(app).showOpenFilesDialog(false);
    }
    
    @Test
    public void testOpenFolder() {
        actions.openFolder.actionPerformed(null);
        Mockito.verify(app).showMacOpenFolderDialog();
    }
    
    @Test
    public void testClose() {
        actions.close.actionPerformed(null);
        Mockito.verify(app).closeCurrentSuite();
    }
    
    @Test
    public void testExportCalcsMultisuite() {
        actions.exportCalcsMultiSuite.actionPerformed(null);
        Mockito.verify(app).exportCalcsMultiSuite();
    }
    
    @Test
    public void testSave() {
        actions.save.actionPerformed(null);
        Mockito.verify(app).save();
    }
    
    @Test
    public void testSaveAsNoSuite() {
        actions.saveAs.actionPerformed(null);
        Mockito.verify(app, Mockito.never()).showSaveAsDialog(Mockito.any());
    }
    
    @Test
    public void testSaveAsWithSuite() {
        final Suite suite = new Suite("test");
        Mockito.when(app.getCurrentSuite()).thenReturn(suite);
        actions.saveAs.actionPerformed(null);
        Mockito.verify(app).showSaveAsDialog(Mockito.any());
    }
    
    @Test
    public void testPageSetup() {
        actions.pageSetup.actionPerformed(null);
        Mockito.verify(app).showPageSetupDialog();
    }
    
    @Test
    public void testFlipSampleX() {
        actions.flipSampleX.actionPerformed(null);
        Mockito.verify(app).flipSelectedSamples(MeasurementAxis.X);
    }
    
    @Test
    public void testFlipSampleY() {
        actions.flipSampleY.actionPerformed(null);
        Mockito.verify(app).flipSelectedSamples(MeasurementAxis.Y);
    }
    
    @Test
    public void testFlipSampleZ() {
        actions.flipSampleZ.actionPerformed(null);
        Mockito.verify(app).flipSelectedSamples(MeasurementAxis.Z);
    }
    
    @Test
    public void testInvertSamples() {
        actions.invertSamples.actionPerformed(null);
        Mockito.verify(app).invertSelectedSamples();
    }
    
    @Test
    public void pcaOnSelection() {
        actions.pcaOnSelection.actionPerformed(null);
        Mockito.verify(app).doPcaOnSelection();
    }
    
    @Test
    public void testCircleFit() {
        actions.circleFit.actionPerformed(null);
        Mockito.verify(app).fitGreatCirclesToSelection();
    }
    
    @Test
    public void testGreatCircleAnalysis() {
        actions.greatCircleAnalysis.actionPerformed(null);
        Mockito.verify(app).calculateGreatCirclesDirections();
    }
    
    @Test
    public void testClearSiteCalcs() {
        actions.clearSiteCalcs.actionPerformed(null);
        Mockito.verify(app).clearSiteCalculations();
    }
    
    @Test
    public void testClearSamplePca() {
        actions.clearSamplePca.actionPerformed(null);
        final Sample sample = modifySelectedSamplesHelper();
        Mockito.verify(sample).clearPca();
        Mockito.verifyNoMoreInteractions(sample);
    }
    
    @Test
    public void testClearSampleGreatCircle() {
        actions.clearSampleGreatCircle.actionPerformed(null);
        final Sample sample = modifySelectedSamplesHelper();
        Mockito.verify(sample).clearGreatCircle();
        Mockito.verifyNoMoreInteractions(sample);
    }

    @Test
    public void testClearSampleCalcs() {
        actions.clearSampleCalcs.actionPerformed(null);
        final Sample sample = modifySelectedSamplesHelper();
        Mockito.verify(sample).clearCalculations();
        Mockito.verifyNoMoreInteractions(sample);
    }
    
    /**
     * We can't directly compare method references (two different instances
     * of "Sample::clearPca" will give two different lambdas) so this is
     * a workaround to check, imperfectly, that the correct sample consumer
     * method reference is passed to PuffinApp#modifySelectedSamples:
     * the sample-consumer argument to modifySelectedSamples is captured
     * and invoked on a mocked sample. The mocked sample is passed back to
     * the caller, who can verify the interaction on it.
     * 
     * @return 
     */
    private Sample modifySelectedSamplesHelper() {
        ArgumentCaptor<Consumer<Sample>> captor =
                ArgumentCaptor.forClass(Consumer.class);
        Mockito.verify(app).modifySelectedSamples(captor.capture());
        final Sample sample = Mockito.mock(Sample.class);
        final Consumer<Sample> consumer = captor.getValue();
        consumer.accept(sample);
        return sample;
    }
    
    @Test
    public void testCopyStepSelection() {
        actions.copyStepSelection.actionPerformed(null);
        Mockito.verify(app).copyStepSelection();
    }
    
    @Test
    public void testPasteStepSelection() {
        actions.pasteStepSelection.actionPerformed(null);
        Mockito.verify(app).pasteStepSelection();
    }
    
    @Test
    public void testPrefs() {
        actions.prefs.actionPerformed(null);
        Mockito.verify(app).showPreferences();
    }
    
    @Test
    public void testPrint() {
        actions.print.actionPerformed(null);
        Mockito.verify(app).showPrintDialog("MAIN");
    }
    
    @Test
    public void testPrintSiteEqualArea() {
        actions.printSiteEqualArea.actionPerformed(null);
        Mockito.verify(app).showPrintDialog("SITE");
    }
    
    @Test
    public void testPrintSuiteEqualArea() {
        actions.printSuiteEqualArea.actionPerformed(null);
        Mockito.verify(app).showPrintDialog("SUITE");
    }
    
    @Test
    public void testQuit() {
        actions.quit.actionPerformed(null);
        Mockito.verify(app).quit();
    }
    
    @Test
    public void testImportAms() {
        actions.importAms.actionPerformed(null);
        Mockito.verify(app).showImportAmsDialog();
    }
    
    @Test
    public void testMultiSuiteMeans() {
        actions.multiSuiteMeans.actionPerformed(null);
        Mockito.verify(app).calculateMultiSuiteMeans();
    }
    
    @Test
    public void testShowCustomFlagsWindow() {
        actions.showCustomFlagsWindow.actionPerformed(null);
        Mockito.verify(app).showCustomFlagsWindow();
    }
    
    @Test
    public void testShowCustomNotesWindow() {
        actions.showCustomNotesWindow.actionPerformed(null);
        Mockito.verify(app).showCustomNotesWindow();
    }
    
    @Test
    public void testBootAmsNaive() {
        actions.bootAmsNaive.actionPerformed(null);
        Mockito.verify(app).doAmsCalc(AmsCalculationType.BOOT, "bootams.py");
    }
    
    @Test
    public void testBootAmsParam() {
        actions.bootAmsParam.actionPerformed(null);
        Mockito.verify(app).
                doAmsCalc(AmsCalculationType.PARA_BOOT, "bootams.py");
    }
    
    @Test
    public void testHestAms() {
        actions.hextAms.actionPerformed(null);
        Mockito.verify(app).doAmsCalc(AmsCalculationType.HEXT, "s_hext.py");
    }
    
    @Test
    public void testRescaleMagSus() {
        actions.rescaleMagSus.actionPerformed(null);
        Mockito.verify(app).showRescaleMagSusDialog();
    }
    
    @Test
    public void clearPreferences() {
        actions.clearPreferences.actionPerformed(null);
        Mockito.verify(app).clearPreferences();
    }
    
    @Test
    public void testImportPrefs() {
        actions.importPrefs.actionPerformed(null);
        Mockito.verify(app).showImportPreferencesDialog();
    }
    
    @Test
    public void testClearAmsCalcs() {
        actions.clearAmsCalcs.actionPerformed(null);
        Mockito.verify(app).clearAmsCalcs();
    }
    
    @Test
    public void testRunJavascriptScript() {
        actions.runJavascriptScript.actionPerformed(null);
        Mockito.verify(app).showRunJavascriptScriptDialog();
    }
    
    @Test
    public void testRunPythonScript() {
        actions.runPythonScript.actionPerformed(null);
        Mockito.verify(app).showRunPythonScriptDialog();
    }
    
    @Test
    public void testCalculateRpi() {
        actions.calculateRpi.actionPerformed(null);
        Mockito.verify(app).showCalculateRpiDialog();
    }
    
    @Test
    public void testCreateBundle() {
        actions.createBundle.actionPerformed(null);
        Mockito.verify(app).showCreateBundleDialog();
    }
    
    @Test
    public void testConvertDiscreteToContinuout() {
        actions.convertDiscreteToContinuous.actionPerformed(null);
        Mockito.verify(app).showDiscreteToContinuousDialog();
    }
    
    @Test
    public void testImportLocations() {
        actions.importLocations.actionPerformed(null);
        Mockito.verify(app).showImportLocationsDialog();
    }

    @Test
    public void testOpenPuffinWebsite() {
        actions.openPuffinWebsite.actionPerformed(null);
        Mockito.verify(app).openWebPage("http://talvi.net/puffinplot");
    }
}
