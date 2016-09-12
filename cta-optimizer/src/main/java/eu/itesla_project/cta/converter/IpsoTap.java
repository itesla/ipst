/** Copyright (c) 2016, Tractebel (http://www.tractebel-engie.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this file,
 * You can obtain one at http://mozilla.org/MPL/2.0/.
 **/
package eu.itesla_project.cta.converter;

import eu.itesla_project.iidm.network.TapChanger;

import static com.google.common.base.Preconditions.checkArgument;
/**
 * @author Yannick Pihan <yannick.pihan at tractebel.engie.com>
 */
public class IpsoTap {
    private final int nominal;
    private final int initial;
    private final int lowstep;
    private final int highstep;

    /**
     * Constructor
     * @param tapChanger
     */
    public IpsoTap(TapChanger tapChanger) {
        checkArgument(tapChanger != null, "tapChanger must not ne null" );
        this.initial   = tapChanger.getTapPosition();
        this.lowstep   = tapChanger.getLowTapPosition();
        this.highstep  = tapChanger.getHighTapPosition();
        // Nominal tap number is not available in IIDM.
        // We take th median plot by default
        this.nominal = tapChanger.getStepCount() / 2 + 1;
    }

    /**
     * Constructor
     */
    public IpsoTap() {
        this.initial = 1;
        this.lowstep = 1;
        this.highstep = 1;
        this.nominal = 1;
    }

    public int getNominal() {
        return nominal;
    }

    public int getInitial() {
        return initial;
    }

    public int getLowstep() {
        return lowstep;
    }

    public int getHighstep() {
        return highstep;
    }
}
