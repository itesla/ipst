/**
 * Copyright (c) 2016, All partners of the iTesla project (http://www.itesla-project.eu/consortium)
 * Copyright (c) 2016, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package eu.itesla_project.online.modules.mock;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

import eu.itesla_project.contingency.Contingency;
import eu.itesla_project.iidm.network.Network;
import eu.itesla_project.modules.contingencies.ContingenciesAndActionsDatabaseClient;
import eu.itesla_project.modules.wca.WCA;
import eu.itesla_project.modules.wca.WCAAsyncResult;
import eu.itesla_project.modules.wca.WCACluster;
import eu.itesla_project.modules.wca.WCAClusterNum;
import eu.itesla_project.modules.wca.WCAParameters;
import eu.itesla_project.modules.wca.WCAResult;
import eu.itesla_project.modules.wca.report.WCAReport;

/**
 *
 * @author Quinary <itesla@quinary.com>
 */
public class WCAMock implements WCA {

    private final WCAClusterNum clusterNum = WCAClusterNum.FOUR; // cluster 4 -> need further analysis

    private final Network network;

    private final ContingenciesAndActionsDatabaseClient contingenciesActionsDbClient;

    public WCAMock(Network network, ContingenciesAndActionsDatabaseClient contingenciesActionsDbClient) {
        Objects.requireNonNull(contingenciesActionsDbClient, "contingenciesActionsDbClient is null");
        Objects.requireNonNull(network, "network is null");
        this.contingenciesActionsDbClient = contingenciesActionsDbClient;
        this.network = network;
    }

    @Override
    public WCAResult run(WCAParameters parameters) throws Exception {
        // classify all the contingencies in the same cluster
        List<WCACluster> clusters = new ArrayList<>();
        for (Contingency contingency: contingenciesActionsDbClient.getContingencies(network)) {
            clusters.add(new WCAClusterImpl(contingency, clusterNum));
        }
        return new WCAResultImpl(clusters);
    }

    @Override
    public CompletableFuture<WCAAsyncResult> runAsync(String baseStateId, WCAParameters parameters) throws Exception {
        throw new UnsupportedOperationException();
    }

    @Override
    public WCAReport getReport() {
        return null;
    }
}
