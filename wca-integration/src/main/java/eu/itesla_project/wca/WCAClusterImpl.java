/**
 * Copyright (c) 2016, All partners of the iTesla project (http://www.itesla-project.eu/consortium)
 * Copyright (c) 2017, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package eu.itesla_project.wca;

import java.util.EnumSet;
import java.util.List;
import java.util.Objects;

import eu.itesla_project.contingency.Contingency;
import eu.itesla_project.modules.wca.WCACluster;
import eu.itesla_project.modules.wca.WCAClusterNum;

/**
 *
 * @author Geoffroy Jamgotchian <geoffroy.jamgotchian at rte-france.com>
 */
class WCAClusterImpl implements WCACluster {

    private final Contingency contingency;

    private final WCAClusterNum num;

    private final EnumSet<WCAClusterOrigin> origins;

    private final List<String> causes;

    WCAClusterImpl(Contingency contingency, WCAClusterNum num, EnumSet<WCAClusterOrigin> origins, List<String> causes) {
        this.contingency = Objects.requireNonNull(contingency);
        this.num =  Objects.requireNonNull(num);
        this.origins = Objects.requireNonNull(origins);
        this.causes = causes;
    }

    @Override
    public Contingency getContingency() {
        return contingency;
    }

    @Override
    public WCAClusterNum getNum() {
        return num;
    }

    @Override
    public String getOrigin() {
        return origins.toString();
    }

    @Override
    public List<String> getCauses() {
        return causes;
    }
}
