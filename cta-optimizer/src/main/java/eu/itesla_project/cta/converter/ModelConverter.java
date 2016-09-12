/** Copyright (c) 2016, Tractebel (http://www.tractebel-engie.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this file,
 * You can obtain one at http://mozilla.org/MPL/2.0/.
 **/
package eu.itesla_project.cta.converter;

import eu.itesla_project.cta.model.IpsoComponent;
import eu.itesla_project.cta.model.IpsoEquipment;
import eu.itesla_project.iidm.network.Identifiable;
import eu.itesla_project.iidm.network.Network;

import java.util.List;
/**
 * @author Yannick Pihan <yannick.pihan at tractebel.engie.com>
 */
interface ModelConverter<F extends Identifiable, T extends IpsoEquipment> {

    /**
     * Convert data from iidm network to a list of {@link IpsoComponent}.
     * The data to createAmplModelFrom is handled in the method {@link  ModelConverter#gatherDataToConvertFrom }
     * All components converted are added to the dictionary
     * of the {@link ConversionContext}
     * @param network
     * @return list of converted components to Ipso format
     */
    List<T> convert(Network network);

    /**
     * Gather iidm data to createAmplModelFrom
     * @param network
     * @return a list of iidm components
     */
    Iterable<F> gatherDataToConvertFrom(Network network);

    /**
     * Create new name for ipso component with one connection.
     * <p>Example: C000002 (where 'C' is a prefix)</p>
     * <p>The prefix of the name is given from {@link ComponentType }</p>
     * @return unique name
     * @see ComponentType
     */
    String createIpsoId();

    /**
     * Create new name for two side connectable ipso component
     * <p>The new name is composeb by name1-name2-x</p>
     * <p>with x as parrallele index</p>
     * @param nodeName1
     * @param nodeName2
     * @return  unique name
     */
    String createIdFrom(String nodeName1, String nodeName2);

    /**
     * @return the conversion context
     */
    ConversionContext getContext();
}
