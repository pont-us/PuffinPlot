/* This file is part of PuffinPlot, a program for palaeomagnetic
 * data plotting and analysis. Copyright 2012-2018 Pontus Lurcock.
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

import org.junit.Test;
import org.mockito.Mockito;

public class PuffinActionsTest {

    final PuffinApp app = Mockito.mock(PuffinApp.class);
    final PuffinActions actions = new PuffinActions(app);
    
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
    public void testPageSetup() {
        actions.pageSetup.actionPerformed(null);
        Mockito.verify(app).showPageSetupDialog();
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
        Mockito.verify(app).calculateRpi();
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
