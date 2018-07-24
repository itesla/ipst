/**
 * Copyright (c) 2017, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package eu.itesla_project.wca;

import static org.junit.Assert.assertEquals;

import java.nio.file.FileSystem;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.Set;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.google.common.jimfs.Configuration;
import com.google.common.jimfs.Jimfs;

import com.powsybl.commons.config.InMemoryPlatformConfig;
import com.powsybl.commons.config.MapModuleConfig;
import com.powsybl.iidm.network.Country;

/**
 *
 * @author Massimo Ferraro <massimo.ferraro@techrain.it>
 */
public class WCAConfigTest {

    private FileSystem fileSystem;
    private InMemoryPlatformConfig platformConfig;
    private MapModuleConfig moduleConfig;
    private Path xpressHome;

    @Before
    public void setUp() throws Exception {
        fileSystem = Jimfs.newFileSystem(Configuration.unix());
        platformConfig = new InMemoryPlatformConfig(fileSystem);
        moduleConfig = platformConfig.createModuleConfig("wca");
        xpressHome = fileSystem.getPath("/xpress-home");
        moduleConfig.setStringProperty("xpressHome", xpressHome.toString());
        
    }

    @After
    public void tearDown() throws Exception {
        fileSystem.close();
    }

    @Test
    public void testBasicConfig() throws Exception {
        WCAConfig config = WCAConfig.load(platformConfig);
        
        checkValues(config, xpressHome, WCAConfig.DEFAULT_REDUCED_VARIABLE_RATIO, WCAConfig.DEFAULT_DEBUG, WCAConfig.DEFAULT_EXPORT_STATE, 
                    WCAConfig.DEFAULT_RESTRICTING_THRESHOLD_LEVELS, WCAConfig.DEFAULT_MARGIN, WCAConfig.DEFAULT_IGNORE_VOLTAGE_CONSTRAINTS, 
                    WCAConfig.DEFAULT_ACTIVATE_FILTERING, WCAConfig.DEFAULT_PREVENTIVE_ACTIONS_FILTER, WCAConfig.DEFAULT_PREVENTIVE_ACTIONS_OPTIMIZER, 
                    WCAConfig.DEFAULT_APPLY_PREVENTIVE_ACTIONS, WCAConfig.DEFAULT_CURATIVE_ACTIONS_OPTIMIZER, WCAConfig.DEFAULT_VOLTAGE_LEVEL_CONSTRAINTS_FILTER, 
                    WCAConfig.DEFAULT_COUNTRY_CONSTRAINTS_FILTER, WCAConfig.DEFAULT_FILTER_PREVENTIVE_ACTIONS, WCAConfig.DEFAULT_FILTER_CURATIVE_ACTIONS, 
                    WCAConfig.DEFAULT_LOOOSEN_CONSTRAINTS, WCAConfig.DEFAULT_RECONDITION_CORRELATION_MATRIX);
    }

    @Test
    public void testCompleteConfig() throws Exception {
        float reducedVariableRatio = 0;
        boolean debug = true;
        boolean exportStates = true;
        float margin = 1;
        Set<WCARestrictingThresholdLevel> restrictingThresholdLevels = EnumSet.of(WCARestrictingThresholdLevel.NO_FOREIGN_THRESHOLDS);
        boolean ignoreVoltageConstraints = true;
        boolean activateFiltering = true;
        WCAPreventiveActionsFilter preventiveActionsFilter = WCAPreventiveActionsFilter.LF;
        WCAPreventiveActionsOptimizer preventiveActionsOptimizer = WCAPreventiveActionsOptimizer.LF_HEURISTIC;
        boolean applyPreventiveActions = true;
        WCACurativeActionsOptimizer curativeActionsOptimizer = WCACurativeActionsOptimizer.LF_HEURISTIC;
        float voltageLevelConstraintFilter = 400;
        Set<Country> countryConstraintFilter = EnumSet.of(Country.FR);
        boolean filterPreventiveActions = false;
        boolean filterCurativeActions = false;
        boolean loosenConstraints = true;
        boolean reconditionCorrelationMatrix = false;
        
        
        moduleConfig.setStringProperty("reducedVariableRatio", Float.toString(reducedVariableRatio));
        moduleConfig.setStringProperty("debug", Boolean.toString(debug));
        moduleConfig.setStringProperty("exportStates", Boolean.toString(exportStates));
        moduleConfig.setStringProperty("restrictingThresholdLevels", WCARestrictingThresholdLevel.NO_FOREIGN_THRESHOLDS.name());
        moduleConfig.setStringProperty("margin", Float.toString(margin));
        moduleConfig.setStringProperty("ignoreVoltageConstraints", Boolean.toString(ignoreVoltageConstraints));
        moduleConfig.setStringProperty("activateFiltering", Boolean.toString(activateFiltering));
        moduleConfig.setStringProperty("preventiveActionsFilter", preventiveActionsFilter.name());
        moduleConfig.setStringProperty("preventiveActionsOptimizer", preventiveActionsOptimizer.name());
        moduleConfig.setStringProperty("applyPreventiveActions", Boolean.toString(applyPreventiveActions));
        moduleConfig.setStringProperty("curativeActionsOptimizer", curativeActionsOptimizer.name());
        moduleConfig.setStringProperty("voltageLevelConstraintFilter", Float.toString(voltageLevelConstraintFilter));
        moduleConfig.setStringProperty("countryConstraintFilter", Country.FR.name());
        moduleConfig.setStringProperty("filterPreventiveActions", Boolean.toString(filterPreventiveActions));
        moduleConfig.setStringProperty("filterCurativeActions", Boolean.toString(filterCurativeActions));
        moduleConfig.setStringProperty("loosenConstraints", Boolean.toString(loosenConstraints));
        moduleConfig.setStringProperty("reconditionCorrelationMatrix", Boolean.toString(reconditionCorrelationMatrix));

        WCAConfig parameters = WCAConfig.load(platformConfig);

        checkValues(parameters, xpressHome, reducedVariableRatio, debug, exportStates, restrictingThresholdLevels, margin, 
                    ignoreVoltageConstraints, activateFiltering, preventiveActionsFilter, preventiveActionsOptimizer, 
                    applyPreventiveActions, curativeActionsOptimizer, voltageLevelConstraintFilter, countryConstraintFilter, 
                    filterPreventiveActions, filterCurativeActions, loosenConstraints, reconditionCorrelationMatrix);
    }

    private void checkValues(WCAConfig config, Path xpressHome, float reducedVariableRatio, boolean debug, boolean exportStates,
                             Set<WCARestrictingThresholdLevel> restrictingThresholdLevels, float margin, boolean ignoreVoltageConstraints, 
                             boolean activateFiltering, WCAPreventiveActionsFilter preventiveActionsFilter, 
                             WCAPreventiveActionsOptimizer preventiveActionsOptimizer, boolean applyPreventiveActions, 
                             WCACurativeActionsOptimizer curativeActionsOptimizer, float voltageLevelConstraintFilter, 
                             Set<Country> countryConstraintFilter, boolean filterPreventiveActions, boolean filterCurativeActions,
                             boolean loosenConstraints, boolean reconditionCorrelationMatrix) {
        assertEquals(xpressHome, config.getXpressHome());
        assertEquals(reducedVariableRatio, config.getReducedVariableRatio(), 0);
        assertEquals(debug, config.isDebug());
        assertEquals(exportStates, config.isExportStates());
        assertEquals(restrictingThresholdLevels, config.getRestrictingThresholdLevels());
        assertEquals(margin, config.getMargin(), 0);
        assertEquals(ignoreVoltageConstraints, config.ignoreVoltageConstraints());
        assertEquals(activateFiltering, config.activateFiltering());
        assertEquals(preventiveActionsFilter, config.getPreventiveActionsFilter());
        assertEquals(preventiveActionsOptimizer, config.getPreventiveActionsOptimizer());
        assertEquals(applyPreventiveActions, config.applyPreventiveActions());
        assertEquals(curativeActionsOptimizer, config.getCurativeActionsOptimizer());
        assertEquals(voltageLevelConstraintFilter, config.getVoltageLevelConstraintFilter(), 0);
        assertEquals(countryConstraintFilter, config.getCountryConstraintFilter());
        assertEquals(filterPreventiveActions, config.filterPreventiveActions());
        assertEquals(filterCurativeActions, config.filterCurativeActions());
        assertEquals(loosenConstraints, config.loosenConstraints());
        assertEquals(reconditionCorrelationMatrix, config.isReconditionCorrelationMatrix());
    }

}