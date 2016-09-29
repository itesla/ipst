/** Copyright (c) 2016, Tractebel (http://www.tractebel-engie.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this file,
 * You can obtain one at http://mozilla.org/MPL/2.0/.
 **/
package eu.itesla_project.cta.converter;

import com.google.common.base.Preconditions;
import eu.itesla_project.cta.model.IpsoEquipment;
import eu.itesla_project.iidm.network.Identifiable;
import eu.itesla_project.iidm.network.Network;
import eu.itesla_project.iidm.network.util.Identifiables;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Yannick Pihan <yannick.pihan at tractebel.engie.com>
 *
 * @param <F> (From) iidm component
 * @param <T> (To)   ipso component
 */
abstract class AbstractModelConverter<F extends Identifiable, T extends IpsoEquipment> implements ModelConverter<F,T> {
    private static final float SNREF = 100.f;
    private static final float RATE = 100.f;
    private static final int ID_LENGTH_FOR_ONE_CONNECTION_COMPONENT = 8;
    private static final int ID_LENGTH_FOR_TWO_CONNECTIONS_COMPONENT = 20;
    private final ConversionContext context;

    protected AbstractModelConverter(ConversionContext context) {
        Preconditions.checkArgument(context != null, "context cannot be null");
        this.context = context;
    }

    @Override
    public List<T> convert(Network network) {
        List<T> result = new ArrayList<>();
        for (F toConvert : Identifiables.sort(gatherDataToConvertFrom(network))) {
            T converted = doConvert(toConvert);
            if (converted != null) {
                result.add(converted);
                addToDictionary(toConvert, converted);
            }
        }
        return result;
    }

    @Override
    public abstract Iterable<F> gatherDataToConvertFrom(Network network);

    @Override
    public String createIpsoId() {
        return context.createId(getComponentType(), ID_LENGTH_FOR_ONE_CONNECTION_COMPONENT);
    }

    /**
     * @return a new Ipso branch id from Ipso nodes ids.
     * If the id is already existing in the dictionary then
     * the parrallele index of the line is increased.
     */
    @Override
    public String createIdFrom(String nodeId1, String nodeId2) {
        String id12, id21;
        int index = 1;
        do {
           char paralleleIndex = Character.forDigit(index, Character.MAX_RADIX);
           id12 = nodeId1 + "-" + nodeId2 + "-" + paralleleIndex;
           id21 = nodeId2 + "-" + nodeId1 + "-" + paralleleIndex;
           id12 = StringUtils.rightPad(id12, ID_LENGTH_FOR_TWO_CONNECTIONS_COMPONENT);
           id21 = StringUtils.rightPad(id21, ID_LENGTH_FOR_TWO_CONNECTIONS_COMPONENT);
           index++;
        }
        while(getContext().getMappingBetweenIidmIdAndIpsoEquipment().containsIpsoId(id12) ||
              getContext().getMappingBetweenIidmIdAndIpsoEquipment().containsIpsoId(id21));

        return id12;
    }

    @Override
    public final ConversionContext getContext() {
        return context;
    }

    /**
     * @return the Ipso compoenent Type
     */
    protected abstract ComponentType getComponentType();

    /**
     * Performs the unitary conversion of iidm component to ipso component
     * @param toConvert
     * @return
     */
    protected abstract T doConvert(F toConvert);

    public void addToDictionary(F fromConvert, T toConvert ) {
        context.getMappingBetweenIidmIdAndIpsoEquipment().add(getIdOf(fromConvert), toConvert);
    }

    protected String getIdOf(F fromConvert) {
        return fromConvert.getId();
    }

    /**
     * Base power as reference for the ipso network
     * @return snref
     */
    public static float snref() {
        return SNREF;
    }

    /**
     * Base rate as reference for the ipso network
     * @return rate
     */
    public static float rate() {
        return RATE;
    }

}