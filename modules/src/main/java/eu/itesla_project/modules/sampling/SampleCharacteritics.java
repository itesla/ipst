/**
 * Copyright (c) 2016, All partners of the iTesla project (http://www.itesla-project.eu/consortium)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package eu.itesla_project.modules.sampling;

import com.powsybl.iidm.network.DanglingLine;
import com.powsybl.iidm.network.Generator;
import com.powsybl.iidm.network.Load;
import com.powsybl.iidm.network.Network;

/**
 *
 * @author Geoffroy Jamgotchian <geoffroy.jamgotchian at rte-france.com>
 */
public class SampleCharacteritics {

    private final double loadPositiveP;
    private final double loadPositiveQ;
    private final double loadNegativeP;
    private final double loadNegativeQ;
    private final double generationP;
    private final double generationQ;
    private final double boundariesP;
    private final double boundariesQ;

    public static SampleCharacteritics fromNetwork(Network network, boolean generationSampled, boolean boundariesSampled) {
        double loadPositiveP = 0.0;
        double loadPositiveQ = 0.0;
        double loadNegativeP = 0.0;
        double loadNegativeQ = 0.0;
        double generationP = 0.0;
        double generationQ = 0.0;
        double boundariesP = 0.0;
        double boundariesQ = 0.0;
        for (Load l : network.getLoads()) {
            if (l.getP0() > 0) {
                loadPositiveP += l.getP0();
            } else {
                loadNegativeP += l.getP0();
            }
            if (l.getQ0() > 0) {
                loadPositiveQ += l.getQ0();
            } else {
                loadNegativeQ += l.getQ0();
            }
        }
        if (generationSampled) {
            for (Generator g : network.getGenerators()) {
                if (g.getEnergySource().isIntermittent()) {
                    generationP += g.getTargetP();
                    if (!g.isVoltageRegulatorOn()) {
                        generationQ += g.getTargetQ();
                    }
                }
            }
        }
        if (boundariesSampled) {
            for (DanglingLine dl : network.getDanglingLines()) {
                boundariesP += dl.getP0();
                boundariesQ += dl.getQ0();
            }
        }
        return new SampleCharacteritics(loadPositiveP, loadPositiveQ, loadNegativeP, loadNegativeQ, generationP, generationQ, boundariesP, boundariesQ);
    }

    public SampleCharacteritics(double loadPositiveP, double loadPositiveQ, double loadNegativeP, double loadNegativeQ,
                                double generationP, double generationQ, double boundariesP, double boundariesQ) {
        this.loadPositiveP = loadPositiveP;
        this.loadPositiveQ = loadPositiveQ;
        this.loadNegativeP = loadNegativeP;
        this.loadNegativeQ = loadNegativeQ;
        this.generationP = generationP;
        this.generationQ = generationQ;
        this.boundariesP = boundariesP;
        this.boundariesQ = boundariesQ;
    }

    public double getLoadPositiveP() {
        return loadPositiveP;
    }

    public double getLoadPositiveQ() {
        return loadPositiveQ;
    }

    public double getLoadNegativeP() {
        return loadNegativeP;
    }

    public double getLoadNegativeQ() {
        return loadNegativeQ;
    }

    public double getGenerationP() {
        return generationP;
    }

    public double getGenerationQ() {
        return generationQ;
    }

    public double getBoundariesP() {
        return boundariesP;
    }

    public double getBoundariesQ() {
        return boundariesQ;
    }

    private static final double EPSILON = 1;

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof SampleCharacteritics) {
            SampleCharacteritics c = (SampleCharacteritics) obj;
            return Math.abs(loadPositiveP - c.loadPositiveP) < EPSILON
                    && Math.abs(loadNegativeP - c.loadNegativeP) < EPSILON
                    && Math.abs(loadPositiveQ - c.loadPositiveQ) < EPSILON
                    && Math.abs(loadNegativeQ - c.loadNegativeQ) < EPSILON
                    && Math.abs(generationP - c.generationP) < EPSILON
                    && Math.abs(generationQ - c.generationQ) < EPSILON
                    && Math.abs(boundariesP - c.boundariesP) < EPSILON
                    && Math.abs(boundariesQ - c.boundariesQ) < EPSILON;
        }
        return false;
    }

    @Override
    public String toString() {
        return "loadP=" + loadPositiveP + " + " + loadNegativeP + " MW"
                + " , loadQ=" + loadPositiveQ + " + " + loadNegativeQ + " MVar"
                + ", generationP=" + generationP + " MW"
                + ", generationQ=" + generationQ + " MVar"
                + ", boundariesP=" + boundariesP + " MW"
                + ", boundariesQ=" + boundariesQ + " MVar";
    }

}
