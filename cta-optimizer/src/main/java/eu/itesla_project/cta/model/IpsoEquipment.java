/** Copyright (c) 2016, Tractebel (http://www.tractebel-engie.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this file,
 * You can obtain one at http://mozilla.org/MPL/2.0/.
 **/
package eu.itesla_project.cta.model;

import org.slf4j.Logger;

import static org.slf4j.LoggerFactory.getLogger;
/**
 * @author Yannick Pihan <yannick.pihan at tractebel.engie.com>
 */
public abstract class IpsoEquipment extends IpsoComponent {

    private static final Logger LOG = getLogger(IpsoEquipment.class);

    private final String iidmId;

    public IpsoEquipment(String id, String iidmId, int world) {
        super(id, world);
        this.iidmId = iidmId;
    }

    public String getIidmId() {
        return iidmId;
    }

    @Override
    public void logNaN(String attributeName, float defaultValue) {
        LOG.warn("The Ipso equipment {} {} ({}) has NaN for attibute {}. It is replaced by {}",
                this.getClass().getSimpleName(),
                getId(),
                iidmId,
                attributeName,
                defaultValue);
    }

    @Override
    public String toString() {
        return String.format("%s, %s=%s (%s)",
                getId(),
                getClass().getSimpleName(),
                getId(),
                getIidmId());
    }
}
