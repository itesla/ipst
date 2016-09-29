/** Copyright (c) 2016, Tractebel (http://www.tractebel-engie.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this file,
 * You can obtain one at http://mozilla.org/MPL/2.0/.
 **/
package eu.itesla_project.cta.converter;

import eu.itesla_project.cta.model.IpsoEquipment;
import eu.itesla_project.cta.model.IpsoLine;
import eu.itesla_project.cta.model.IpsoNode;
import eu.itesla_project.cta.model.IpsoNodeType;
import eu.itesla_project.iidm.network.Line;
import eu.itesla_project.iidm.network.Network;
import org.apache.commons.lang3.StringUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
/**
 * @author Yannick Pihan <yannick.pihan at tractebel.engie.com>
 */
public class LineModelConverterImplTest {

    private ConversionContext context;
    private static final int TOTAL_LENGTH = 20;

    @Before
    public void setup() {
        context = new ConversionContext("caseName");
        MappingBetweenIidmIdAndIpsoEquipment dictionary = context.getMappingBetweenIidmIdAndIpsoEquipment();
        dictionary.add("iidm0", creatIpsoLineWithTheId("node1-node2-0"));
        dictionary.add("iidm1", creatIpsoLineWithTheId("node1-node2-1"));
        dictionary.add("iidm2", creatIpsoLineWithTheId("node1-node2-2"));
        dictionary.add("iidm3", creatIpsoLineWithTheId("node1-node2-3"));
        dictionary.add("iidm4", creatIpsoLineWithTheId("node1-node2-4"));
        dictionary.add("iidm5", creatIpsoLineWithTheId("node1-node2-5"));
        dictionary.add("iidm6", creatIpsoLineWithTheId("node1-node2-6"));
        dictionary.add("iidm7", creatIpsoLineWithTheId("node1-node2-7"));
        dictionary.add("iidm8", creatIpsoLineWithTheId("node1-node2-8"));
        dictionary.add("iidm9", creatIpsoLineWithTheId("node1-node2-9"));
    }

    private IpsoEquipment creatIpsoLineWithTheId(String id) {
        return new IpsoLine(StringUtils.rightPad(id, TOTAL_LENGTH), "iidm id", createIpsoNode("node1"), createIpsoNode("node2"), true, true, 0f,0f, 0f, 0f, 0f, 0f, 0f, 0);
    }

    private IpsoNode createIpsoNode(String node1) {
        return new IpsoNode(node1, "iidm id", "", "", 0,0,0, IpsoNodeType.PQ,0,0,0,0,0);
    }

    @Test
    public void createIpsoNameReturnsLineWithUniqueName() {

        LineModelConverter converter = new LineModelConverter(context) {
            @Override
            public Iterable<Line> gatherDataToConvertFrom(Network network) {
                throw new UnsupportedOperationException("Not supported yet.");
            }

            @Override
            protected ComponentType getComponentType() {
                throw new UnsupportedOperationException("Not supported yet.");
            };
        };

        Assert.assertEquals(converter.createIdFrom("node1", "node2"), "node1-node2-a       ");
        context.getMappingBetweenIidmIdAndIpsoEquipment().add("iid10", creatIpsoLineWithTheId("node1-node2-a"));
        Assert.assertEquals(converter.createIdFrom("node1", "node2"), "node1-node2-b       ");
        Assert.assertEquals(converter.createIdFrom("node1", "node3"), "node1-node3-1       ");
        context.getMappingBetweenIidmIdAndIpsoEquipment().add("iid11", creatIpsoLineWithTheId("node1-node3-1"));
        Assert.assertEquals(converter.createIdFrom("node1", "node3"), "node1-node3-2       ");
    }
}
