/** Copyright (c) 2016, Tractebel (http://www.tractebel-engie.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this file,
 * You can obtain one at http://mozilla.org/MPL/2.0/.
 **/
package eu.itesla_project.cta.model;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.slf4j.Logger;

import static com.google.common.base.Preconditions.checkArgument;
import static org.apache.commons.lang3.StringUtils.isNotEmpty;
import static org.slf4j.LoggerFactory.getLogger;
/**
 * @author Yannick Pihan <yannick.pihan at tractebel.engie.com>
 */
public abstract class IpsoComponent implements IpsoWritable{

    private static final Logger LOG = getLogger(IpsoComponent.class);

    protected static final String NAME = "NAME";
    protected static final String WORLD = "WORLD";

    private final String id;
    private final int world;
    public IpsoComponent(String id, int world) {
        checkArgument(isNotEmpty(id), "id cannot be null or empty");
        checkArgument(world >= 0, "world must be greater or equal to zero");
        this.id = id;
        this.world = world;
    }

    /**
     * @return id of the Ipso component
     * Remark: The id is limited to a specific number of character in
     * according to the type of component
     */
    public String getId() {
        return id;
    }

    /**
     * @return the network state index of the component:
     * <p>0 - healthy state</p>
     * <p>1 - network state for the first contengy</p>
     * <p>2 - network state for the second contengy</p>     *
     * <p>...</p>
     */
    public int getWorld() {
        return world;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (!(o instanceof IpsoComponent)) {
            return false;
        }

        IpsoComponent that = (IpsoComponent) o;
        return new EqualsBuilder().append(id, that.getId()).isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder().append(id).toHashCode();
    }

    /**
     * @return replace NaN by a default value and log it
     */
    protected float replaceNanAndLogIt(String attributeName, float value, float defaultValue) {
        checkArgument(attributeName != null, "attributeName must not be null");
        if ( Float.isNaN(value)) {
            logNaN(attributeName, defaultValue);
        }
        return DataUtil.getSafeValueOf(value, defaultValue);
    }

    public void logNaN(String attributeName, float defaultValue) {
        LOG.warn("The Ipos component {} {} has NaN for attibute {}. It is replaced by {}",
                getClass().getSimpleName(),
                getId(),
                attributeName,
                defaultValue);
    }

    @Override
    public String toString() {
        return String.format("%s %s",
                getClass().getSimpleName(),
                getOrderedHeaders().toString());
    }
}
