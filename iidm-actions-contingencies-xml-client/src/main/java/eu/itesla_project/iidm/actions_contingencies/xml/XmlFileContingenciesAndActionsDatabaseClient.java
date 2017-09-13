/**
 * Copyright (c) 2016, All partners of the iTesla project (http://www.itesla-project.eu/consortium)
 * Copyright (c) 2016-2017, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package eu.itesla_project.iidm.actions_contingencies.xml;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;

import javax.xml.XMLConstants;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

import eu.itesla_project.contingency.ContingencyElement;
import eu.itesla_project.contingency.ContingencyImpl;
import eu.itesla_project.contingency.GeneratorContingency;
import eu.itesla_project.contingency.BranchContingency;
import eu.itesla_project.iidm.actions_contingencies.xml.mapping.Action;
import eu.itesla_project.iidm.actions_contingencies.xml.mapping.ActionCtgAssociations;
import eu.itesla_project.iidm.actions_contingencies.xml.mapping.ActionPlan;
import eu.itesla_project.iidm.actions_contingencies.xml.mapping.ActionsContingencies;
import eu.itesla_project.iidm.actions_contingencies.xml.mapping.And;
import eu.itesla_project.iidm.actions_contingencies.xml.mapping.Association;
import eu.itesla_project.iidm.actions_contingencies.xml.mapping.Constraint;
import eu.itesla_project.iidm.actions_contingencies.xml.mapping.Contingency;
import eu.itesla_project.iidm.actions_contingencies.xml.mapping.ElementaryAction;
import eu.itesla_project.iidm.actions_contingencies.xml.mapping.Equipment;
import eu.itesla_project.iidm.actions_contingencies.xml.mapping.GenerationOperation;
import eu.itesla_project.iidm.actions_contingencies.xml.mapping.LineOperation;
import eu.itesla_project.iidm.actions_contingencies.xml.mapping.LogicalExpression;
import eu.itesla_project.iidm.actions_contingencies.xml.mapping.Operand;
import eu.itesla_project.iidm.actions_contingencies.xml.mapping.Or;
import eu.itesla_project.iidm.actions_contingencies.xml.mapping.Parameter;
import eu.itesla_project.iidm.actions_contingencies.xml.mapping.PstOperation;
import eu.itesla_project.iidm.actions_contingencies.xml.mapping.Redispatching;
import eu.itesla_project.iidm.actions_contingencies.xml.mapping.SwitchOperation;
import eu.itesla_project.iidm.actions_contingencies.xml.mapping.Then;
import eu.itesla_project.iidm.actions_contingencies.xml.mapping.VoltageLevel;
import eu.itesla_project.iidm.actions_contingencies.xml.mapping.Zone;
import eu.itesla_project.iidm.actions_contingencies.xml.mapping.Zones;
import eu.itesla_project.iidm.network.Line;
import eu.itesla_project.iidm.network.Network;
import eu.itesla_project.iidm.network.Switch;
import eu.itesla_project.iidm.network.TieLine;
import eu.itesla_project.modules.contingencies.ActionElement;
import eu.itesla_project.modules.contingencies.ActionPlanOption;
import eu.itesla_project.modules.contingencies.ActionsContingenciesAssociation;
import eu.itesla_project.modules.contingencies.BinaryOperator;
import eu.itesla_project.modules.contingencies.ConstraintType;
import eu.itesla_project.modules.contingencies.ContingenciesAndActionsDatabaseClient;
import eu.itesla_project.modules.contingencies.GenerationRedispatching;
import eu.itesla_project.modules.contingencies.GeneratorStartAction;
import eu.itesla_project.modules.contingencies.GeneratorStopAction;
import eu.itesla_project.modules.contingencies.LineTrippingAction;
import eu.itesla_project.modules.contingencies.OperatorType;
import eu.itesla_project.modules.contingencies.Scenario;
import eu.itesla_project.modules.contingencies.ShuntAction;
import eu.itesla_project.modules.contingencies.SwitchClosingAction;
import eu.itesla_project.modules.contingencies.SwitchOpeningAction;
import eu.itesla_project.modules.contingencies.TapChangeAction;
import eu.itesla_project.modules.contingencies.TransformerOpeningAction;
import eu.itesla_project.modules.contingencies.UnaryOperator;
import eu.itesla_project.modules.contingencies.impl.ActionImpl;
import eu.itesla_project.modules.contingencies.impl.ActionPlanImpl;
import eu.itesla_project.modules.contingencies.impl.ActionsContingenciesAssociationImpl;
import eu.itesla_project.modules.contingencies.impl.ConstraintImpl;
import eu.itesla_project.modules.contingencies.impl.LogicalExpressionImpl;
import eu.itesla_project.modules.contingencies.impl.OptionImpl;
import eu.itesla_project.modules.contingencies.impl.VoltageLevelImpl;
import eu.itesla_project.modules.contingencies.impl.ZoneImpl;

/**
 *
 * @author Quinary <itesla@quinary.com>
 */
public class XmlFileContingenciesAndActionsDatabaseClient implements ContingenciesAndActionsDatabaseClient {

    private static final Logger LOGGER = LoggerFactory.getLogger(XmlFileContingenciesAndActionsDatabaseClient.class);

    private ActionsContingencies actionContingencies;
    private Map<Number, String> zonesMapping = new HashMap<Number, String>();

    public XmlFileContingenciesAndActionsDatabaseClient(Path file) throws JAXBException, SAXException, IOException {
        try (InputStream stream = Files.newInputStream(file)) {
            load(stream);
        }
    }

    public XmlFileContingenciesAndActionsDatabaseClient(URL url) throws JAXBException, SAXException, IOException {
        try (InputStream stream = url.openStream()) {
            load(stream);
        }
    }

    private void load(InputStream stream) throws JAXBException, SAXException, IOException {
        JAXBContext jaxbContext = JAXBContext.newInstance(ActionsContingencies.class);
        Unmarshaller jaxbMarshaller = jaxbContext.createUnmarshaller();

        SchemaFactory sf = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
        URL res = XmlFileContingenciesAndActionsDatabaseClient.class.getClassLoader().getResource("xsd/actions.xsd");
        if (res == null) {
            throw new IOException("Unable to find schema");
        }
        Schema schema = sf.newSchema(res);
        jaxbMarshaller.setSchema(schema);

        actionContingencies = (ActionsContingencies) jaxbMarshaller.unmarshal(stream);
    }

    @Override
    public List<Scenario> getScenarios() {
        return Collections.emptyList();
    }

    /** 
     * @param action's ID, Network
     * @return eu.itesla_project.modules.contingencies.Action 
     * */
    public eu.itesla_project.modules.contingencies.Action getAction(String id, Network network) {
        Objects.requireNonNull(id, "action id is null");
        Objects.requireNonNull(network, "netiwork id is null");
        LOGGER.info("Getting {} action for network {}", id, network.getId());
        if (zonesMapping.isEmpty()) {
            getZones();
        }
        if (id != null) {
            List<ElementaryAction> elactions = actionContingencies.getElementaryActions().getElementaryAction();
            for (ElementaryAction ele : elactions) {
                if (ele.getName().equals(id)) {
                    List<ActionElement> elements = getActionElements(ele, network);

                    List<String> zones = new ArrayList<String>();
                    Zones eleZones = ele.getZones();
                    if (eleZones != null) {
                        for (BigInteger z : eleZones.getNum()) {
                            zones.add(zonesMapping.get(z));
                        }
                    }
                    if (elements.size() > 0) {
                        LOGGER.info("action {} for network {} found", id, network.getId());
                        return new ActionImpl(id, ele.isPreventiveType(), ele.isCurativeType(), elements, zones, ele.getStartTime());
                    } else {
                        LOGGER.info("action {} for network {} not found", id, network.getId());
                        return null;
                    }
                }

            }
        }        
        return null;
    }

    /** 
     * @param  Network
     * @return List<eu.itesla_project.modules.contingencies.Action>
     * */
    public List<eu.itesla_project.modules.contingencies.Action> getActions(Network network) {
        Objects.requireNonNull(network, "network is null");
        LOGGER.info("Getting actions for network {}", network.getId());
        if (zonesMapping.isEmpty()) {
            getZones();
        }

        List<eu.itesla_project.modules.contingencies.Action> actions = new ArrayList<>();

        try {
            List<ElementaryAction> elactions = actionContingencies.getElementaryActions().getElementaryAction();

            for (ElementaryAction ele : elactions) {

                String name = ele.getName();
                List<ActionElement> elements = getActionElements(ele, network);

                List<String> zones = new ArrayList<String>(); 
                Zones eleZones = ele.getZones();
                if (eleZones != null) {
                    for (BigInteger z : eleZones.getNum()) {
                        zones.add(zonesMapping.get(z));
                    }
                }
                if ( elements.size() > 0 ) {
                    LOGGER.info("Adding {} action to list for network {}", name, network.getId());
                    actions.add(new ActionImpl(name, ele.isPreventiveType(), ele.isCurativeType(), elements, zones, ele.getStartTime()));
                }

            }

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        LOGGER.info("Found {} actions for network {}", actions.size(), network.getId());
        return actions;
    }

    /** 
     * @param Network
     * @return List<eu.itesla_project.contingency.Contingency>
     * */
    @Override
    public List<eu.itesla_project.contingency.Contingency> getContingencies(Network network) {
        Objects.requireNonNull(network, "network is null");
        LOGGER.info("Getting contingencies for network {}", network.getId());
        if ( zonesMapping.isEmpty() ) {
            getZones();
        }
        List<eu.itesla_project.contingency.Contingency> contingencies = new ArrayList<>();

        try {
            // pre-index tie lines
            Map<String, String> tieLines = new HashMap<>();
            for (Line l : network.getLines()) {
                if (l.isTieLine()) {
                    TieLine tl = (TieLine) l;
                    tieLines.put(tl.getHalf1().getId(), tl.getId());
                    tieLines.put(tl.getHalf2().getId(), tl.getId());
                }
            }
            for (Contingency cont : actionContingencies.getContingencies().getContingency()) {
                String contingency = cont.getName();
                LOGGER.info("contingency: {}", contingency);
                List<ContingencyElement> elements = new ArrayList<>();
                for (Equipment eq : cont.getEquipments().getEquipment()) {
                    String id = eq.getId();
                    if (network.getLine(id) != null) {
                        LOGGER.info("contingency: {} - element BranchContingency, id: {}", contingency, id);
                        elements.add(new BranchContingency(id));
                    } else if (network.getGenerator(id) != null) {
                        LOGGER.info("contingency: {} - element GeneratorContingency, id: {}", contingency, id);
                        elements.add(new GeneratorContingency(id));
                    } else if (tieLines.containsKey(id)) {
                        LOGGER.info("contingency: {} - element BranchContingency, tieLines id: {}", contingency, tieLines.get(id));
                        elements.add(new BranchContingency(tieLines.get(id)));
                    } else {
                        LOGGER.warn("Contingency element '{}' of contingency {} not found in network {}, skipping it", id, contingency, network.getId());
                    }
                }
                List<String> zones = new ArrayList<String>();
                Zones contZones = cont.getZones();
                if ( contZones != null ) {
                    for ( BigInteger z: contZones.getNum()) {
                        zones.add(zonesMapping.get(z));
                    }
                }
                if (elements.size() > 0) {
                    contingencies.add(new ContingencyImpl(contingency, elements));
                }
            }

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        LOGGER.info("Found {} contingencies for network {}", contingencies.size(), network.getId());
        return contingencies;

    }

    /** 
     * @param Contingency's name, Network
     * @return List<eu.itesla_project.contingency.Contingency>
     * */
    public eu.itesla_project.contingency.Contingency getContingency(String name, Network network) {
        Objects.requireNonNull(name, "contingency id is null");
        Objects.requireNonNull(network, "network is null");
        LOGGER.info("Getting contingency {} for network {}", name, network);
        if (name != null) {
            for (eu.itesla_project.contingency.Contingency c : getContingencies(network)) {
                if (c.getId().equals(name)) {
                    return c;
                }
            }

        }
        return null;
    }

    @Override
    public Set<eu.itesla_project.modules.contingencies.Zone> getZones() {
        LOGGER.info("Getting zones");
        Set<eu.itesla_project.modules.contingencies.Zone> res = new HashSet<eu.itesla_project.modules.contingencies.Zone>();
        Zones zones    =  actionContingencies.getZones();
        if (zones != null) {
            for (Zone z : zones.getZone()) {
                List<eu.itesla_project.modules.contingencies.VoltageLevel> vls = new ArrayList<eu.itesla_project.modules.contingencies.VoltageLevel>();
                for (VoltageLevel vl : z.getVoltageLevels().getVoltageLevel()) {
                    vls.add(new VoltageLevelImpl(vl.getID(), vl.getLevel()));
                }
                eu.itesla_project.modules.contingencies.Zone zone = new ZoneImpl(z.getName(), z.getNumber(), vls, z.getDescription());
                res.add(zone);
                zonesMapping.put(zone.getNumber(), zone.getName());
            }
        }
        LOGGER.info("Found {} zones", res.size());
        return res;
    }

    @Override
    /**
     * @param zone Name
     */
    public eu.itesla_project.modules.contingencies.Zone getZone(String id) {
        Objects.requireNonNull(id, "zone id is null");
        LOGGER.info("Getting zone {} ", id);
        Zone z = XmlActionsContingenciesUtils.getZone(actionContingencies, id);

        if (z == null) {
            LOGGER.warn("Zones element '{}' with id " + id + " not found");
            return null;
        } else {
            List<eu.itesla_project.modules.contingencies.VoltageLevel> vls = new ArrayList<eu.itesla_project.modules.contingencies.VoltageLevel>();
            for (VoltageLevel vl : z.getVoltageLevels().getVoltageLevel()) {
                vls.add(new VoltageLevelImpl(vl.getID(), vl.getLevel()));
            }

            return new ZoneImpl(z.getName(), z.getNumber(), vls, z.getDescription());
        }

    }

    // it returns the zone containing at least a voltage level of the network, linking to the zone only the voltage levels of the network 
    @Override
    public Set<eu.itesla_project.modules.contingencies.Zone> getZones(Network network) {
        Objects.requireNonNull(network, "network is null");
        LOGGER.info("Getting zones for network {}", network.getId());
        Set<eu.itesla_project.modules.contingencies.Zone> res = new HashSet<eu.itesla_project.modules.contingencies.Zone>();
        Zones zones = actionContingencies.getZones();
        if (zones != null) {
            for (Zone z : zones.getZone()) {
                List<eu.itesla_project.modules.contingencies.VoltageLevel> vls = new ArrayList<eu.itesla_project.modules.contingencies.VoltageLevel>();
                for (VoltageLevel vl : z.getVoltageLevels().getVoltageLevel()) {
                    if (network.getVoltageLevel(vl.getID()) != null) {
                        vls.add(new VoltageLevelImpl(vl.getID(), vl.getLevel()));
                    } else {
                        LOGGER.warn("Voltage level {} of zone {} does not belong to network {}, skipping it", vl.getID(), z.getName(), network.getId());
                    }
                }
                if (vls.size() > 0) {
                    eu.itesla_project.modules.contingencies.Zone zone = new ZoneImpl(z.getName(), z.getNumber(), vls, z.getDescription());
                    res.add(zone);
                }
            }
        }
        LOGGER.info("Found {} zones for network {}", res.size(), network.getId());
        return res;
    }

    @Override
    public Set<eu.itesla_project.modules.contingencies.ActionPlan> getActionPlans( Network network) {
        Objects.requireNonNull(network, "network is null");
        LOGGER.info("Getting action plans for network {}", network.getId());
        Set<eu.itesla_project.modules.contingencies.ActionPlan> netActionPlans = new HashSet<eu.itesla_project.modules.contingencies.ActionPlan>();
        List<eu.itesla_project.modules.contingencies.ActionPlan> actionPlans = this.getActionPlans();

        if (actionPlans == null) {
            LOGGER.warn("ActionPlans elements not found");
            return null;
        }

        List<eu.itesla_project.modules.contingencies.Action> netActions = this.getActions(network);
        List<String> actionsId = new ArrayList<String>();
        for (eu.itesla_project.modules.contingencies.Action netAct : netActions) {
            actionsId.add(netAct.getId());
        }

        for (eu.itesla_project.modules.contingencies.ActionPlan ap : actionPlans) {

            for (Map.Entry<BigInteger, ActionPlanOption> entryOpt : ap.getPriorityOption().entrySet()) {

                for (Map.Entry<BigInteger, String> entryAct : entryOpt.getValue().getActions().entrySet()) {
                    String actId = entryAct.getValue();
                    if (actionsId.size() > 0 && actionsId.contains(actId)) {
                        netActionPlans.add(ap);
                        break;
                    }
                }

            }
        }
        LOGGER.info("Found {} action plans for network {}", netActionPlans.size(), network.getId());
        return netActionPlans;
    }

    /**
     * return Action Plan by name
     */
    public eu.itesla_project.modules.contingencies.ActionPlan getActionPlan(String id) {
        Objects.requireNonNull(id, "action plan id is null");
        LOGGER.info("Getting {} action plan", id);
        if (zonesMapping.isEmpty()) {
            getZones();
        }

        if (id != null && actionContingencies.getActionPlans() != null) {
            List<ActionPlan> actPlans = actionContingencies.getActionPlans().getActionPlan();

            for (ActionPlan plan : actPlans) {
                if (plan.getName().equals(id)) {

                    Map<BigInteger, ActionPlanOption> priorityOptions = new TreeMap<BigInteger, ActionPlanOption>();

                    for (eu.itesla_project.iidm.actions_contingencies.xml.mapping.Option op : plan.getOption()) {
                        Map<BigInteger, String> actionMap = new TreeMap<BigInteger, String>();
                        for (eu.itesla_project.iidm.actions_contingencies.xml.mapping.Action ac : op.getAction()) {
                            actionMap.put(ac.getNum(), ac.getId());
                        }


                        OptionImpl opImpl = new OptionImpl(op.getPriority(), convertExpression(op.getLogicalExpression(), actionMap), actionMap);
                        priorityOptions.put(op.getPriority(), opImpl);
                    }

                    List<String> zonesName = new ArrayList<String>();
                    Zones planZones = plan.getZones();
                    if ( planZones != null ) {
                        for ( BigInteger z: planZones.getNum()) {
                            zonesName.add(zonesMapping.get(z));
                        }
                    }
                    LOGGER.info("{} action plan found", id);
                    return new ActionPlanImpl(plan.getName(), plan.getDescription().getInfo(), zonesName, priorityOptions);
                }
            }
        }
        return null;
    }

    /*
     * 
     * @return all action plans defined into xml
     */
    @Override
    public List<eu.itesla_project.modules.contingencies.ActionPlan> getActionPlans() {
        LOGGER.info("Getting action plans");
        if (zonesMapping.isEmpty()) {
            getZones();
        }

        List<eu.itesla_project.modules.contingencies.ActionPlan> actPlanList = new ArrayList<eu.itesla_project.modules.contingencies.ActionPlan>();
        List<ActionPlan> actPlans = actionContingencies.getActionPlans().getActionPlan();
        if (actPlans == null) {
            LOGGER.warn("ActionPlans elements not found");
            return null;
        } else {
            for (ActionPlan plan : actPlans) {
                Map<BigInteger, ActionPlanOption> priorityOptions = new TreeMap<BigInteger, ActionPlanOption>();

                for (eu.itesla_project.iidm.actions_contingencies.xml.mapping.Option op : plan.getOption()) {
                    Map<BigInteger, String> sequenceActions = new TreeMap<BigInteger, String>();
                    for (eu.itesla_project.iidm.actions_contingencies.xml.mapping.Action ac : op.getAction()) {
                        sequenceActions.put(ac.getNum(), ac.getId());
                    }

                    LogicalExpression exp = op.getLogicalExpression();

                    eu.itesla_project.modules.contingencies.LogicalExpression le = convertExpression(exp, sequenceActions);

                    OptionImpl opImpl = new OptionImpl(op.getPriority(), le, sequenceActions);
                    priorityOptions.put(op.getPriority(), opImpl);
                }

                List<String> zonesName = new ArrayList<String>();
                Zones planZones = plan.getZones();
                if (planZones != null) {
                    for (BigInteger z : planZones.getNum()) {
                        zonesName.add(zonesMapping.get(z));
                    }
                }
                actPlanList.add(new ActionPlanImpl(plan.getName(), plan.getDescription().getInfo(), zonesName, priorityOptions));

            }
        }
        LOGGER.info("Found {} action plans", actPlanList.size());
        return actPlanList;
    }

    private eu.itesla_project.modules.contingencies.LogicalExpression convertExpression(
            LogicalExpression exp, Map<BigInteger, String> sequenceActions) {
        LogicalExpressionImpl le = new LogicalExpressionImpl();

        if (exp.getAnd() != null) {
            if (exp.getAnd().getOperand().size() == 2) {
                le.setOperator(new BinaryOperator(OperatorType.AND, toOperator(exp.getAnd().getOperand().get(0), sequenceActions), toOperator(exp.getAnd().getOperand().get(1), sequenceActions)));
            } else {
                throw new RuntimeException("Operand mismatch");
            }

        } else if (exp.getOr() != null) {
            if (exp.getOr().getOperand().size() == 2) {
                le.setOperator(new BinaryOperator(OperatorType.OR, toOperator(exp.getOr().getOperand().get(0), sequenceActions), toOperator(exp.getOr().getOperand().get(1), sequenceActions)));
            } else {
                throw new RuntimeException("Operand mismatch");
            }
        } else if (exp.getThen() != null) {
            if (exp.getThen().getOperand().size() == 2) {
                le.setOperator(new BinaryOperator(OperatorType.THEN, toOperator(exp.getThen().getOperand().get(0), sequenceActions), toOperator(exp.getThen().getOperand().get(1), sequenceActions)));
            } else {
                throw new RuntimeException("Operand mismatch");
            }
        } else if (exp.getOperand() != null) {
            le.setOperator(toOperator(exp.getOperand(), sequenceActions));
        }

        return le;
    }

    private List<Object> getFilteredContent(Operand op) {

        if (op.getContent().size() > 1) {
            ArrayList<Object> filtered = new ArrayList<Object>();
            for (Object o : op.getContent()) {
                if (o instanceof String) {
                    continue;
                }
                filtered.add(o);
            }
            return filtered;
        } else {
            return op.getContent();
        }
    }

    private boolean isConstant(Operand op) {
        return (getFilteredContent(op).size() == 1 && getFilteredContent(op).get(0) instanceof String);

    }

    private eu.itesla_project.modules.contingencies.Operator toOperator(
            Operand op, Map<BigInteger, String> sequenceActions) {
        List<Object> content = getFilteredContent(op);

        if (isConstant(op)) {
            String v = sequenceActions.get(new BigInteger((String) content.get(0)));
            return new UnaryOperator(v);
        }

        for (Object o : content) {
            if (o instanceof String) {
                throw new RuntimeException("Operand mismatch: " + o);
            } else if (o instanceof And) {
                And aa = (And) o;
                if (aa.getOperand().size() == 2) {
                    return new BinaryOperator(OperatorType.AND, toOperator(aa.getOperand().get(0), sequenceActions), toOperator(aa.getOperand().get(1), sequenceActions));
                } else {
                    throw new RuntimeException("Operand mismatch");
                }
            } else if (o instanceof Or) {
                Or aa = (Or) o;

                if (aa.getOperand().size() == 2) {
                    return new BinaryOperator(OperatorType.OR, toOperator(aa.getOperand().get(0), sequenceActions), toOperator(aa.getOperand().get(1), sequenceActions));
                } else {
                    throw new RuntimeException("Operand mismatch");
                }

            } else if (o instanceof Then) {
                Then aa = (Then) o;

                if (aa.getOperand().size() == 2) {
                    return new BinaryOperator(OperatorType.THEN, toOperator(aa.getOperand().get(0), sequenceActions), toOperator(aa.getOperand().get(1), sequenceActions));
                } else {
                    throw new RuntimeException("Operand mismatch");
                }
            }
        }
        return null;
    }

    @Override
    public Collection<eu.itesla_project.modules.contingencies.ActionsContingenciesAssociation> getActionsCtgAssociations() {
        LOGGER.info("Getting actions/contigencies associations");
        List<eu.itesla_project.modules.contingencies.ActionsContingenciesAssociation> associationList = new ArrayList<eu.itesla_project.modules.contingencies.ActionsContingenciesAssociation>();
        ActionCtgAssociations xmlActionContAssociation = actionContingencies.getActionCtgAssociations();
        List<Association> xmlAssociations = xmlActionContAssociation.getAssociation();
        if (xmlAssociations == null) {
            LOGGER.warn("Action Contingencies associations not found");
            return associationList;
        } else {


            for (Association association : xmlAssociations) {
                List<Contingency> xmlContingencies = association.getContingency();
                List<Constraint> xmlConstraints = association.getConstraint();
                List<Action> xmlActions = association.getAction();

                List<String> ctgIds = new ArrayList<String>();
                for (Contingency c : xmlContingencies) {
                    ctgIds.add(c.getId());
                }

                List<eu.itesla_project.modules.contingencies.Constraint> constraints = new ArrayList<eu.itesla_project.modules.contingencies.Constraint>();
                for (Constraint c : xmlConstraints) {

                    constraints.add(new ConstraintImpl(c.getEquipment(), c.getValue(), XmlActionsContingenciesUtils.getConstraintType(c.getType())));
                }

                List<String> actionIds = new ArrayList<String>();
                for (Action a : xmlActions) {
                    actionIds.add(a.getId());

                }

                associationList.add(new ActionsContingenciesAssociationImpl(ctgIds, constraints, actionIds));

            }

        }
        LOGGER.info("Found {} actions/contigencies associations", associationList.size());
        return associationList;
    }

    @Override
    public Collection<eu.itesla_project.modules.contingencies.ActionsContingenciesAssociation> getActionsCtgAssociationsByContingency(String contingencyId) {
        Objects.requireNonNull(contingencyId, "contingency id is null");
        LOGGER.info("Getting actions/contingencies associations for contingency {}", contingencyId);
        List<eu.itesla_project.modules.contingencies.ActionsContingenciesAssociation> associationList = new ArrayList<eu.itesla_project.modules.contingencies.ActionsContingenciesAssociation>();
        ActionCtgAssociations xmlActionContAssociation = actionContingencies.getActionCtgAssociations();
        List<Association> xmlAssociations = xmlActionContAssociation.getAssociation();
        if (xmlAssociations == null) {
            LOGGER.warn("Actions Contingencies associations not found");
            return associationList;
        } else {
            for (Association association : xmlAssociations) {
                List<Contingency>     xmlContingencies    = association.getContingency();
                List<String> contingenciesIds = xmlContingencies.stream().map(Contingency::getId).collect(Collectors.toList());

                if (contingenciesIds.contains(contingencyId)) {
                    List<Constraint> xmlConstraints = association.getConstraint();
                    List<Action> xmlActions = association.getAction();

                    List<String> ctgIds = new ArrayList<String>();
                    for (Contingency c : xmlContingencies) {
                        ctgIds.add(c.getId());
                    }

                    List<eu.itesla_project.modules.contingencies.Constraint> constraints = new ArrayList<eu.itesla_project.modules.contingencies.Constraint>();
                    for (Constraint c : xmlConstraints) {
                        constraints.add(new ConstraintImpl(c.getEquipment(), c.getValue(), XmlActionsContingenciesUtils.getConstraintType(c.getType())));
                    }

                    List<String> actionIds = new ArrayList<String>();
                    for (Action a : xmlActions) {
                        actionIds.add(a.getId());

                    }

                    associationList.add(new ActionsContingenciesAssociationImpl(ctgIds, constraints, actionIds) );
                }
            }
        }
        LOGGER.info("Found {} actions/contingencies associations for contingency {}", associationList.size(), contingencyId);
        return associationList;
    }

    /** 
     * @param contingencyId
     * @return List<String> action 
     * 
     */
    public List<String> getActionsByContingency(String contingencyId) {
        Objects.requireNonNull(contingencyId, "contingency id is null");
        LOGGER.info("Getting actions for contingency {}", contingencyId);
        List<String> actions = new ArrayList<String>();

        ActionCtgAssociations actCont = actionContingencies.getActionCtgAssociations();

        List<Association> associations = actCont.getAssociation();
        for (Association association : associations) {
            List<Contingency> contingencies = association.getContingency();
            for (Contingency c : contingencies) {
                if (c.getId().equals(contingencyId)) {
                    List<Action> acs = association.getAction();
                    for (Action a : acs) {
                        actions.add(a.getId());
                    }
                }
            }
        }
        LOGGER.info("Found {} actions for contingency {}", actions.size(), contingencyId);
        return actions;
    }

    /** 
     * @param all network association 
     * @return List<Association> 
     * 
     */
    @Override
    public List<eu.itesla_project.modules.contingencies.ActionsContingenciesAssociation> getActionsCtgAssociations(Network network) {
        Objects.requireNonNull(network, "network is null");
        LOGGER.info("Getting actions/contingencies associations for network {}", network.getId());
        List<eu.itesla_project.modules.contingencies.ActionsContingenciesAssociation> associationList = new ArrayList<eu.itesla_project.modules.contingencies.ActionsContingenciesAssociation>();
        ActionCtgAssociations xmlActionContAssociation = actionContingencies.getActionCtgAssociations();
        List<Association> xmlAssociations = xmlActionContAssociation.getAssociation();
        if (xmlAssociations == null) {
            LOGGER.warn(" Action Contingencies associations not found");
            return null;
        } else {

            List<eu.itesla_project.modules.contingencies.Action> networkActions = getActions(network);
            List<eu.itesla_project.modules.contingencies.ActionPlan> networkActionPlans = new ArrayList<>(getActionPlans(network));
            // pre-index tie lines
            Map<String, String> tieLines = new HashMap<>();
            for (Line l : network.getLines()) {
                if (l.isTieLine()) {
                    TieLine tl = (TieLine) l;
                    tieLines.put(tl.getHalf1().getId(), tl.getId());
                    tieLines.put(tl.getHalf2().getId(), tl.getId());
                }
            }
            for (Association association : xmlAssociations) {
                List<Contingency>    xmlContingencies    = association.getContingency();
                List<Constraint>     xmlConstraints      = association.getConstraint();
                List<Action>         xmlActions          = association.getAction();

                List<String> ctgIds = new ArrayList<String>();
                for (Contingency c: xmlContingencies) {
                    boolean found = false;
                    for (Contingency ctg : actionContingencies.getContingencies().getContingency()) {
                        if (ctg.getName().equals(c.getId())) {
                            found = true;
                            if (ctg.getEquipments() != null) {
                                for ( Equipment eq:ctg.getEquipments().getEquipment()) {
                                    if (network.getIdentifiable(eq.getId()) != null) {
                                        ctgIds.add(c.getId());
                                        break;
                                    } else if (tieLines.containsKey(eq.getId())) {
                                        ctgIds.add(c.getId());
                                        break;
                                    } else {
                                        LOGGER.warn("Equipment {} referred in contingency (in association) does not belong to network {}, skipping it", eq.getId(), network.getId());
                                    }
                                }
                            }
                            break;
                        }
                    }
                    if (!found) {
                        LOGGER.warn("Contingency {} referred in actions/contingencies associations not in the DB: skipping it", c.getId());
                    }
                }

                List<eu.itesla_project.modules.contingencies.Constraint> constraints = new ArrayList<eu.itesla_project.modules.contingencies.Constraint>();
                for (Constraint con: xmlConstraints) {
                    if (network.getIdentifiable(con.getEquipment()) != null) {
                        constraints.add(new ConstraintImpl(con.getEquipment(), con.getValue(), XmlActionsContingenciesUtils.getConstraintType(con.getType())));
                    } else if (tieLines.containsKey(con.getEquipment())) {
                        constraints.add(new ConstraintImpl(tieLines.get(con.getEquipment()), con.getValue(), XmlActionsContingenciesUtils.getConstraintType(con.getType())));
                    } else {
                        LOGGER.warn("Equipment {} referred in constraints does not belong to network {}, skipping it", con.getEquipment(), network.getId());
                    }
                }

                List<String> actionIds = new ArrayList<String>();
                for (Action a: xmlActions) {
                    boolean found = false;
                    for (eu.itesla_project.modules.contingencies.Action action : networkActions) {
                        if ( action.getId().equals(a.getId())) {
                            found = true;
                            actionIds.add(a.getId());
                            break;
                        }
                    }
                    for (eu.itesla_project.modules.contingencies.ActionPlan actionPlan : networkActionPlans) {
                        if (actionPlan.getName().equals(a.getId())) {
                            found = true;
                            actionIds.add(a.getId());
                            break;
                        }
                    }
                    if (!found) {
                        LOGGER.warn("Action/Action Plan {} referred in actions/contingencies associations not in the DB: skipping it", a.getId());
                    }
                }

                associationList.add(new ActionsContingenciesAssociationImpl(ctgIds, constraints, actionIds) );

            }

        }
        LOGGER.info("Found {} actions/contingencies associations for network {}", associationList.size(), network.getId());
        return associationList;
    }

    private List<ActionElement> getActionElements(ElementaryAction ele,     Network network) {
        if (zonesMapping.isEmpty()) {
            getZones();
        }

        // pre-index tie lines
        Map<String, String> tieLines = new HashMap<>();
        for (Line l : network.getLines()) {
            if (l.isTieLine()) {
                TieLine tl = (TieLine) l;
                tieLines.put(tl.getHalf1().getId(), tl.getId());
                tieLines.put(tl.getHalf2().getId(), tl.getId());
            }
        }
        List<ActionElement> elements = new ArrayList<>();
        for (LineOperation lo : ele.getLineOperation()) {
            String lineId = lo.getId();
            if (network.getLine(lineId) != null) {
                elements.add(new LineTrippingAction(lineId, lo.getImplementationTime(), lo.getAchievmentIndex()));
            } else if (tieLines.containsKey(lineId)) {
                elements.add(new LineTrippingAction(tieLines.get(lineId), lo.getImplementationTime(), lo.getAchievmentIndex()));
            } else {
                LOGGER.warn("LineOperation : Line id not found: " + lineId);
            }
        }

        for (GenerationOperation go : ele.getGenerationOperation()) {
            String genId = go.getId();
            if (network.getGenerator(genId) != null) {
                if (go.getAction().equals("stop")
                        || go.getAction().equals("stopPumping")) {
                    elements.add(new GeneratorStopAction(genId, go.getImplementationTime(), go.getAchievmentIndex()));
                } else if (go.getAction().equals("start")
                        || go.getAction().equals("startPumping")) {
                    elements.add(new GeneratorStartAction(genId, go.getImplementationTime(), go.getAchievmentIndex()));
                }
            } else {
                LOGGER.warn("GenerationOperation : generator id not found: " + genId);
            }
        }

        for (SwitchOperation swOp : ele.getSwitchOperation()) {
            String switchId = swOp.getId();
            Switch sw = network.getSwitch(switchId);             
            if (sw != null) {
                if (swOp.getAction().equals("opening")) {
                    elements.add(new SwitchOpeningAction(sw.getVoltageLevel().getId(), switchId, swOp.getImplementationTime(), swOp.getAchievmentIndex()));
                } else if (swOp.getAction().equals("closing")) {
                    elements.add(new SwitchClosingAction(sw.getVoltageLevel().getId(), switchId, swOp.getImplementationTime(), swOp.getAchievmentIndex()));
                }
            } else {
                LOGGER.warn("SwitchOperation : switch id not found: " + switchId);
            }

        }

        for (PstOperation pst : ele.getPstOperation()) {
            String transformerId = pst.getId();
            if (network.getTwoWindingsTransformer(transformerId) != null) {
                if (pst.getAction().equals("shunt")) {
                    elements.add(new ShuntAction(pst.getId(), pst.getImplementationTime(), pst.getAchievmentIndex()));
                } else if (pst.getAction().equals("tapChange")) {
                    Parameter tapPositionParameter = getParameter(pst.getParameter(), "tapPosition");
                    if (tapPositionParameter != null) {
                        int tapPosition = Integer.parseInt(tapPositionParameter.getValue());
                        elements.add(new TapChangeAction(pst.getId(), tapPosition, pst.getImplementationTime(), pst.getAchievmentIndex()));
                    }
                } else if (pst.getAction().equals("opening")) {
                    Parameter substationParameter = getParameter(pst.getParameter(), "substation");
                    String substation = (substationParameter == null) ? null : substationParameter.getValue();
                    elements.add(new TransformerOpeningAction(pst.getId(), substation, pst.getImplementationTime(), pst.getAchievmentIndex()));
                } else {
                    LOGGER.warn("pst operation not supported : " + pst.getAction());
                }
            } else {
                LOGGER.warn("PstOperation : transformer id " + transformerId + " not found");
            }
        }

        for (Redispatching redispatching : ele.getRedispatching()) {
            elements.add(new GenerationRedispatching(redispatching.getGenerator(), redispatching.getAchievmentIndex(), redispatching.getImplementationTime()));
        }

        return elements;
    }

    @Override
    public Collection<ActionsContingenciesAssociation> getActionsCtgAssociationsByConstraint(
            String equipmentId, ConstraintType constraintType) {
        Objects.requireNonNull(equipmentId, "equipment id is null");
        Objects.requireNonNull(constraintType, "constraint type is null");
        LOGGER.info("Getting actions/contigencies association by {} constraint on equipment {}", constraintType, equipmentId);
        List<eu.itesla_project.modules.contingencies.ActionsContingenciesAssociation> associationList = new ArrayList<eu.itesla_project.modules.contingencies.ActionsContingenciesAssociation>();
        ActionCtgAssociations xmlActionContAssociation = actionContingencies.getActionCtgAssociations();
        List<Association> xmlAssociations = xmlActionContAssociation.getAssociation();
        if (xmlAssociations == null) {
            LOGGER.warn("Actions Contingencies associations not found");
            return associationList;
        } else {
            for (Association association : xmlAssociations) {
                List<Constraint> xmlConstraints = association.getConstraint();

                if (constraintOnEquipment(xmlConstraints, equipmentId, constraintType)) {
                    List<Contingency> xmlContingencies = association.getContingency();
                    List<Action> xmlActions = association.getAction();

                    List<String> ctgIds = new ArrayList<String>();
                    for (Contingency c : xmlContingencies) {
                        ctgIds.add(c.getId());
                    }

                    List<eu.itesla_project.modules.contingencies.Constraint> constraints = new ArrayList<eu.itesla_project.modules.contingencies.Constraint>();
                    for (Constraint c : xmlConstraints) {

                        constraints.add(new ConstraintImpl(c.getEquipment(), c.getValue(), XmlActionsContingenciesUtils.getConstraintType(c.getType())));
                    }

                    List<String> actionIds = new ArrayList<String>();
                    for (Action a : xmlActions) {
                        actionIds.add(a.getId());

                    }

                    associationList.add(new ActionsContingenciesAssociationImpl(ctgIds, constraints, actionIds));
                }
            }
        }

        LOGGER.info("Found {} actions/contigencies associations for {} constraint on equipment {}", associationList.size(), constraintType, equipmentId);
        return associationList;
    }

    private boolean constraintOnEquipment(List<Constraint> constraints, String equipmentId, ConstraintType constraintType) {
        for (Constraint constraint : constraints) {
            if (equipmentId.equals(constraint.getEquipment()) && constraintType.equals(XmlActionsContingenciesUtils.getConstraintType(constraint.getType()))) {
                return true;
            }
        }
        return false;
    }

    private static Parameter getParameter(List<Parameter> parameters, String name) {
        Objects.requireNonNull(parameters);
        Objects.requireNonNull(name);

        return parameters.stream().filter(p -> p.getName().equals(name)).findFirst().get();
    }
}
