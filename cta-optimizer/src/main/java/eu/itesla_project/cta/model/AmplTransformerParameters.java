/** Copyright (c) 2016, Tractebel (http://www.tractebel-engie.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this file,
 * You can obtain one at http://mozilla.org/MPL/2.0/.
 **/
package eu.itesla_project.cta.model;

import com.google.common.collect.Lists;

import java.util.List;
/**
 * @author Yannick Pihan <yannick.pihan at tractebel.engie.com>
 */
public class AmplTransformerParameters implements AmplWritable {

    final String id;
    final float rMin;
    final float initialR;
    final float rMax;
    final float tapInit;
    private final float tapMin;
    private final float tapMax;
    private final float phaseMin;
    private final float phaseByTap;


    public AmplTransformerParameters(String id, float rMin, float initialR, float rMax, float tapInit, float tapMin, float tapMax, float phaseMin, float phaseByTap) {
        this.id = id;
        this.rMin = rMin;
        this.initialR = initialR;
        this.rMax = rMax;
        this.tapInit = tapInit;
        this.tapMin = tapMin;
        this.tapMax = tapMax;
        this.phaseMin = phaseMin;
        this.phaseByTap = phaseByTap;

    }

    @Override
    public List<Object> getOrderedValues() {
        return Lists.newArrayList(
                this.id,
                this.rMin,
                this.initialR,
                this.rMax,
                this.tapInit,
                this.tapMin,
                this.tapMax,
                this.phaseMin,
                this.phaseByTap
        );
    }
}
