/** Copyright (c) 2016, Tractebel (http://www.tractebel-engie.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this file,
 * You can obtain one at http://mozilla.org/MPL/2.0/.
 **/
package eu.itesla_project.cta.model;

import java.util.List;
/**
 * @author Yannick Pihan <yannick.pihan at tractebel.engie.com>
 */
public class AmplModel {

    private final List<AmplGenerator> generators;
    private final List<AmplSimpleNode> nodes;
    private final List<AmplTransformerBranch> transBranches;
    private final List<AmplSimpleBranch> simpleBranches;
    private final List<AmplCouplage> couplages;
    private final List<AmplSlackBus> slackBuses;
    private final List<AmplNodeParameter> nodeParameters;
    private final List<AmplSlackBusParameters> slackBusParameters;
    private final List<AmplGeneratorParameters> generatorParameters;
    private final List<AmplBranchParameters> branchParameters;
    private final List<AmplTransformerParameters> transformerParameters;
    private final List<AmplCouplingParameters> couplingParameters;
    private final List<AmplLoad> loads;
    private final List<AmplLoadParameters> loadParameters;
    private final List<AmplCapacitor> capacitors;
    private final List<AmplCapacitorParameters> capacitorParameters;

    /**
     * Constructor
     */
    public AmplModel(List<AmplGenerator> generators,
                     List<AmplLoad> loads,
                     List<AmplCapacitor> capacitors, List<AmplSimpleNode> nodes,
                     List<AmplSlackBus> slackBuses,
                     List<AmplTransformerBranch> transBranches,
                     List<AmplSimpleBranch> simpleBranches,
                     List<AmplCouplage> couplages,
                     List<AmplNodeParameter> nodeParameters,
                     List<AmplSlackBusParameters> slackBusParameters,
                     List<AmplGeneratorParameters> generatorParameters,
                     List<AmplLoadParameters> loadParameters,
                     List<AmplCapacitorParameters> capacitorParameters,
                     List<AmplBranchParameters> branchParameters,
                     List<AmplTransformerParameters> transformerParameters,
                     List<AmplCouplingParameters> amplCouplingParameters) {

        this.generators = generators;
        this.loads = loads;
        this.capacitors = capacitors;
        this.nodes = nodes;
        this.slackBuses = slackBuses;
        this.transBranches = transBranches;
        this.simpleBranches = simpleBranches;
        this.couplages = couplages;
        this.nodeParameters = nodeParameters;
        this.slackBusParameters = slackBusParameters;
        this.generatorParameters = generatorParameters;
        this.loadParameters = loadParameters;
        this.capacitorParameters = capacitorParameters;
        this.branchParameters = branchParameters;
        this.transformerParameters = transformerParameters;
        this.couplingParameters = amplCouplingParameters;
    }

    public List<AmplGenerator> getGenerators() {
        return generators;
    }

    public List<AmplLoad> getLoads() { return loads; }

    public List<AmplSimpleNode> getNodes() { return nodes; }

    public List<AmplNodeParameter> getNodeParameters() { return nodeParameters; }

    public List<AmplTransformerBranch> getTransBranches() { return transBranches; }

    public List<AmplSimpleBranch> getSimpleBranches() { return simpleBranches; }

    public List<AmplCouplage> getCouplages() { return this.couplages; }

    public List<AmplSlackBus> getSlackBuses() { return this.slackBuses; }

    public List<AmplSlackBusParameters> getSlackBusParameters() { return this.slackBusParameters; }

    public List<AmplGeneratorParameters> getGeneratorParameters() { return this.generatorParameters; }

    public List<AmplBranchParameters> getBranchParameters() { return this.branchParameters; }

    public List<AmplTransformerParameters> getTransformerParameters() { return this.transformerParameters;  }

    public List<AmplCouplingParameters> getCouplingParameters() { return this.couplingParameters; }

    public List<AmplCapacitor> getCapacitors() {
        return capacitors;
    }

    public List<AmplLoadParameters> getLoadParameters() {
        return loadParameters;
    }

    public List<AmplCapacitorParameters> getCapacitorParameters() {
        return capacitorParameters;
    }
}
