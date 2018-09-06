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
public class EsgDissymmetricalBranch {
    private final EsgBranchName name;
    private final EsgBranchConnectionStatus status;
    private final double rb; // total resistance sending node towards receiving node [p.u.]
    private final double rxb; // total reactance sending node towards receiving node [p.u.]
    private final double gs; // sending side shunt conductance [p.u.]
    private final double bs; // sending side shunt susceptance [p.u.]
    private final double rate; // branch rated power [MVA]
    private final double rb2; // total resistance receiving node towards sending node [p.u.]
    private final double rxb2; // total reactance receiving node towards sending node [p.u.]
    private final double gs2; // receiving side shunt conductance [p.u.]
    private final double bs2; // receiving side shunt susceptance [p.u.]

    public EsgDissymmetricalBranch(EsgBranchName name, EsgBranchConnectionStatus status, double rb, double rxb, double gs, double bs, double rate, double rb2, double rxb2, double gs2, double bs2) {
        this.name = Objects.requireNonNull(name);
        this.status = Objects.requireNonNull(status);
        this.rb = rb;
        this.rxb = rxb;
        this.gs = gs;
        this.bs = bs;
        this.rate = rate;
        this.rb2 = rb2;
        this.rxb2 = rxb2;
        this.gs2 = gs2;
        this.bs2 = bs2;
    }

    public EsgBranchName getName() {
        return name;
    }

    public EsgBranchConnectionStatus getStatus() {
        return status;
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

    public double getRb2() {
        return rb2;
    }

    public double getRxb2() {
        return rxb2;
    }

    public double getGs2() {
        return gs2;
    }

    public double getBs2() {
        return bs2;
    }

    public double getRate() {
        return rate;
    }

}
