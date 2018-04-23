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
public class EsgDCNode {

    private final Esg2charName area; // zone identifier
    private final Esg8charName name; // node name
    private final float vbase; // base voltage [kV]
    private final float vinit; // Initial voltage [p.u.]

    public EsgDCNode(Esg2charName area, Esg8charName name, float vbase, float vinit) {
        this.area = Objects.requireNonNull(area);
        this.name = Objects.requireNonNull(name);
        this.vbase = vbase;
        this.vinit = vinit;
    }

    public Esg2charName getArea() {
        return area;
    }

    public Esg8charName getName() {
        return name;
    }

    public float getVbase() {
        return vbase;
    }

    public float getVinit() {
        return vinit;
    }

}
