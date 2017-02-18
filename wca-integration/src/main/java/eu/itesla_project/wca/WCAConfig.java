/**
 * Copyright (c) 2016, All partners of the iTesla project (http://www.itesla-project.eu/consortium)
 * Copyright (c) 2017, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package eu.itesla_project.wca;

import eu.itesla_project.commons.config.ModuleConfig;
import eu.itesla_project.commons.config.PlatformConfig;

import java.nio.file.Path;
import java.util.EnumSet;
import java.util.Objects;
import java.util.Set;

/**
 * @author Geoffroy Jamgotchian <geoffroy.jamgotchian at rte-france.com>
 */
public class WCAConfig {

    private static final float DEFAULT_REDUCED_VARIABLE_RATIO = 1f;

    private final Path xpressHome;

    private final float reducedVariableRatio;

    private final boolean debug;

    private final boolean exportStates;

    private final Set<WCARestrictingThresholdLevel> restrictingThresholdLevels;

    public static WCAConfig load() {
        return load(PlatformConfig.defaultConfig());
    }

    public static WCAConfig load(PlatformConfig platformConfig) {
        ModuleConfig config = platformConfig.getModuleConfig("wca");
        Path xpressHome = config.getPathProperty("xpressHome");
        float reducedVariableRatio = config.getFloatProperty("reducedVariableRatio", DEFAULT_REDUCED_VARIABLE_RATIO);
        boolean debug = config.getBooleanProperty("debug", false);
        boolean exportStates = config.getBooleanProperty("exportStates", false);
        Set<WCARestrictingThresholdLevel> restrictingThresholdLevels = config.getEnumSetProperty("restrictingThresholdLevels", WCARestrictingThresholdLevel.class, EnumSet.noneOf(WCARestrictingThresholdLevel.class));
        return new WCAConfig(xpressHome, reducedVariableRatio, debug, exportStates, restrictingThresholdLevels);
    }

    public WCAConfig(Path xpressHome, float reducedVariableRatio, boolean debug, boolean exportStates, Set<WCARestrictingThresholdLevel> restrictingThresholdLevels) {
        this.xpressHome = Objects.requireNonNull(xpressHome);
        this.reducedVariableRatio = reducedVariableRatio;
        this.debug = debug;
        this.exportStates = exportStates;
        this.restrictingThresholdLevels = Objects.requireNonNull(restrictingThresholdLevels, "invalid restrictingThresholdLevels");
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

    @Override
    public String toString() {
        return getClass().getSimpleName() + " [xpressHome=" + xpressHome +
                ", reducedVariableRatio=" + reducedVariableRatio +
                ", debug=" + debug +
                ", exportStates=" + exportStates +
                ", restrictingThresholdLevels=" + restrictingThresholdLevels + " -> level=" + WCARestrictingThresholdLevel.getLevel(restrictingThresholdLevels) +
                "]";
    }
}
