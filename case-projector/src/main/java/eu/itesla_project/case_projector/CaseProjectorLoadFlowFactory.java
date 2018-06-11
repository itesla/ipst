/**
 * Copyright (c) 2018, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package eu.itesla_project.case_projector;

import com.powsybl.computation.ComputationManager;
import com.powsybl.iidm.network.Network;
import com.powsybl.loadflow.LoadFlow;
import com.powsybl.loadflow.LoadFlowFactory;

/**
* @author Giovanni Ferrari <giovanni.ferrari at techrain.eu>
*/
public class CaseProjectorLoadFlowFactory implements LoadFlowFactory {

    @Override
    public LoadFlow create(Network network, ComputationManager computationManager, int priority) {

        return new CaseProjectorLoadFlow(network, computationManager, priority);
    }

}
