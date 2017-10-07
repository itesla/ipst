/**
 * Copyright (c) 2016, All partners of the iTesla project (http://www.itesla-project.eu/consortium)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package eu.itesla_project.dymola;

import com.google.common.collect.ImmutableMap;
import com.powsybl.simulation.SimulationState;
import com.powsybl.simulation.StabilizationResult;
import com.powsybl.simulation.StabilizationStatus;

import java.util.Map;
import java.util.Objects;

/**
 *
 * @author Quinary <itesla@quinary.com>
 */
class DymolaStabilizationResult implements StabilizationResult {

    private final DymolaState state;

    DymolaStabilizationResult(DymolaState state) {
        this.state = Objects.requireNonNull(state);
    }

    @Override
    public StabilizationStatus getStatus() {
        return StabilizationStatus.COMPLETED;
    }

    @Override
    public Map<String, String> getMetrics() {
        return ImmutableMap.of();
    }

    @Override
    public SimulationState getState() {
        return state;
    }

}
