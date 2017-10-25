/**
 * Copyright (c) 2016, All partners of the iTesla project (http://www.itesla-project.eu/consortium)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package itesla.converter;

import java.util.ArrayList;
import java.util.List;

/**
 * Single block of the Eurostag macroblock
 * @author Marc Sabate <sabatem@aia.es>
 * @author Raul Viruez <viruezr@aia.es>
 */
public class Block {
    public String[] param = new String[8];
    public String[] entries = new String[5];
    public String output;
    public Integer GraphicalNumber;
    public Integer idEu;
    public Integer count;
    public List<Boolean> UsedInputPins = new ArrayList<Boolean>();

    public Block(String[] param, String[] entries, String output, Integer graphicalNumber, Integer idEu, Integer cont, Integer nInputPins) {
        this.param = param;
        this.entries = entries;
        this.output = output;
        this.GraphicalNumber = graphicalNumber;
        this.idEu = idEu;
        this.count = cont;
        for (int i = 0; i < nInputPins; i++) {
            UsedInputPins.add(false);
        }
    }
}
