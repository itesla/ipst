/**
 * Copyright (c) 2017, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package eu.itesla_project.eurostag.network;

import java.util.Objects;

/**
 * @author Christian Biasuzzi <christian.biasuzzi@techrain.it>
 */
public class EsgACDCVscConverter {

    public enum ConverterState {
        ON,
        OFF
    }

    public enum DCControlMode {
        AC_ACTIVE_POWER,
        DC_VOLTAGE
    }

    public enum ACControlMode {
        AC_VOLTAGE,
        AC_REACTIVE_POWER,
        AC_POWER_FACTOR
    }

    private final Esg8charName znconv; // converter name
    private final Esg8charName dcNode1; // sending DC node name
    private final Esg8charName dcNode2; // receiving DC node name
    private final Esg8charName acNode; // AC node name
    private final ConverterState xstate; // converter state ' ' ON; 'S' OFF
    private final DCControlMode xregl; // DC control mode 'P' AC_ACTIVE_POWER; 'V' DC_VOLTAGE
    private final ACControlMode xoper; // AC control mode 'V' AC_VOLTAGE; 'Q' AC_REACTIVE_POWER; 'A' AC_POWER_FACTOR
    private final float rrdc; // resistance [Ohms]
    private final float rxdc; // reactance [Ohms]
    private final float pac; // AC active power setpoint [MW]. Only if DC control mode is 'P'
    private final float pvd; // DC voltage setpoint [MW]. Only if DC control mode is 'V'
    private final float pva; // AC voltage setpoint [kV]. Only if AC control mode is 'V'
    private final float pre; // AC reactive power setpoint [Mvar]. Only if AC control mode is 'Q'
    private final float pco; // AC power factor setpoint. Only if AC control mode is 'A'
    private final float qvscsh; // Reactive sharing cofficient [%]. Only if AC control mode is 'V'
    private final float pvscmin; // Minimum AC active power [MW]
    private final float pvscmax; // Maximum AC active power [MW]
    private final float qvscmin; // Minimum reactive power injected on AC node [kV]
    private final float qvscmax; // Maximum reactive power injected on AC node [kV]
    private final float vsb0; // Losses coefficient Beta0 [MW]
    private final float vsb1; // Losses coefficient Beta1 [kW]
    private final float vsb2; // Losses coefficient Beta2 [Ohms]
    private final float mvm; // Initial AC modulated voltage magnitude [p.u.]
    private final float mva; // Initial AC modulated voltage angle [deg]

    public EsgACDCVscConverter(Esg8charName znconv,
                               Esg8charName dcNode1,
                               Esg8charName dcNode2,
                               Esg8charName acNode,
                               ConverterState xstate,
                               DCControlMode xregl,
                               ACControlMode xoper,
                               float rrdc,
                               float rxdc,
                               float pac,
                               float pvd,
                               float pva,
                               float pre,
                               float pco,
                               float qvscsh,
                               float pvscmin,
                               float pvscmax,
                               float qvscmin,
                               float qvscmax,
                               float vsb0,
                               float vsb1,
                               float vsb2,
                               float mvm,
                               float mva) {
        this.znconv = Objects.requireNonNull(znconv);
        this.dcNode1 = Objects.requireNonNull(dcNode1);
        this.dcNode2 = Objects.requireNonNull(dcNode2);
        this.acNode = Objects.requireNonNull(acNode);
        this.xstate = Objects.requireNonNull(xstate);
        this.xregl = Objects.requireNonNull(xregl);
        this.xoper = Objects.requireNonNull(xoper);
        this.rrdc = rrdc;
        this.rxdc = rxdc;
        this.pac = pac;
        this.pvd = pvd;
        this.pva = pva;
        this.pre = pre;
        this.pco = pco;
        this.qvscsh = qvscsh;
        this.pvscmin = pvscmin;
        this.pvscmax = pvscmax;
        this.qvscmin = qvscmin;
        this.qvscmax = qvscmax;
        this.vsb0 = vsb0;
        this.vsb1 = vsb1;
        this.vsb2 = vsb2;
        this.mvm = mvm;
        this.mva = mva;
    }


    public Esg8charName getZnconv() {
        return znconv;
    }

    public Esg8charName getDcNode1() {
        return dcNode1;
    }

    public Esg8charName getDcNode2() {
        return dcNode2;
    }

    public Esg8charName getAcNode() {
        return acNode;
    }

    public ConverterState getXstate() {
        return xstate;
    }

    public DCControlMode getXregl() {
        return xregl;
    }

    public ACControlMode getXoper() {
        return xoper;
    }

    public float getRrdc() {
        return rrdc;
    }

    public float getRxdc() {
        return rxdc;
    }

    public float getPac() {
        return pac;
    }

    public float getPvd() {
        return pvd;
    }

    public float getPva() {
        return pva;
    }

    public float getPre() {
        return pre;
    }

    public float getPco() {
        return pco;
    }

    public float getQvscsh() {
        return qvscsh;
    }

    public float getPvscmin() {
        return pvscmin;
    }

    public float getPvscmax() {
        return pvscmax;
    }

    public float getQvscmin() {
        return qvscmin;
    }

    public float getQvscmax() {
        return qvscmax;
    }

    public float getVsb0() {
        return vsb0;
    }

    public float getVsb1() {
        return vsb1;
    }

    public float getVsb2() {
        return vsb2;
    }

    public float getMvm() {
        return mvm;
    }

    public float getMva() {
        return mva;
    }
}
