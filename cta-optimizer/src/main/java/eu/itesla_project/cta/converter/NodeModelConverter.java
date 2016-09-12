/** Copyright (c) 2016, Tractebel (http://www.tractebel-engie.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this file,
 * You can obtain one at http://mozilla.org/MPL/2.0/.
 **/
package eu.itesla_project.cta.converter;

import com.google.common.base.Function;
import com.google.common.base.Preconditions;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;
import com.google.gdata.util.common.base.Pair;
import eu.itesla_project.cta.model.IpsoNode;
import eu.itesla_project.cta.model.IpsoNodeType;
import eu.itesla_project.cta.model.IpsoRegionType;
import eu.itesla_project.iidm.network.Bus;
import eu.itesla_project.iidm.network.Network;
import eu.itesla_project.iidm.network.util.Identifiables;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
/**
 * @author Yannick Pihan <yannick.pihan at tractebel.engie.com>
 */
class NodeModelConverter extends AbstractNodeModelConverter<Bus> {

    private static final Logger LOG = LoggerFactory.getLogger(NodeModelConverter.class);

    public NodeModelConverter(ConversionContext context) {
        super(context);
    }

    /**
     * Convert iidm bus to ipso node .
     * Moreover, the conversion performs the research of
     * macro region as internal or external and de type PV, SB or PQ for nodes
     *
     * @param network
     * @return list of node for ipso
     */
    @Override
    public List<IpsoNode> convert(Network network) {
        Preconditions.checkArgument(network != null, "network cannto be null");
        List<IpsoNode> nodes = new ArrayList<>();

        SpecialNodeFinder specialNodesFinder = new SpecialNodeFinder();
        for(Bus bus : Identifiables.sort(gatherDataToConvertFrom(network)) ) {
            // check if the node is special
            specialNodesFinder.browse(bus);
            // createAmplModelFrom it to Ipso node
            final IpsoNode ipsoNode = this.doConvert(bus);
            // store created ipso node
            nodes.add(ipsoNode);
            // add bus and node names to the dictionary
            super.addToDictionary(bus, ipsoNode);
        }

        // set PV node type
        definePvNodesFor(nodes, specialNodesFinder);

        // set slackbus type
        if (specialNodesFinder.hasSlackBusFound() ) {
            defineSlackBusfor(nodes, specialNodesFinder);
        }
        else {
            LOG.error("no slackbus found!");
        }

        // set Macro region internal or external
        defineMacroRegionOf(nodes);

        return nodes;
    }

    @Override
    protected ComponentType getComponentType() {
        return ComponentType.NODE;
    }

    @Override
    public Iterable<Bus> gatherDataToConvertFrom(Network network) {
        return network.getBusBreakerView().getBuses();
    }

    @Override
    protected IpsoNode doConvert(Bus bus) {
        return super.convertBus(bus.getId(), bus);
    }

    /**
     * Change node type to slackbus in the list of Ipso nodes
     * @param nodes
     * @param specialNodeFinder
     */
    private void defineSlackBusfor(List<IpsoNode> nodes, SpecialNodeFinder specialNodeFinder) {
            int index = specialNodeFinder.getSlackBusIndex();
            IpsoNode ipsoNode = nodes.get(index);
            // set the type of node to SB
            ipsoNode.setNodeType(IpsoNodeType.SB);
            nodes.set(index ,ipsoNode);
    }

    private void definePvNodesFor(List<IpsoNode> nodes, SpecialNodeFinder specialNodeFinder) {
        for(int index : specialNodeFinder.getPvNodeIndexes()) {
            IpsoNode ipsoNode = nodes.get(index);
            if (!ipsoNode.isSlackBus()) {
                // set the type of node to PV
                ipsoNode.setNodeType(IpsoNodeType.PV);
                // TODO à confirmer
                //ipsoNode.setValue(NodeAttribute.ACTIVE_POWER, specialNodeFinder.getSlackBusGenerator().getTargetP() );
                nodes.set(index, ipsoNode);
            }
        }
    }

    /**
     * Defines the macroregion of node as internal or external accordingly to
     * the number of node by region. The bigger number of nodes for a given region
     * will be set as "Internal", others as "External"
     * @param nodes
     */
    private void defineMacroRegionOf(List<IpsoNode> nodes) {
        Preconditions.checkArgument(!nodes.isEmpty(), "At least one node is required.");

        defineEveryNodeAsExternal(nodes);
        // order IpsoNode by Country
        Multimap<String, IpsoNode> nodesByCountry = Multimaps.index(nodes, byCountry());
        String internalCountry = findCountryThatContainsTheBiggestNumberOfNodes(nodesByCountry);

         // internal nodes
        for (IpsoNode node : nodesByCountry.get(internalCountry)) {
            node.setMacroRegionName(IpsoRegionType.INTERNAL.getValue());
        }
    }

    private Function<IpsoNode, String> byCountry() {
        return input -> input.getRegionName();
    }

    private String findCountryThatContainsTheBiggestNumberOfNodes(Multimap<String, IpsoNode> nodeByCountry) {
        Pair<String, Integer> maxLengthWithCountry = null;
        for (String country : nodeByCountry.keySet()) {
            int numberOfNodes = nodeByCountry.get(country).size();
            if (maxLengthWithCountry == null || maxLengthWithCountry.getSecond() < numberOfNodes) {
                maxLengthWithCountry = Pair.of(country, numberOfNodes);
            }
        }
        return maxLengthWithCountry.getFirst();
    }

    private void defineEveryNodeAsExternal(List<IpsoNode> nodes) {
        // default values
        for (IpsoNode node : nodes) {
            node.setMacroRegionName(IpsoRegionType.EXTERNAL.getValue());
        }
    }

}
