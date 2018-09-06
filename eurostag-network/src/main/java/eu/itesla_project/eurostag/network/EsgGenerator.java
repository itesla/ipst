/**
 * Copyright (c) 2016, All partners of the iTesla project (http://www.itesla-project.eu/consortium)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package eu.itesla_project.eurostag.network;

import java.util.Objects;

/**
 * @author Geoffroy Jamgotchian <geoffroy.jamgotchian at rte-france.com>
 */
public class EsgGenerator {

    private final Esg8charName znamge; // generator name
    private final EsgConnectionStatus xgenest; // status
                                            // ‘Y‘     : connected
                                            // ‘N’     : not connected
    private final Esg8charName znodge; // connection node name
    private final double pgmin; // minimum active power [MW]
    private final double pgen; // active power [MW]
    private final double pgmax; // maximum active power [MW]
    private final double qgmin; // minimum reactive power [Mvar]
    private final double qgen; // reactive power [Mvar]
    private final double qgmax; // maximum reactive power [Mvar]
    private EsgRegulatingMode xregge; // regulating mode
    private double vregge; // voltage target
    private final Esg8charName zregnoge; // regulated node (= ZNODGE if blank)
    private final double qgensh; // Reactive sharing coefficient [%]

    public EsgGenerator(Esg8charName znamge, Esg8charName znodge, double pgmin, double pgen, double pgmax, double qgmin, double qgen, double qgmax, EsgRegulatingMode xregge, double vregge, Esg8charName zregnoge, double qgensh, EsgConnectionStatus xgenest) {
        this.znamge = Objects.requireNonNull(znamge);
        this.znodge = Objects.requireNonNull(znodge);
        this.pgmin = pgmin;
        this.pgen = pgen;
        this.pgmax = pgmax;
        this.qgmin = qgmin;
        this.qgen = qgen;
        this.qgmax = qgmax;
        this.xregge = Objects.requireNonNull(xregge);
        this.vregge = vregge;
        this.zregnoge = zregnoge;
        this.qgensh = qgensh;
        this.xgenest = xgenest;
    }

    public double getPgen() {
        return pgen;
    }

    public double getPgmax() {
        return pgmax;
    }

    public double getPgmin() {
        return pgmin;
    }

    public double getQgen() {
        return qgen;
    }

    public double getQgensh() {
        return qgensh;
    }

    public double getQgmax() {
        return qgmax;
    }

    public double getQgmin() {
        return qgmin;
    }

    public double getVregge() {
        return vregge;
    }

    public void setVregge(double vregge) {
        this.vregge = vregge;
    }

    public EsgConnectionStatus getXgenest() {
        return xgenest;
    }

    public EsgRegulatingMode getXregge() {
        return xregge;
    }

    public void setXregge(EsgRegulatingMode xregge) {
        this.xregge = xregge;
    }

    public Esg8charName getZnamge() {
        return znamge;
    }

    public Esg8charName getZnodge() {
        return znodge;
    }

    public Esg8charName getZregnoge() {
        return zregnoge;
    }
}
