/**
 * Copyright (c) 2016, All partners of the iTesla project (http://www.itesla-project.eu/consortium)
 * Copyright (c) 2017, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package eu.itesla_project.wca;

import java.nio.file.Path;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import eu.itesla_project.commons.config.ModuleConfig;
import eu.itesla_project.commons.config.PlatformConfig;
import eu.itesla_project.iidm.network.Country;

/**
 * @author Geoffroy Jamgotchian <geoffroy.jamgotchian at rte-france.com>
 */
public class WCAConfig {

    public static final float DEFAULT_REDUCED_VARIABLE_RATIO = 1f;
    public static final boolean DEFAULT_DEBUG = false;
    public static final boolean DEFAULT_EXPORT_STATE = false;
    public static final Set<WCARestrictingThresholdLevel> DEFAULT_RESTRICTING_THRESHOLD_LEVELS = EnumSet.noneOf(WCARestrictingThresholdLevel.class);
    public static final float DEFAULT_MARGIN = 0f;
    public static final boolean DEFAULT_IGNORE_VOLTAGE_CONSTRAINTS = false;
    public static final boolean DEFAULT_DEACTIVATE_FILTERING = true;
    public static final WCAPreventiveActionsFilter DEFAULT_PREVENTIVE_ACTIONS_FILTER = WCAPreventiveActionsFilter.LF;
    public static final WCAPreventiveActionsOptimizer DEFAULT_PREVENTIVE_ACTIONS_OPTIMIZER = WCAPreventiveActionsOptimizer.DOMAINS;
    public static final boolean DEFAULT_APPLY_PREVENTIVE_ACTIONS = false;
    public static final WCACurativeActionsOptimizer DEFAULT_CURATIVE_ACTIONS_OPTIMIZER = WCACurativeActionsOptimizer.CLUSTERS;
    public static final float DEFAULT_VOLTAGE_LEVEL_CONSTRAINTS_FILTER = 0f;
    public static final Set<Country> DEFAULT_COUNTRY_CONSTRAINTS_FILTER = EnumSet.noneOf(Country.class);
    public static final boolean DEFAULT_FILTER_PREVENTIVE_ACTIONS = true;
    public static final boolean DEFAULT_FILTER_CURATIVE_ACTIONS = true;
    public static final boolean DEFAULT_LOOOSEN_CONSTRAINTS = false;

    private final Path xpressHome;
    private final float reducedVariableRatio;
    private final boolean debug;
    private final boolean exportStates;
    private final Set<WCARestrictingThresholdLevel> restrictingThresholdLevels;
    private final float margin;
    private final boolean ignoreVoltageConstraints;
    private final boolean deactivateFiltering;
    private final WCAPreventiveActionsFilter preventiveActionsFilter;
    private final WCAPreventiveActionsOptimizer preventiveActionsOptimizer;
    private final boolean applyPreventiveActions;
    private final WCACurativeActionsOptimizer curativeActionsOptimizer;
    private final float voltageLevelConstraintFilter;
    private final Set<Country> countryConstraintFilter;
    private final boolean filterPreventiveActions;
    private final boolean filterCurativeActions;
    private final boolean loosenConstraints;

    public static WCAConfig load() {
        return load(PlatformConfig.defaultConfig());
    }

    public static WCAConfig load(PlatformConfig platformConfig) {
        ModuleConfig config = platformConfig.getModuleConfig("wca");
        Path xpressHome = config.getPathProperty("xpressHome");
        float reducedVariableRatio = config.getFloatProperty("reducedVariableRatio", DEFAULT_REDUCED_VARIABLE_RATIO);
        boolean debug = config.getBooleanProperty("debug", DEFAULT_DEBUG);
        boolean exportStates = config.getBooleanProperty("exportStates", DEFAULT_EXPORT_STATE);
        Set<WCARestrictingThresholdLevel> restrictingThresholdLevels = config.getEnumSetProperty("restrictingThresholdLevels", WCARestrictingThresholdLevel.class, DEFAULT_RESTRICTING_THRESHOLD_LEVELS);
        float margin = Float.parseFloat(config.getStringProperty("margin", Float.toString(DEFAULT_MARGIN)));
        boolean ignoreVoltageConstraints = config.getBooleanProperty("ignoreVoltageConstraints", DEFAULT_IGNORE_VOLTAGE_CONSTRAINTS);
        boolean deactivateFiltering = config.getBooleanProperty("deactivateFiltering", DEFAULT_DEACTIVATE_FILTERING);
        WCAPreventiveActionsFilter preventiveActionsFilter = config.getEnumProperty("preventiveActionsFilter", WCAPreventiveActionsFilter.class, DEFAULT_PREVENTIVE_ACTIONS_FILTER);
        WCAPreventiveActionsOptimizer preventiveActionsOptimizer = config.getEnumProperty("preventiveActionsOptimizer", WCAPreventiveActionsOptimizer.class, DEFAULT_PREVENTIVE_ACTIONS_OPTIMIZER);
        boolean applyPreventiveActions = config.getBooleanProperty("applyPreventiveActions", DEFAULT_APPLY_PREVENTIVE_ACTIONS);
        WCACurativeActionsOptimizer curativeActionsOptimizer = config.getEnumProperty("curativeActionsOptimizer", WCACurativeActionsOptimizer.class, DEFAULT_CURATIVE_ACTIONS_OPTIMIZER);
        float voltageLevelConstraintFilter = Float.parseFloat(config.getStringProperty("voltageLevelConstraintFilter", Float.toString(DEFAULT_VOLTAGE_LEVEL_CONSTRAINTS_FILTER)));
        Set<Country> countryConstraintFilter = config.getEnumSetProperty("countryConstraintFilter", Country.class, new HashSet<Country>(DEFAULT_COUNTRY_CONSTRAINTS_FILTER));
        boolean filterPreventiveActions = config.getBooleanProperty("filterPreventiveActions", DEFAULT_FILTER_PREVENTIVE_ACTIONS);
        boolean filterCurativeActions = config.getBooleanProperty("filterCurativeActions", DEFAULT_FILTER_CURATIVE_ACTIONS);
        boolean loosenConstraints = config.getBooleanProperty("loosenConstraints", DEFAULT_LOOOSEN_CONSTRAINTS);
        return new WCAConfig(xpressHome, reducedVariableRatio, debug, exportStates, restrictingThresholdLevels, margin, ignoreVoltageConstraints, 
                             deactivateFiltering, preventiveActionsFilter, preventiveActionsOptimizer, applyPreventiveActions, curativeActionsOptimizer,
                             voltageLevelConstraintFilter, countryConstraintFilter, filterPreventiveActions, filterCurativeActions, loosenConstraints);
    }

    public WCAConfig(Path xpressHome, float reducedVariableRatio, boolean debug, boolean exportStates, Set<WCARestrictingThresholdLevel> restrictingThresholdLevels, 
                     float margin, boolean ignoreVoltageConstraints, boolean deactivateFiltering, WCAPreventiveActionsFilter preventiveActionsFilter, 
                     WCAPreventiveActionsOptimizer preventiveActionsOptimizer, boolean applyPreventiveActions, WCACurativeActionsOptimizer curativeActionsOptimizer, 
                     float voltageLevelConstraintFilter, Set<Country> countryConstraintFilter, boolean filterPreventiveActions, boolean filterCurativeActions,
                     boolean loosenConstraints) {
        this.xpressHome = Objects.requireNonNull(xpressHome);
        this.reducedVariableRatio = reducedVariableRatio;
        this.debug = debug;
        this.exportStates = exportStates;
        this.margin = margin;
        this.restrictingThresholdLevels = Objects.requireNonNull(restrictingThresholdLevels, "invalid restrictingThresholdLevels");
        this.ignoreVoltageConstraints = ignoreVoltageConstraints;
        this.deactivateFiltering = deactivateFiltering;
        this.preventiveActionsFilter = Objects.requireNonNull(preventiveActionsFilter);
        this.preventiveActionsOptimizer = Objects.requireNonNull(preventiveActionsOptimizer);
        this.applyPreventiveActions = applyPreventiveActions;
        this.curativeActionsOptimizer = Objects.requireNonNull(curativeActionsOptimizer);
        this.voltageLevelConstraintFilter = voltageLevelConstraintFilter;
        this.countryConstraintFilter = Objects.requireNonNull(countryConstraintFilter);
        this.filterPreventiveActions = filterPreventiveActions;
        this.filterCurativeActions = filterCurativeActions;
        this.loosenConstraints = loosenConstraints;
    }

    public Path getXpressHome() {
        return xpressHome;
    }

    public float getReducedVariableRatio() {
        return reducedVariableRatio;
    }

    public boolean isDebug() {
        return debug;
    }

    public boolean isExportStates() {
        return exportStates;
    }

    public Set<WCARestrictingThresholdLevel> getRestrictingThresholdLevels() {
        return restrictingThresholdLevels;
    }

    public float getMargin() {
        return margin;
    }

    public boolean ignoreVoltageConstraints() {
        return ignoreVoltageConstraints;
    }

    public boolean deactivateFiltering() {
        return deactivateFiltering;
    }

    public WCAPreventiveActionsFilter getPreventiveActionsFilter() {
        return preventiveActionsFilter;
    }

    public WCAPreventiveActionsOptimizer getPreventiveActionsOptimizer() {
        return preventiveActionsOptimizer;
    }

    public boolean applyPreventiveActions() {
        return applyPreventiveActions;
    }

    public WCACurativeActionsOptimizer getCurativeActionsOptimizer() {
        return curativeActionsOptimizer;
    }

    public float getVoltageLevelConstraintFilter() {
        return voltageLevelConstraintFilter;
    }

    public Set<Country> getCountryConstraintFilter() {
        return countryConstraintFilter;
    }

    public boolean filterPreventiveActions() {
        return filterPreventiveActions;
    }

    public boolean filterCurativeActions() {
        return filterCurativeActions;
    }

    public boolean loosenConstraints() {
        return loosenConstraints;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + " [xpressHome=" + xpressHome +
                ", reducedVariableRatio=" + reducedVariableRatio +
                ", debug=" + debug +
                ", exportStates=" + exportStates +
                ", restrictingThresholdLevels=" + restrictingThresholdLevels + " -> level=" + WCARestrictingThresholdLevel.getLevel(restrictingThresholdLevels) +
                ", margin=" + margin +
                ", ignoreVoltageConstraints=" + ignoreVoltageConstraints +
                ", deactivateFiltering=" + deactivateFiltering +
                ", preventiveActionsFilter=" + preventiveActionsFilter +
                ", preventiveActionsOptimizer=" + preventiveActionsOptimizer +
                ", applyPreventiveActions=" + applyPreventiveActions + 
                ", curativeActionsOptimizer=" + curativeActionsOptimizer +
                ", voltageLevelConstraintFilter=" + voltageLevelConstraintFilter + 
                ", countryConstraintFilter=" + countryConstraintFilter + 
                ", filterPreventiveActions=" + filterPreventiveActions +
                ", filterCurativeActions=" + filterCurativeActions +
                ", loosenConstraints=" + loosenConstraints +
                "]";
    }
}
