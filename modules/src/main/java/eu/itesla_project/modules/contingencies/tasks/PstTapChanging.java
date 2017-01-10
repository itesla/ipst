/**
 * Copyright (c) 2017, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package eu.itesla_project.modules.contingencies.tasks;

import eu.itesla_project.commons.ITeslaException;
import eu.itesla_project.contingency.tasks.ModificationTask;
import eu.itesla_project.iidm.network.Network;
import eu.itesla_project.iidm.network.PhaseTapChanger;
import eu.itesla_project.iidm.network.TwoWindingsTransformer;

import java.util.Objects;

/**
 * @author Mathieu Bague <mathieu.bague at rte-france.com>
 */
public class PstTapChanging implements ModificationTask {

    private final String transformerId;

    private final int tapPosition;

    public PstTapChanging(String transformerId, int tapPosition) {
        this.transformerId = Objects.requireNonNull(transformerId);
        this.tapPosition = tapPosition;
    }

    @Override
    public void modify(Network network) {
        TwoWindingsTransformer transformer = network.getTwoWindingsTransformer(transformerId);
        if (transformer == null) {
            throw new ITeslaException("Two windings transformer '" + transformerId + "' not found");
        }
        PhaseTapChanger tapChanger = transformer.getPhaseTapChanger();
        if (tapChanger == null) {
            throw new ITeslaException("Transformer " + transformerId + " is not a PST");
        }
        tapChanger.setRegulationMode(PhaseTapChanger.RegulationMode.FIXED_TAP);
        tapChanger.setTapPosition(tapPosition);
    }
}
