/**
 * Copyright (c) 2016, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package eu.itesla_project.online.rest.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
*
* @author Giovanni Ferrari <giovanni.ferrari@techrain.it>
*/
public class ProcessSynthesis {
    private final String processId;
    private final List<StateSynthesis> states = new ArrayList();

    public ProcessSynthesis(String processId) {
        this.processId = Objects.requireNonNull(processId);
    }

    public String getProcessId() {
        return processId;
    }

    public List<StateSynthesis> getStates() {
        return states;
    }

    public void addStateSynthesis(List<StateSynthesis> states) {
        Objects.requireNonNull(states);
        this.states.addAll(states);
    }

    public void addStateSynthesis(StateSynthesis state) {
        Objects.requireNonNull(state);
        this.states.add(state);
    }

}
