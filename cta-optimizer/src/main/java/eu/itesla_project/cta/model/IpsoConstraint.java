/** Copyright (c) 2016, Tractebel (http://www.tractebel-engie.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this file,
 * You can obtain one at http://mozilla.org/MPL/2.0/.
 **/
package eu.itesla_project.cta.model;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import eu.itesla_project.cta.service.IpsoIdUtil;
import org.slf4j.Logger;

import java.util.List;
import java.util.Set;

import static org.slf4j.LoggerFactory.getLogger;
/**
 * @author Yannick Pihan <yannick.pihan at tractebel.engie.com>
 */
public abstract class IpsoConstraint<T extends IpsoEquipment> extends IpsoComponent implements IpsoProblemDefinitionElement<T> {

    private static final Logger LOG = getLogger(IpsoConstraint.class);
    protected static final String UNIT = "UNIT";

    private final T equipment;

    private final float boundsMin;
    private final float boundsMax;

    public IpsoConstraint(T equipment, float boundsMin, float boundsMax, int world) {
        super(IpsoIdUtil.getNextUniqueId(), world);
        this.equipment = equipment;
        this.boundsMin = replaceNanAndLogIt("MIN", boundsMin, defaultBoundsMin());
        this.boundsMax = replaceNanAndLogIt("MAX", boundsMax, defaultBoundsMax());
    }

    protected float defaultBoundsMin() {
        return -9999f;
    }

    protected float defaultBoundsMax() {
        return 9999f;
    }

    public boolean isViolated() {
        return IpsoBoundsEvaluator.isOutOfBounds(getConstrainedAttributeValueFor(equipment), boundsMin, boundsMax);
    }

    public boolean isNotViolated() {
        return !isViolated();
    }

    @Override
    public List<Object> getOrderedValues() {
        return Lists.newArrayList(equipment.getId(), getBoundsMin(), getBoundsMax(), getWorld());
    }

    public float getBoundsMin() {
        return boundsMin;
    }

    public float getBoundsMax() {
        return boundsMax;
    }

    protected abstract float getConstrainedAttributeValueFor(T equipment);

    public float getConstrainedValue() {
        return getConstrainedAttributeValueFor(equipment);
    }

    @Override
    public T getRelatedIpsoEquipment() {
        return equipment;
    }

    public boolean isSetpointConstraint() {
        return IpsoBoundsEvaluator.isConfoundedBounds(boundsMin, boundsMax);
    }

    public boolean isNotSetpointConstraint() {
        return !isSetpointConstraint();
    }

    @Override
    public void logNaN(String attributeName, float defaultValue) {
        LOG.warn("The Ipso Constraint {} for {} ({}) has NaN for attibute {}. It is replaced by {}",
                this.getClass().getSimpleName(),
                equipment.getId(),
                equipment.getIidmId(),
                attributeName,
                defaultValue);
    }

    @Override
    public String toString() {
        return String.format("Constraint (%s) id=%s, %s=%s (%s), value=%s, min=%s, max = %s",
                this.getClass().getSimpleName(),
                getId(),
                getRelatedIpsoEquipment().getClass().getSimpleName(),
                getRelatedIpsoEquipment().getId(),
                getRelatedIpsoEquipment().getIidmId(),
                getConstrainedValue(),
                getBoundsMin(),
                getBoundsMax());
    }

    @Override
    public boolean isConstraint() {
        return true;
    }

    @Override
    public boolean isVariable() {
        return false;
    }


    private static Set<Class<? extends IpsoConstraint>> flowConstraints = Sets.newHashSet(
            IpsoConstraint2WTransformerFlow.class,
            IpsoConstraint3WTransformerFlow.class,
            IpsoConstraintLineFlowSide1.class,
            IpsoConstraintLineFlowSide2.class
    );

    public boolean isCurrentViolation() {
        return (flowConstraints.contains(this.getClass()));
    }
}
