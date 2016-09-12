/** Copyright (c) 2016, Tractebel (http://www.tractebel-engie.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this file,
 * You can obtain one at http://mozilla.org/MPL/2.0/.
 **/
package eu.itesla_project.cta.service;

import com.google.common.collect.Lists;
import eu.itesla_project.cta.model.*;

import java.util.List;

import static com.google.common.base.Preconditions.checkArgument;
import static java.util.stream.Collectors.toList;
/**
 * @author Yannick Pihan <yannick.pihan at tractebel.engie.com>
 */
class IpsoControlVariableFactory {

    public static final float DEFAULT_SPEED_TAP = 2.f;
    public static final float EPSILON = 0.00001f;
    public static final float DEFAULT_SPEED_Q = 0f;
    public static final float DEFAULT_SPEED_P = 9999f;
    private final IpsoProblemComponentFactory ipsoProblemComponentFactory;

    public IpsoControlVariableFactory(IpsoProblemComponentFactory ipsoProblemComponentFactory) {
        checkArgument(ipsoProblemComponentFactory != null, "ipsoProblemComponentFactory must not be null");
        this.ipsoProblemComponentFactory = ipsoProblemComponentFactory;
    }

    /**
     *
     * @return the active power control variables
     */
    public List<IpsoControlVariableProductionP> createActiveProductionVariables(IpsoNetworkState networkState) {
        checkArgument(networkState != null, "networkState must not be null");
        return networkState.getIpsoGenerators().stream()
                .filter(IpsoGenerator::isBiggest)
                .map(generator -> ipsoProblemComponentFactory.createVariableProductionP(
                                generator,
                                LinkOption.FREE,
                                DEFAULT_SPEED_P,
                                networkState.getWorld())
                )
                .collect(toList());
    }

    /**
     *
     * @return the reactive power control variables
     */
    public List<IpsoControlVariableProductionQ> createReactiveProductionVariables(IpsoNetworkState networkState) {
        checkArgument(networkState != null, "networkState must not be null");

        List<IpsoControlVariableProductionQ> variables = networkState.getConnectedAndRegulatingGenerators()
                .filter(IpsoGenerator::isDeltaQLimitUpperThanOneMvar)
                .map(generator -> ipsoProblemComponentFactory.createVariableProductionQ(
                                generator,
                                LinkOption.VOLTAGE,
                                DEFAULT_SPEED_Q,
                                networkState.getWorld())
                )
                .collect(toList());

        return variables;
    }

    public List<IpsoControlVariable2WTransformerTap> createTwoWindingTransformerTapVariables(IpsoNetworkState networkState, IpsoOptions option) {

        if ( option.isTransformerRegulateTakenIntoAccount() ) {
            checkArgument(networkState != null, "networkState must not be null");
            return networkState.getIpsoTwoWindingsTransformers().stream()
                    .filter(IpsoTwoWindingsTransformer::isRegulating)
                    .filter(IpsoTwoWindingsTransformer::isConnectedOnBothSides)
                    .filter(IpsoTwoWindingsTransformer::hasMoreThanOneStep)
                    .map(transformer -> ipsoProblemComponentFactory.createVariable2WTransformerTap(
                            transformer,
                            LinkOption.TAP,
                            DEFAULT_SPEED_TAP,
                            EPSILON,
                            networkState.getWorld()))
                    .collect(toList());
        }
        else {
            return Lists.newArrayList();
        }
    }

    public List<IpsoControlVariableGeneratorStatism> createGeneratorStatismVariables(IpsoNetworkState networkState) {
        return Lists.newArrayList();
    }

    public List<IpsoControlVariableBankStep> createBankStepVariables(IpsoNetworkState networkState) {
        return Lists.newArrayList();
    }
}
