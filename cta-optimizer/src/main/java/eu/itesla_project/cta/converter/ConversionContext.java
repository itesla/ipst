/** Copyright (c) 2016, Tractebel (http://www.tractebel-engie.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this file,
 * You can obtain one at http://mozilla.org/MPL/2.0/.
 **/
package eu.itesla_project.cta.converter;

import com.google.common.base.Preconditions;

import java.util.HashMap;
import java.util.Map;
/**
 * @author Yannick Pihan <yannick.pihan at tractebel.engie.com>
 */
public class ConversionContext {

    private MappingBetweenIidmIdAndIpsoEquipment mappingBetweenIidmIdAndIpsoEquipment;

    private Map<Character,Integer> indexByPrefix = new HashMap<>();
    private String caseName;

    /**
     * constructor
     */
    public ConversionContext(String caseName) {
        Preconditions.checkArgument(caseName != null, "caseName cannot be null");
        this.mappingBetweenIidmIdAndIpsoEquipment = new MappingBetweenIidmIdAndIpsoEquipment();
        this.caseName = caseName;
    }

    /**
     * Create a unique name by Ipso component types
     * @param componentType, an enumeration to identify NODE, LINE, ...
     * @param nameMaxLength, the max length of the generated name
     * @return the generated name
     */
    public String createId(ComponentType componentType, int nameMaxLength) {
        // increase the index associeted to the component (through its prefix)
        char prefix = componentType.getChar();
        int increasedIndex = increaseIndexFor(prefix);
        // compute the length of the name
        int nameLength = nameMaxLength - 1;
        return String.format("%s%0" + nameLength + "d" ,
                prefix,
                increasedIndex);
    }

    /**
     * Increase index value associated to a prefix
     * @param prefix
     * @return increased index
     */
    private int increaseIndexFor(char prefix) {
        int index = 0;
        if ( indexByPrefix.containsKey(prefix)) {
            index = indexByPrefix.get(prefix) + 1;
            indexByPrefix.put(prefix, index);
        }
        else {
            indexByPrefix.put(prefix, index);
        }
        return index;
    }

    public MappingBetweenIidmIdAndIpsoEquipment getMappingBetweenIidmIdAndIpsoEquipment() {
        return mappingBetweenIidmIdAndIpsoEquipment;
    }

    int getWorld() {
        return 0;
    }

    /**
     * Should be a ressource such as Eurostag export
     * @return flase by default
     */
    public boolean isNoGeneratorMinMaxQ() {
        return false;
    }

    String getCaseName() {
        return caseName;
    }
}
