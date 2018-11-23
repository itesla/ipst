/**
 * Copyright (c) 2018, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package eu.itesla_project.eurostag;

import com.powsybl.contingency.ContingenciesProvider;
import com.powsybl.contingency.Contingency;
import com.powsybl.iidm.network.Network;

import java.util.List;

/**
 * @author Christian Biasuzzi <christian.biasuzzi@techrain.it>
 */
public class FilterValidContingenciesProvider implements ContingenciesProvider {

    final ContingenciesProvider wrappedProvider;

    public FilterValidContingenciesProvider(ContingenciesProvider wrappedProvider) {
        this.wrappedProvider = wrappedProvider;
    }

    @Override
    public List<Contingency> getContingencies(Network network) {
        return Contingency.checkValidity(wrappedProvider.getContingencies(network), network);
    }
}
