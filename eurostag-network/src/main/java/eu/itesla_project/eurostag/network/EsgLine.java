/**
 * Copyright (c) 2016, All partners of the iTesla project (http://www.itesla-project.eu/consortium)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package eu.itesla_project.eurostag.network;

/**
 * @author Geoffroy Jamgotchian <geoffroy.jamgotchian at rte-france.com>
 */
public class EsgLine {

    private final EsgBranchName name;
    private final EsgBranchConnectionStatus status;
    private final double rb; // total line resistance [p.u.]
    private final double rxb; // total line reactance [p.u.]
    private final double gs; // semi shunt conductance [p.u.]
    private final double bs; // semi shunt susceptance [p.u.]
    private final double rate; // line rated power [MVA].

    public EsgLine(EsgBranchName name, EsgBranchConnectionStatus status, double rb, double rxb, double gs, double bs, double rate) {
        this.name = name;
        this.status = status;
        this.rb = rb;
        this.rxb = rxb;
        this.gs = gs;
        this.bs = bs;
        this.rate = rate;
    }

    public EsgBranchName getName() {
        return name;
    }

    public EsgBranchConnectionStatus getStatus() {
        return status;
    }

    public double getRate() {
        return rate;
    }

    public double getRb() {
        return rb;
    }

    public double getRxb() {
        return rxb;
    }

    public double getGs() {
        return gs;
    }

    public double getBs() {
        return bs;
    }

}
