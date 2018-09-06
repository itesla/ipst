/**
 * Copyright (c) 2016, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package eu.itesla_project.eurostag.network;

import java.util.Objects;

/**
 * @author Mathieu Bague <mathieu.bague at rte-france.com>
 */
public class EsgStaticVarCompensator {

    private final Esg8charName znamsvc; // SVC name
    private final EsgConnectionStatus xsvcst; // status: 'Y' -> connected, 'N' -> not connected
    private final Esg8charName znodsvc; // connection node name
    private final double bmin; // minimum reactive power at node base voltage [Mvar]
    private final double binit; // initial reactive power at node base voltage [Mvar]
    private final double bmax; // maximum reactive power at node base voltage [Mvar]
    private final EsgRegulatingMode xregsvc; // regulating mode
    private final double vregsvc; // voltage target
    private final double qsvcsh; // reactive sharing coefficient [%]

    public EsgStaticVarCompensator(Esg8charName znamsvc, EsgConnectionStatus xsvcst, Esg8charName znodsvc, double bmin,
                                   double binit, double bmax, EsgRegulatingMode xregsvc, double vregsvc, double qsvcsh) {
        this.znamsvc = Objects.requireNonNull(znamsvc);
        this.xsvcst = Objects.requireNonNull(xsvcst);
        this.znodsvc = Objects.requireNonNull(znodsvc);
        this.bmin = bmin;
        this.binit = binit;
        this.bmax = bmax;
        this.xregsvc = Objects.requireNonNull(xregsvc);
        this.vregsvc = vregsvc;
        this.qsvcsh = qsvcsh;
    }

    public Esg8charName getZnamsvc() {
        return znamsvc;
    }

    public EsgConnectionStatus getXsvcst() {
        return xsvcst;
    }

    public Esg8charName getZnodsvc() {
        return znodsvc;
    }

    public double getBmin() {
        return bmin;
    }

    public double getBinit() {
        return binit;
    }

    public double getBmax() {
        return bmax;
    }

    public EsgRegulatingMode getXregsvc() {
        return xregsvc;
    }

    public double getVregsvc() {
        return vregsvc;
    }

    public double getQsvcsh() {
        return qsvcsh;
    }
}
