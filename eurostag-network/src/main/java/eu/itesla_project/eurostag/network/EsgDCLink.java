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
public class EsgDCLink {

    public enum LinkStatus {
        ON,
        OFF
    }

    private final Esg8charName node1Name; // sending node name
    private final Esg8charName node2Name; // receiving node name
    private final char xpp; // parallel index
    private final float rldc; // total link resistance [p.u.]
    private final LinkStatus linkStatus; // ' ' ON; 'S' OFF

    public EsgDCLink(Esg8charName node1Name, Esg8charName node2Name, char xpp, float rldc, LinkStatus linkStatus) {
        this.node1Name = Objects.requireNonNull(node1Name);
        this.node2Name = Objects.requireNonNull(node2Name);
        this.linkStatus = Objects.requireNonNull(linkStatus);
        this.xpp = xpp;
        this.rldc = rldc;
    }

    public Esg8charName getNode1Name() {
        return node1Name;
    }

    public Esg8charName getNode2Name() {
        return node2Name;
    }

    public char getXpp() {
        return xpp;
    }

    public float getRldc() {
        return rldc;
    }

    public LinkStatus getLinkStatus() {
        return linkStatus;
    }

    @Override
    public String toString() {
        return node1Name + "-" + node2Name + "-" + xpp + "";
    }

}
