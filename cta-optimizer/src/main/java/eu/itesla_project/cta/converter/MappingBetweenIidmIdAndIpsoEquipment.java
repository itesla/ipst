/** Copyright (c) 2016, Tractebel (http://www.tractebel-engie.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this file,
 * You can obtain one at http://mozilla.org/MPL/2.0/.
 **/
package eu.itesla_project.cta.converter;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import eu.itesla_project.cta.model.IpsoEquipment;
import eu.itesla_project.cta.model.IpsoNode;
import eu.itesla_project.iidm.network.Bus;
import eu.itesla_project.iidm.network.Identifiable;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Strings.padEnd;
import static java.util.Optional.ofNullable;
/**
 * @author Yannick Pihan <yannick.pihan at tractebel.engie.com>
 */
public class MappingBetweenIidmIdAndIpsoEquipment {

    private final BiMap<String, IpsoEquipment> iidmId2IpsoEquipment;

    /**
     * default constructor
     */
    public MappingBetweenIidmIdAndIpsoEquipment() {
        this(new HashMap<String, IpsoEquipment>());
    }

    /**
     * copy constructor
     */
    public MappingBetweenIidmIdAndIpsoEquipment(Map<String, IpsoEquipment> iidmId2IpsoEquipment) {
        this.iidmId2IpsoEquipment = HashBiMap.create(iidmId2IpsoEquipment);
    }

    /**
     * Put iidm and ipso component names to the dictionary
     */
    public void add(Identifiable identifiable, IpsoEquipment ipsoComponent) {
        checkArgument(identifiable != null, "identifiable must not be null");
        checkArgument(ipsoComponent != null, "ipsoComponent must not be null");
        this.add(identifiable.getId(), ipsoComponent);
    }

    /**
     * Put iidm and ipso id's to the dictionary
     */
    public void add(String iidmId, IpsoEquipment ipsoComponent) {
        checkArgument(iidmId != null, "iidmId must not be null");
        checkArgument(ipsoComponent != null, "ipsoComponent must not be null");
        if (containsIpsoEquipmentFor(iidmId)) {
            throw new RuntimeException("IIDM id '" + iidmId + "' already exists in the dictionary");
        }
        iidmId2IpsoEquipment.put(iidmId, ipsoComponent);
    }

    /**
     * @return Ipso node corresponding to the given iidm bus
     */
    public Optional<IpsoNode> getIpsoNodeFor(Bus bus) {
        checkArgument(bus != null, "bus must not be null");
        return getIpsoNodeFor(bus.getId());
    }

    /**
     * @return Ipso node corresponding to the given iidm bus id
     */
    public Optional<IpsoNode> getIpsoNodeFor(String busId) {
        checkArgument(busId != null, "busId must not be null");
        return ofNullable((IpsoNode) iidmId2IpsoEquipment.get(busId));
    }

    public boolean containsIpsoEquipmentFor(String iidmId) {
        return iidmId2IpsoEquipment.containsKey(iidmId);
    }

    public Optional<IpsoEquipment> getIpsoEquipmentFor(String iidmId) {
        checkArgument(iidmId != null, "iidmId must not be null");
        return ofNullable(iidmId2IpsoEquipment.get(iidmId));
    }

    /**
     * @return true if ipso id is already defined in the dictionary
     */
    boolean containsIpsoId(String id) {
        checkArgument(id != null, "id must not be null");
        return iidmId2IpsoEquipment.values().stream()
                .anyMatch(ipsoComponent -> ipsoComponent.getId().equals(id));
    }

    public String getIidmIdFor(String ipsoName) {

        return iidmId2IpsoEquipment.values().stream()
                .filter(ipsoEquipment -> ipsoEquipment.getId().equals(padEnd(ipsoName, 20, ' ')))
                .findFirst()
                .map(IpsoEquipment::getIidmId)
                .orElseThrow(() -> new IllegalArgumentException("no such Iidm equipment for Ispo name=" + ipsoName));
    }

    public BiMap<String, IpsoEquipment> getIidmId2IpsoEquipment() {
        return iidmId2IpsoEquipment;
    }
}
