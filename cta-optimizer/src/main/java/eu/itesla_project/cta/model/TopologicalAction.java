/** Copyright (c) 2016, Tractebel (http://www.tractebel-engie.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this file,
 * You can obtain one at http://mozilla.org/MPL/2.0/.
 **/
package eu.itesla_project.cta.model;

import eu.itesla_project.cta.service.AmplConstants;
import eu.itesla_project.modules.contingencies.ActionParameters;

/**
 * @author Yannick Pihan <yannick.pihan at tractebel.engie.com>
 */
public class TopologicalAction {

    final String equipmentId;
    final SwitchAction switchAction;
    final String elementaryActionId;

    public TopologicalAction(String equipmentId, SwitchAction switchAction, String elementaryActionId) {
        this.equipmentId = equipmentId;
        this.switchAction = switchAction;
        this.elementaryActionId = elementaryActionId;
    }

    public String getEquipmentId() { return equipmentId; }

    public SwitchAction getSwitchAction() { return switchAction; }

    public String getElementaryActionId() {
        return elementaryActionId;
    }

    @Override
    public String toString() {
        return String.format("Equipment %s - Action: %s",
                equipmentId,
                switchAction.toString());
    }

    public boolean isEquivalentTo(ActionParameters action) {
        return action.getNames().stream()
                .filter(name -> name.equals(AmplConstants.SWITCH_TAG))
                .map(action::getValue)
                .filter(value -> value instanceof Boolean)
                .anyMatch(value -> !switchAction.isOppositeTo((boolean)value));
    }


}
