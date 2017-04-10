/**
 * Copyright (c) 2016, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package eu.itesla_project.online.rest.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
*
* @author Giovanni Ferrari <giovanni.ferrari@techrain.it>
*/
public class StateSynthesis {
    private final int state;
    private final List<ViolationSynthesis> preContingencyViolations = new ArrayList();
    private final Map<String, List<ViolationSynthesis>> postContingencyViolations = new HashMap();

    public StateSynthesis(int state){
        this.state = state;
    }

    public int getState() {
        return state;
    }

    public List<ViolationSynthesis> getPreContingencyViolations() {
        return preContingencyViolations;
    }

    public Map<String, List<ViolationSynthesis>> getPostContingencyViolations() {
        return postContingencyViolations;
    }

}
