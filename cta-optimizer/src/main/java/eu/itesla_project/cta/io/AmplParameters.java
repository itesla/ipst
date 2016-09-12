/** Copyright (c) 2016, Tractebel (http://www.tractebel-engie.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this file,
 * You can obtain one at http://mozilla.org/MPL/2.0/.
 **/
package eu.itesla_project.cta.io;

import eu.itesla_project.cta.model.AmplModel;
import eu.itesla_project.cta.model.AmplWritable;

import java.util.List;
import java.util.function.Function;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.collect.Lists.newArrayList;
import static java.util.stream.Collectors.toList;
/**
 * @author Yannick Pihan <yannick.pihan at tractebel.engie.com>
 */
public enum AmplParameters {

    GENERATORS("generators", AmplModel::getGenerators),
    LOADS("loads", AmplModel::getLoads),
    CAPACITORS("capacitors", AmplModel::getCapacitors),
    NODES("nodes", AmplModel::getNodes),
    TRANSFORMERS("transformers", AmplModel::getTransBranches),
    SIMPLE_BRANCHES("simpleBranches", AmplModel::getSimpleBranches),
    COUPLAGES("couplages", AmplModel::getCouplages),
    SLACKBUSES("slackbuses", AmplModel::getSlackBuses),

    NODE_PARAMETERS("nodeParametersList", AmplModel::getNodeParameters),
    SLACKBUS_PARAMETERS("slackbusParametersList", AmplModel::getSlackBusParameters),
    GENERATOR_PARAMETERS("generatorParametersList", AmplModel::getGeneratorParameters),
    LOAD_PARAMETERS("loadParametersList", AmplModel::getLoadParameters),
    CAPACITOR_PARAMETERS("capacitorParametersList", AmplModel::getCapacitorParameters),
    BRANCH_PARAMETERS("branchParametersList", AmplModel::getBranchParameters),
    TRANSFORMER_PARAMETERS("transformerParametersList", AmplModel::getTransformerParameters),
    COUPLING_PARAMETERS("couplingParametersList", AmplModel::getCouplingParameters),
    ;

    private static List<String> EMPTY_SET = newArrayList("{}");
    private static final AmplParameterFormatter FORMATTER = new AmplParameterFormatter();

    private final String name;
    private final Function<AmplModel, List<? extends AmplWritable>> amplElements;

    AmplParameters(String name,
                   Function<AmplModel, List<? extends AmplWritable>> amplElements) {
        this.name = name;
        this.amplElements = amplElements;
    }

    public List<String> format(AmplModel model) {
        checkArgument(model != null, "model must not be null");
        List<String> strings = amplElements.apply(model)
                .stream()
                .map(element -> FORMATTER.format(element.getOrderedValues()))
                .collect(toList());
        return strings.isEmpty() ? EMPTY_SET : strings;
    }

    public String getName() {
        return name;
    }
}
