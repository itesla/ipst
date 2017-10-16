/**
 * Copyright (c) 2017, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package eu.itesla_project.uncertainties;

import com.powsybl.commons.config.ModuleConfig;
import com.powsybl.commons.config.PlatformConfig;
import com.powsybl.iidm.network.Country;

import java.util.Set;

/**
 * @author Geoffroy Jamgotchian <geoffroy.jamgotchian@rte-france.com>
 */
public class UncertaintiesAnalysisConfig {

    private static final String MODULE_NAME = "uncertainties-analysis";

    private static final boolean DEFAULT_ONLY_INTERMITTENT_GENERATION = false;
    private static final boolean DEFAULT_WITH_BOUNDARIES = false;
    private static final float DEFAULT_PRCT_RISK = 0.85f;
    private static final boolean DEFAULT_USE_MONTHLY_CACHE = false;

    private final boolean onlyIntermittentGeneration;

    private final float prctRisk;

    private final boolean withBoundaries;

    private final Set<Country> boundariesFilter;

    private final boolean debug;

    private final boolean useMonthlyCache;

    public static UncertaintiesAnalysisConfig load() {
        boolean onlyIntermittentGeneration = DEFAULT_ONLY_INTERMITTENT_GENERATION;
        float prctRisk = DEFAULT_PRCT_RISK;
        boolean withBoundaries = DEFAULT_WITH_BOUNDARIES;
        Set<Country> boundariesFilter = null;
        boolean debug = false;
        boolean useMonthlyCache = DEFAULT_USE_MONTHLY_CACHE;
        if (PlatformConfig.defaultConfig().moduleExists(MODULE_NAME)) {
            ModuleConfig config = PlatformConfig.defaultConfig().getModuleConfig(MODULE_NAME);
            onlyIntermittentGeneration = config.getBooleanProperty("onlyIntermittentGeneration", DEFAULT_ONLY_INTERMITTENT_GENERATION);
            prctRisk = config.getFloatProperty("prctRisk", DEFAULT_PRCT_RISK);
            withBoundaries = config.getBooleanProperty("withBoundaries", DEFAULT_WITH_BOUNDARIES);
            boundariesFilter = config.getEnumSetProperty("boundariesFilter", Country.class, null);
            debug = config.getBooleanProperty("debug", false);
            useMonthlyCache = config.getBooleanProperty("useMonthlyCache", DEFAULT_USE_MONTHLY_CACHE);
        }
        return new UncertaintiesAnalysisConfig(onlyIntermittentGeneration, prctRisk, withBoundaries, boundariesFilter, debug, useMonthlyCache);
    }

    public UncertaintiesAnalysisConfig(boolean onlyIntermittentGeneration, float prctRisk, boolean withBoundaries,
                                       Set<Country> boundariesFilter, boolean debug, boolean useMonthlyCache) {
        this.onlyIntermittentGeneration = onlyIntermittentGeneration;
        this.prctRisk = prctRisk;
        this.withBoundaries = withBoundaries;
        this.boundariesFilter = boundariesFilter;
        this.debug = debug;
        this.useMonthlyCache = useMonthlyCache;
    }

    public boolean isOnlyIntermittentGeneration() {
        return onlyIntermittentGeneration;
    }

    public float getPrctRisk() {
        return prctRisk;
    }

    public boolean isWithBoundaries() {
        return withBoundaries;
    }

    public Set<Country> getBoundariesFilter() {
        return boundariesFilter;
    }

    public boolean isDebug() {
        return debug;
    }

    public boolean useMonthlyCache() {
        return useMonthlyCache;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + " [onlyIntermittentGeneration=" + onlyIntermittentGeneration +
                ", withBoundaries=" + withBoundaries +
                ", boundariesFilter=" + boundariesFilter +
                ", debug=" + debug +
                ", useMonthlyCache=" + useMonthlyCache +
                "]";
    }

}
