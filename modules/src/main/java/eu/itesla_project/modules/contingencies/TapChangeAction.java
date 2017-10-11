/**
 * Copyright (c) 2016, All partners of the iTesla project (http://www.itesla-project.eu/consortium)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package eu.itesla_project.modules.contingencies;

import com.powsybl.contingency.tasks.ModificationTask;
import eu.itesla_project.modules.contingencies.tasks.PstTapChanging;

import java.util.Objects;

/**
 * @author Quinary <itesla@quinary.com>
 * @author Mathieu Bague <mathieu.bague@rte-france.com>
 */
public class TapChangeAction implements ActionElement {

    private final String equipmentId;

    private final int tapPosition;

    private final Number implementationTime;

    private final Number achievmentIndex;

    public TapChangeAction(String elementId, int tapPosition) {
        this(elementId, tapPosition, null, null);
    }

    public TapChangeAction(String elementId, int tapPosition, Number implementationTime, Number achievmentIndex) {
        this.equipmentId = Objects.requireNonNull(elementId);
        this.tapPosition = tapPosition;
        this.implementationTime = implementationTime;
        this.achievmentIndex = achievmentIndex;
    }

    @Override
    public ActionElementType getType() {
        return ActionElementType.TAP_CHANGE;
    }

    @Override
    public String getEquipmentId() {
        return equipmentId;
    }

    public int getTapPosition() {
        return tapPosition;
    }

    @Override
    public ModificationTask toTask() {
        return new PstTapChanging(equipmentId, tapPosition);
    }

    @Override
    public Number getImplementationTime() {
        return implementationTime;
    }

    @Override
    public Number getAchievmentIndex() {
        return achievmentIndex;
    }

    @Override
    public ModificationTask toTask(ActionParameters parameters) {
        throw new UnsupportedOperationException();
    }

}
