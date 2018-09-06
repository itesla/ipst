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
public class EsgNode {

    private final Esg2charName area; // zone identifier
    private final Esg8charName name; // node name
    private final double vbase; // base voltage [kV]
    private final double vinit; // Initial voltage [p.u.]
    private final double vangl; // Initial angle of the voltage [deg]
    private final boolean slackBus;

    public EsgNode(Esg2charName area, Esg8charName name, double vbase, double vinit, double vangl, boolean slackBus) {
        this.area = Objects.requireNonNull(area);
        this.name = Objects.requireNonNull(name);
        this.vbase = vbase;
        this.vinit = vinit;
        this.vangl = vangl;
        this.slackBus = slackBus;
    }

    public Esg2charName getArea() {
        return area;
    }

    public Esg8charName getName() {
        return name;
    }

    public double getVangl() {
        return vangl;
    }

    public double getVbase() {
        return vbase;
    }

    public double getVinit() {
        return vinit;
    }

    public boolean isSlackBus() {
        return slackBus;
    }
}
