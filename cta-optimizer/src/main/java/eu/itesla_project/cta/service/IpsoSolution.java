/** Copyright (c) 2016, Tractebel (http://www.tractebel-engie.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this file,
 * You can obtain one at http://mozilla.org/MPL/2.0/.
 **/
package eu.itesla_project.cta.service;

import java.util.List;

import static com.google.common.base.Preconditions.checkArgument;
/**
 * @author Yannick Pihan <yannick.pihan at tractebel.engie.com>
 */
public class IpsoSolution {

    private final List<IpsoSolutionElement> solutionElements;
    private final float objectiveFunctionValue;

    public IpsoSolution(List<IpsoSolutionElement> solutionElements, float objectiveFunctionValue) {
        checkArgument(solutionElements != null, "solutionElements must not be null");
        this.solutionElements = solutionElements;
        this.objectiveFunctionValue = objectiveFunctionValue;
    }

    public List<IpsoSolutionElement> getSolutionElements() {
        return solutionElements;
    }

    public float getObjectiveFunctionValue() {
        return objectiveFunctionValue;
    }
}
