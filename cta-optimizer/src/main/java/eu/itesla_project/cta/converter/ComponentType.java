/** Copyright (c) 2016, Tractebel (http://www.tractebel-engie.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this file,
 * You can obtain one at http://mozilla.org/MPL/2.0/.
 **/
package eu.itesla_project.cta.converter;

/**
 * @author Yannick Pihan <yannick.pihan at tractebel.engie.com>
 */
public enum ComponentType {
    NETWORK('A'),
    NODE('N'),
    XNODE('X'),
    LINE('-'),
    DANGLING_LINE('-'),
    TWO_WINDINGS_TRANSFORMER('-'),
    THREE_WINDINGS_TRANSFORMER('-'),
    COUPLING('-'),
    GENERATOR('G'),
    LOAD('L'),
    XLOAD('Z'),
    BANK('B');

    private char type;

    ComponentType(char type) {
        this.type = type;
    }

    public char getChar() {
        return type;
    }
}
