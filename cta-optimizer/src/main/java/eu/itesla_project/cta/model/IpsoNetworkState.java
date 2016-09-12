/** Copyright (c) 2016, Tractebel (http://www.tractebel-engie.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this file,
 * You can obtain one at http://mozilla.org/MPL/2.0/.
 **/
package eu.itesla_project.cta.model;

import eu.itesla_project.cta.converter.MappingBetweenIidmIdAndIpsoEquipment;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Stack;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toList;
/**
 * @author Yannick Pihan <yannick.pihan at tractebel.engie.com>
 */
public class IpsoNetworkState implements IpsoOptimizationDataInput {

    private final MappingBetweenIidmIdAndIpsoEquipment mappingBetweenIidmIdAndIpsoEquipment;
    private final int world;
    private String caseName;
    private List<IpsoNode> ipsoNodes;
    private List<IpsoLine> ipsoLines;
    private List<IpsoGenerator> ipsoGenerators;
    private List<IpsoLoad> ipsoLoads;
    private List<IpsoBank> ipsoBanks;
    private List<IpsoCoupling> ipsoCouplings;
    private List<IpsoTwoWindingsTransformer> ipsoTwoWindingsTransformers;

    /**
     * Constructor
     */
    public IpsoNetworkState(int world, String caseName, MappingBetweenIidmIdAndIpsoEquipment mappingBetweenIidmIdAndIpsoEquipment) {
        this.world = world;
        this.caseName = caseName;
        this.ipsoNodes = new ArrayList<>();
        this.ipsoLines = new ArrayList<>();
        this.ipsoGenerators = new ArrayList<>();
        this.ipsoLoads = new ArrayList<>();
        this.ipsoBanks = new ArrayList<>();
        this.ipsoCouplings = new ArrayList<>();
        this.ipsoTwoWindingsTransformers = new ArrayList<>();
        this.mappingBetweenIidmIdAndIpsoEquipment = mappingBetweenIidmIdAndIpsoEquipment;
    }

    public IpsoNetworkState(IpsoNetworkState ipsoNetworkState, int contingencyIndex, MappingBetweenIidmIdAndIpsoEquipment mappingBetweenIidmIdAndIpsoEquipment) {
        super();
        this.world = contingencyIndex+1;
        this.caseName = ipsoNetworkState.getCaseName();
        this.ipsoNodes = new ArrayList<>(ipsoNetworkState.getIpsoNodes());
        this.ipsoLines = new ArrayList<>(ipsoNetworkState.getIpsoLines());
        this.ipsoGenerators = new ArrayList<>(ipsoNetworkState.getIpsoGenerators());
        this.ipsoLoads = new ArrayList<>(ipsoNetworkState.getIpsoLoads());
        this.ipsoBanks = new ArrayList<>(ipsoNetworkState.getIpsoBanks());
        this.ipsoCouplings = new ArrayList<>(ipsoNetworkState.getIpsoCouplings());
        this.ipsoTwoWindingsTransformers = new ArrayList<>(ipsoNetworkState.getIpsoTwoWindingsTransformers());
        this.mappingBetweenIidmIdAndIpsoEquipment = mappingBetweenIidmIdAndIpsoEquipment;
    }


    public List<IpsoNode> getIpsoNodes() {
        return ipsoNodes;
    }

    public List<IpsoLine> getIpsoLines() {
        return ipsoLines;
    }

    public List<IpsoGenerator> getIpsoGenerators() {
        return ipsoGenerators;
    }

    public List<IpsoLoad> getIpsoLoads() {
        return ipsoLoads;
    }

    public List<IpsoBank> getIpsoBanks() {
        return ipsoBanks;
    }

    public List<IpsoCoupling> getIpsoCouplings() {
        return ipsoCouplings;
    }

    public List<IpsoTwoWindingsTransformer> getIpsoTwoWindingsTransformers() {
        return ipsoTwoWindingsTransformers;
    }

    public void addNodes(List<IpsoNode> nodes) {
        ipsoNodes.addAll(nodes);
    }

    public void addNode(IpsoNode nodes) { ipsoNodes.add(nodes); }

    public void addLines(List<IpsoLine> lines) {
        ipsoLines.addAll(lines);
    }

    public void addGenerators(List<IpsoGenerator> generators) {
        this.ipsoGenerators.addAll(generators);
    }

    public void addLoads(List<IpsoLoad> loads) {
        this.ipsoLoads.addAll(loads);
    }

    public void addBanks(List<IpsoBank> banks) {
        this.ipsoBanks.addAll(banks);
    }

    public void addCouplings(List<IpsoCoupling> couplings) {
        this.ipsoCouplings.addAll(couplings);
    }

    public void addTwoWindingsTransformers(List<IpsoTwoWindingsTransformer> transformers) {
        this.ipsoTwoWindingsTransformers.addAll(transformers);
    }

    @Override
    public int getWorld() {
        return world;
    }

    @Override
    public String getCaseName() {
        return caseName;
    }

    public Stream<IpsoGenerator> getConnectedAndRegulatingGenerators() {
        return getIpsoGenerators().stream()
                .filter(IpsoGenerator::isConnected)
                .filter(IpsoGenerator::isRegulating);
    }

    public Stream<IpsoTwoWindingsTransformer> getConnectedAndRegulatingRatioTapChangerTransformer() {
        return getIpsoTwoWindingsTransformers().stream()
                .filter(IpsoTwoWindingsTransformer::isConnectedOnBothSides)
                .filter(IpsoTwoWindingsTransformer::isRegulating)
                .filter(IpsoTwoWindingsTransformer::isRatioTapChanger);
    }

    public Stream<IpsoTwoWindingsTransformer> getConnectedAndRegulatingPhaseTapChangerTransformer() {
        return getIpsoTwoWindingsTransformers().stream()
                .filter(IpsoTwoWindingsTransformer::isConnectedOnBothSides)
                .filter(IpsoTwoWindingsTransformer::isRegulating)
                .filter(IpsoTwoWindingsTransformer::isPhaseTapChanger);
    }

    public MappingBetweenIidmIdAndIpsoEquipment getMappingBetweenIidmIdAndIpsoEquipment() {
        return mappingBetweenIidmIdAndIpsoEquipment;
    }


    public IpsoNetworkState getInterconnectedVersion(List<TopologicalAction> topologicalActions) {
        IpsoNetworkState result = new IpsoNetworkState(this.world,
                this.caseName,
                new MappingBetweenIidmIdAndIpsoEquipment(
                        this.mappingBetweenIidmIdAndIpsoEquipment.getIidmId2IpsoEquipment()
                ));
        ArrayList<IpsoNode> roots = new ArrayList<IpsoNode>(
                this.getIpsoNodes().stream()
                .filter(IpsoNode::isSlackBus)
                .collect(toList()));

        for(IpsoNode root:roots) {
            graphSearchFrom(root, result, topologicalActions);
        }

        return result;
    }

    private void graphSearchFrom(IpsoNode root, IpsoNetworkState result, List<TopologicalAction> topologicalActions) {

        Stack<IpsoNode> frontier = new Stack<>();
        frontier.push(root);

        try {
            Method[][] branchGetters = {
                    {IpsoNetworkState.class.getMethod("getIpsoLines"), IpsoNetworkState.class.getMethod("addLine", IpsoLine.class)},
                    {IpsoNetworkState.class.getMethod("getIpsoCouplings"), IpsoNetworkState.class.getMethod("addCoupling", IpsoCoupling.class)},
                    {IpsoNetworkState.class.getMethod("getIpsoTwoWindingsTransformers"), IpsoNetworkState.class.getMethod("addTwoWindingsTransformer", IpsoTwoWindingsTransformer.class)},
            };
            Method[][] oneConnectionElementGetters = {
                    {IpsoNetworkState.class.getMethod("getIpsoGenerators"), IpsoNetworkState.class.getMethod("addGenerator", IpsoGenerator.class)},
                    {IpsoNetworkState.class.getMethod("getIpsoLoads"), IpsoNetworkState.class.getMethod("addLoad", IpsoLoad.class)},
                    {IpsoNetworkState.class.getMethod("getIpsoBanks"), IpsoNetworkState.class.getMethod("addBank", IpsoBank.class)},
            };


            while (!frontier.isEmpty()) {

                IpsoNode currentNode = frontier.pop();

                if (!result.getIpsoNodes().contains(currentNode)) {
                    result.addNode(currentNode);

                    for (Method[] element : branchGetters) {
                        List<AbstractIpsoBranch> thisElementList =  (List<AbstractIpsoBranch>)element[0].invoke(this, null);
                        List<AbstractIpsoBranch> linkedElements = thisElementList.stream()
                                .filter(isLinked(currentNode))
                                .collect(Collectors.toList());

                        List<AbstractIpsoBranch> resultElementList =  (List<AbstractIpsoBranch>)element[0].invoke(result, null);

                        for (AbstractIpsoBranch branch : linkedElements) {
                            if(branch.couldBeConnected(topologicalActions) && !resultElementList.contains(branch)) {
                                element[1].invoke(result, branch);
                                if (branch.getIpsoNode1().equals(currentNode)) {
                                    frontier.push(branch.getIpsoNode2());
                                } else {
                                    frontier.push(branch.getIpsoNode1());
                                }
                            }

                        }

                    }

                    for (Method[] element : oneConnectionElementGetters) {

                        List<IpsoOneConnectionEquipment> thisElementList =  (List<IpsoOneConnectionEquipment>)element[0].invoke(this, null);
                        thisElementList.stream()
                                .filter(isConnected(currentNode))
                                .forEach(equipment -> {
                                    try {
                                        if(equipment.couldBeConnected(topologicalActions)) {
                                            element[1].invoke(result, equipment);
                                        }
                                    }
                                    catch (InvocationTargetException e) {e.printStackTrace();}
                                    catch (IllegalAccessException e) {e.printStackTrace();}
                                    }
                                );
                    }
                }
            }
        } catch(NoSuchMethodException | InvocationTargetException | IllegalAccessException e){
            e.printStackTrace();
        }
    }


    private Predicate<? super AbstractIpsoBranch> isLinked(IpsoNode node) {
        return branch -> branch.getIpsoNode1() == node || branch.getIpsoNode2() == node;
    }

    private Predicate<? super IpsoOneConnectionEquipment> isConnected(IpsoNode node) {
        return ipsoOneConnectionEquipment -> ipsoOneConnectionEquipment.getConnectedNode() == node;
    }

}
