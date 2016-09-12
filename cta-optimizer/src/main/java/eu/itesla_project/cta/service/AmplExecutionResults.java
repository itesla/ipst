/** Copyright (c) 2016, Tractebel (http://www.tractebel-engie.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this file,
 * You can obtain one at http://mozilla.org/MPL/2.0/.
 **/
package eu.itesla_project.cta.service;

import eu.itesla_project.modules.contingencies.ActionParameters;

import java.util.HashMap;
import java.util.Map;
/**
 * @author Yannick Pihan <yannick.pihan at tractebel.engie.com>
 */
class AmplExecutionResults {

    private final Map<String, ActionParameters> actionElements;
    private final AmplStatus status;

    AmplExecutionResults(Map<String, ActionParameters> actionElements, AmplStatus status) {
        this.actionElements = actionElements;
        this.status = status;
    }

    public AmplExecutionResults(AmplStatus status) {
        actionElements = new HashMap<>();
        this.status = status;
    }

    public Map<String, ActionParameters> getActionElements() {
        return actionElements;
    }

    public AmplStatus getStatus() {
        return status;
    }
}
