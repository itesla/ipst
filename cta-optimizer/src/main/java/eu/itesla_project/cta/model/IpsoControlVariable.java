/** Copyright (c) 2016, Tractebel (http://www.tractebel-engie.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this file,
 * You can obtain one at http://mozilla.org/MPL/2.0/.
 **/
package eu.itesla_project.cta.model;

import com.google.common.collect.Lists;
import eu.itesla_project.cta.service.IpsoIdUtil;
import org.slf4j.Logger;

import java.util.List;

import static org.slf4j.LoggerFactory.getLogger;
/**
 * @author Yannick Pihan <yannick.pihan at tractebel.engie.com>
 */
public abstract class IpsoControlVariable<T extends IpsoEquipment> extends IpsoComponent implements IpsoProblemDefinitionElement<T> {

    private static final Logger LOG = getLogger(IpsoControlVariable.class);

    private final T equipment;
    private final float controlledValue;

    public IpsoControlVariable(T equipment, float controlledValue, int world) {
        super(IpsoIdUtil.getNextUniqueId(), world);
        this.equipment = equipment;
        this.controlledValue = controlledValue;
    }

    @Override
    public List<Object> getOrderedValues() {
        return Lists.newArrayList(equipment.getId(), controlledValue, getWorld());
    }

    @Override
    public T getRelatedIpsoEquipment() {
        return equipment;
    }

    public float getControlledValue() {
        return controlledValue;
    }

    @Override
    public void logNaN(String attributeName, float defaultValue) {
        LOG.warn("The Ipso control variable {} for {} ({}) has NaN for attibute {}. It is replaced by {}",
                this.getClass().getSimpleName(),
                equipment.getId(),
                equipment.getIidmId(),
                attributeName,
                defaultValue);
    }

    @Override
    public String toString() {
        return String.format("Control variable id=%s, %s=%s (%s)",
                getId(),
                getRelatedIpsoEquipment().getClass().getSimpleName(),
                getRelatedIpsoEquipment().getId(),
                getRelatedIpsoEquipment().getIidmId(),
                getControlledValue());
    }

    @Override
    public boolean isVariable() {
        return true;
    }

    @Override
    public boolean isConstraint() {
        return false;
    }
}
