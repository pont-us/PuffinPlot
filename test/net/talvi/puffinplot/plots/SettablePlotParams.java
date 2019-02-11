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
package net.talvi.puffinplot.plots;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.talvi.puffinplot.data.Correction;
import net.talvi.puffinplot.data.MeasurementAxis;
import net.talvi.puffinplot.data.Sample;

/**
 * A simple configurable PlotParams implementation intended for testing.
 * 
 * @author pont
 */
public class SettablePlotParams implements PlotParams {

    private Sample sample;
    private List<Sample> selectedSamples;
    private Correction correction;
    private MeasurementAxis vprojXaxis;
    private MeasurementAxis hprojXaxis;
    private MeasurementAxis hprojYaxis;
    private List<Sample> allSamplesInSelectedSites;
    private float unitSize = 0.12f;
    private final Map<String, String> settingsMap = new HashMap<>();
    private final Map<String, Boolean> settingsMapBoolean = new HashMap<>();
    
    @Override
    public Sample getSample() {
        return sample;
    }

    public void setSample(Sample sample) {
        this.sample = sample;
    }

    @Override
    public List<Sample> getSelectedSamples() {
        return selectedSamples;
    }

    public void setSelectedSamples(List<Sample> selectedSamples) {
        this.selectedSamples = selectedSamples;
    }

    @Override
    public Correction getCorrection() {
        return correction;
    }

    public void setCorrection(Correction correction) {
        this.correction = correction;
    }

    @Override
    public MeasurementAxis getVprojXaxis() {
        return vprojXaxis;
    }

    public void setVprojXaxis(MeasurementAxis vprojXaxis) {
        this.vprojXaxis = vprojXaxis;
    }

    @Override
    public MeasurementAxis getHprojXaxis() {
        return hprojXaxis;
    }

    public void setHprojXaxis(MeasurementAxis hprojXaxis) {
        this.hprojXaxis = hprojXaxis;
    }

    @Override
    public MeasurementAxis getHprojYaxis() {
        return hprojYaxis;
    }

    public void setHprojYaxis(MeasurementAxis hprojYaxis) {
        this.hprojYaxis = hprojYaxis;
    }

    @Override
    public List<Sample> getAllSamplesInSelectedSites() {
        return allSamplesInSelectedSites;
    }

    public void setAllSamplesInSelectedSites(
            List<Sample> allSamplesInSelectedSites) {
        this.allSamplesInSelectedSites = allSamplesInSelectedSites;
    }

    @Override
    public float getUnitSize() {
        return unitSize;
    }

    public void setUnitSize(float unitSize) {
        this.unitSize = unitSize;
    }

    @Override
    public String getSetting(String key, String def) {
        return getSettingsMap().getOrDefault(key, def);
    }

    @Override
    public boolean getSettingBoolean(String key, boolean def) {
        return getSettingsMapBoolean().getOrDefault(key, def);
    }
    
    public Map<String, String> getSettingsMap() {
        return settingsMap;
    }

    public Map<String, Boolean> getSettingsMapBoolean() {
        return settingsMapBoolean;
    }

}
