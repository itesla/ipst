/** Copyright (c) 2016, Tractebel (http://www.tractebel-engie.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this file,
 * You can obtain one at http://mozilla.org/MPL/2.0/.
 **/
package eu.itesla_project.cta.service;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import eu.itesla_project.computation.ComputationManager;
import eu.itesla_project.cta.converter.MappingBetweenIidmIdAndIpsoEquipment;
import eu.itesla_project.cta.model.IpsoEquipment;
import eu.itesla_project.iidm.network.*;
import eu.itesla_project.modules.contingencies.Contingency;
import eu.itesla_project.modules.optimizer.CCOFinalStatus;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Collection;
import java.util.List;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.collect.Lists.newArrayList;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.lang3.SystemUtils.LINE_SEPARATOR;
/**
 * @author Yannick Pihan <yannick.pihan at tractebel.engie.com>
 */
public class IpsoOutputListing {

    public static final String DELIMITER = ",";
    protected static final String OUTOUT_LISTING_FILENAME = "ipsoOutputListing.txt";
    protected static final String DATE_FORMAT_NOW = "yyyy-MM-dd HH:mm:ss";

    private final MappingBetweenIidmIdAndIpsoEquipment mappingBetweenIidmIdAndIpsoEquipment;
    private List<String> listing;
    private String filename;

    private ComputationManager computationManager;

    /**
     * Constructor
     */
    public IpsoOutputListing(MappingBetweenIidmIdAndIpsoEquipment mappingBetweenIidmIdAndIpsoEquipment) {
        checkArgument(mappingBetweenIidmIdAndIpsoEquipment != null, "mappingBetweenIidmIdAndIpsoEquipment must not be null");
        this.mappingBetweenIidmIdAndIpsoEquipment = mappingBetweenIidmIdAndIpsoEquipment;
        listing = newArrayList();
        filename = OUTOUT_LISTING_FILENAME;
    }

    private String getIpsoId(String iidmId) {
        return mappingBetweenIidmIdAndIpsoEquipment.getIpsoEquipmentFor(iidmId)
                .map(IpsoEquipment::getId)
                .orElse("?");
    }

    private String formatLine(Object ... elements) {
        List<Object> lines = newArrayList(elements);
        return lines.stream()
                .map(Object::toString)
                .collect(joining(DELIMITER));
    }

    private String header() {
        return IpsoOutputListingMessageType.IPSO_OUTPUT_LISTING.getMessage();
    }

    private void addListHeader(int numberOfElement, String header) {
        listing.add("");
        listing.add(String.format("%s %s:", numberOfElement, header));
    }

    public <T> void addToListing(IpsoOutputListingMessageType messageType, Collection<T> components) {
        addListHeader(components.size(), messageType.getMessage());
        listing.addAll(
                components
                        .stream()
                        .map(T::toString)
                        .collect(toList())
        );
    }

    public void addToListing(IpsoOutputListingMessageType correctiveActions, String... arguments) {
        listing.add(String.format(correctiveActions.getMessage(), arguments));
    }

    public void addContingency(Contingency contingency) throws IOException {
        checkArgument(contingency != null, "contingency must not have a null contingency");
        listing.add(IpsoOutputListingMessageType.CONTINGENCY_HEADER.getMessage()); // Contingency applied:
        listing.add(contingency.getId());

        listing.add(IpsoOutputListingMessageType.CONTINGENCY_ARRAY_HEADER.getMessage());
        listing.addAll(contingency.getElements().stream()
                .map(element -> formatLine(getIpsoId(element.getId()), element.getId(), element.getType().name()))
                .collect(toList()));
    }

    public void addLoadFlowResultsFor(Network network) {
        Preconditions.checkArgument(network != null, "network must be null");

        listing.add("");
        listing.add(String.format("LOAD FLOW RESULTS FOR NETWORK: %s", network.getName()));
        listing.add("");
        listing.add("GENERATORS");
        listing.add("#        Names                terminal P (MW)  terminal Q (MVAR) ");
        listing.add("#--------------------         ---------------- ---------------- ");
        for (Generator gen : network.getGenerators()) {
            listing.add(String.format("%s %s %15.8f %15.8f", getIpsoId(gen.getId()), gen.getId(), gen.getTerminal().getP(), gen.getTerminal().getQ()));
        }
        listing.add( "");

        listing.add( "BUSES");
        listing.add("#        Name                     V (kV)     Angle (Degrees) ");
        listing.add("#--------------------       ---------------  --------------- ");
        for (Bus bus : network.getBusBreakerView().getBuses()) {
            listing.add(String.format("%s %s %15.8f %15.8f", getIpsoId(bus.getId()), bus.getId(), bus.getV(), bus.getAngle()));
        }
        //listing.add("END BUSES\n");
        listing.add( "");

        listing.add( "BRANCHES");
        listing.add("#                                                            Q (MVAR)                          P (MW) ");
        listing.add("#                                                 ------------------------------- ------------------------------- ");
        listing.add("#                    Name                            From              To            From              To ");
        listing.add("#---------------------------------                --------------- --------------- --------------- --------------- ");
        for (Line line : network.getLines()) {
            listing.add(String.format("%s %s %15.8f %15.8f %15.8f %15.8f", getIpsoId(line.getId()), line.getId(), line.getTerminal1().getQ(), line.getTerminal2().getQ(), line.getTerminal1().getP(), line.getTerminal2().getP()));
        }
        listing.add( "");

        listing.add( "DANGLINGLINES");
        listing.add("#                                                            Q (MVAR)                          P (MW) ");
        listing.add("#                                                 ------------------------------- ------------------------------- ");
        listing.add("#                    Name                            From              To            From              To ");
        listing.add("#---------------------------------                --------------- --------------- --------------- --------------- ");
        for (DanglingLine danglingLine : network.getDanglingLines()) {
            listing.add(String.format("%s %s %15.8f %15.8f %15.8f %15.8f", getIpsoId(danglingLine.getId()), danglingLine.getId(), danglingLine.getTerminal().getQ(), null, danglingLine.getTerminal().getP(), null));
        }

        listing.add( "");
        listing.add( "BRANCHES CURRENT");
        listing.add("#                                                Flow (Ampere)     ");
        listing.add("#                                  ------------------------------- ");
        listing.add("#                    Name            From              To          ");
        listing.add("#--------------------------------- --------------- --------------- ");
        for (Line line : network.getLines()) {
            listing.add(String.format("%s %s %15.8f %15.8f", getIpsoId(line.getId()), line.getId(), line.getTerminal1().getI(), line.getTerminal2().getI()));
        }

        listing.add( "");
        listing.add( "DANGLINGLINES CURRENT");
        listing.add("#                                                Flow (Ampere)     ");
        listing.add("#                                  ------------------------------- ");
        listing.add("#                    Name            From              To          ");
        listing.add("#--------------------------------- --------------- --------------- ");
        for (DanglingLine danglingLine : network.getDanglingLines()) {
            listing.add(String.format("%s %s %15.8f %15.8f", getIpsoId(danglingLine.getId()), danglingLine.getId(), danglingLine.getTerminal().getI(), null));
        }
        //listing.add("END DANGLINGLINES CURRENT\n");
        listing.add( "");
        listing.add("TRANSFORMERS");
        listing.add("#                                                Q (MVAR)                         P (MW) ");
        listing.add("#                                  ------------------------------- ------------------------------- ");
        listing.add("#                    Name            From              To            From              To ");
        listing.add("#--------------------------------- --------------- --------------- --------------- --------------- ");
        for (TwoTerminalsConnectable transfo : network.getTwoWindingsTransformers()) {
            listing.add(String.format("%s %s %15.8f %15.8f %15.8f %15.8f", getIpsoId(transfo.getId()), transfo.getId(), transfo.getTerminal1().getQ(), transfo.getTerminal2().getQ(), transfo.getTerminal1().getP(), transfo.getTerminal2().getP()));
        }
        //listing.add("END TRANSFORMERS\n");
        listing.add("");
        //listing.add("TRANSFORMERS");
        listing.add("#                                                Flow (Ampere)      ");
        listing.add("#                                  -------------------------------  ");
        listing.add("#                    Name            From              To           ");
        listing.add("#--------------------------------- --------------- ---------------  ");
        for (TwoTerminalsConnectable transfo : network.getTwoWindingsTransformers()) {
            listing.add(String.format("%s %s %15.8f %15.8f", getIpsoId(transfo.getId()), transfo.getId(), transfo.getTerminal1().getI(), transfo.getTerminal2().getI()));
        }
        //listing.add("END TRANSFORMERS\n");

        listing.add("");
        listing.add("COUPLING");
        listing.add("#                    Name                                  Bus 1              Bus 2               V");
        listing.add("#--------------------------------------------------- ---------------- ------------------ ---------------  ");
        for (VoltageLevel voltageLevel : network.getVoltageLevels()) {
            for(Switch s : voltageLevel.getBusBreakerView().getSwitches()) {
                listing.add(String.format("%s %s %s %s %15.8f",
                        getIpsoId(s.getId()),
                        s.getId(),
                        voltageLevel.getBusBreakerView().getBus1(s.getId()),
                        voltageLevel.getBusBreakerView().getBus2(s.getId()),
                        voltageLevel.getNominalV()));
            }
        }

        listing.add("");
        listing.add("LOAD");
        listing.add("#                                           INITIAL POWER           ");
        listing.add("#                                  -------------------------------  ");
        listing.add("#                    Name            P0              Q0             ");
        listing.add("#--------------------------------- --------------- ---------------  ");
        for (Load load : network.getLoads()) {
            listing.add(String.format("%s %s %15.8f %15.8f", getIpsoId(load.getId()), load.getId(), load.getP0(), load.getQ0()));
        }
        listing.add(separation());
    }

    private String separation() {
        return StringUtils.repeat("=", 80);
    }

    @Deprecated
    public void write() {
        try {
            try (OutputStream fileToWrite = computationManager.newCommonFile(filename)) {
                List<String> linesToWrite = Lists.newArrayList();
                linesToWrite.add(separation());
                linesToWrite.add(header());
                linesToWrite.add(now());
                linesToWrite.add(separation());
                linesToWrite.addAll(listing);
                IOUtils.writeLines(linesToWrite, LINE_SEPARATOR, fileToWrite);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void write(ComputationManager computationManager, String filename) {
        try {
            try (OutputStream fileToWrite = computationManager.newCommonFile(filename)) {
                List<String> linesToWrite = newArrayList();
                linesToWrite.add(separation());
                linesToWrite.add(header());
                linesToWrite.add(now());
                linesToWrite.add(separation());
                linesToWrite.addAll(listing);
                IOUtils.writeLines(linesToWrite, LINE_SEPARATOR, fileToWrite);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void write(Path outputPath, String filename) {
        try {
            try (OutputStream fileToWrite = new FileOutputStream(outputPath.resolve(filename).toFile())) {
                List<String> linesToWrite = newArrayList();
                linesToWrite.add(separation());
                linesToWrite.add(header());
                linesToWrite.add(now());
                linesToWrite.add(separation());
                linesToWrite.addAll(listing);
                IOUtils.writeLines(linesToWrite, LINE_SEPARATOR, fileToWrite);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void addAmplError(Exception e) {
        addSeparator();
        listing.add("ERROR during AMPL execution: "+ e.getMessage());
        addSeparator();
    }

    public void addOptimizationError(IllegalArgumentException e) {
        addSeparator();
        listing.add("Optimization process error: "+ e.getMessage());
        addSeparator();
    }

    public void addResultCode(CCOFinalStatus status) {
        addSeparator();
        listing.add(IpsoOutputListingMessageType.CORRECTIVE_ACTION_RESULT.getMessage());
        listing.add(status.toString());
        addSeparator();
    }


    public void addAmplResult(AmplStatus status, Collection<String> actions) {
        addSeparator();
        listing.add(IpsoOutputListingMessageType.AMPL_SOLUTION_HEADER.getMessage());
        listing.add(status.getTextDescription());
        if(status == AmplStatus.SUCCEEDED) {
            addSeparator();
            listing.add(IpsoOutputListingMessageType.AMPL_SOLUTION_FOUND.getMessage());
            actions.stream().forEach(listing::add);
        }
        addSeparator();
    }


    public void addSeparator() {
        listing.add("");
    }

    public static String now() {
        Calendar cal = Calendar.getInstance();
        SimpleDateFormat sdf = new SimpleDateFormat(DATE_FORMAT_NOW);
        return sdf.format(cal.getTime());
    }

    public void addToListing(IpsoOptimizationResults result) {
        checkArgument(result != null, "result cannot be null");
        addStatusOf(result);
        if (result.hasSolutionFound()) {
            addSolutionOf(result);
        }
    }

    private void addSolutionOf(IpsoOptimizationResults results) {
        listing.add(String.format("%s %s", IpsoOutputListingMessageType.OBJ_FUN_VALUE.getMessage(), results.getSolution().getObjectiveFunctionValue()));
        listing.add(IpsoOutputListingMessageType.SOLUTION_FOUND.getMessage());
        results.getSolution().getSolutionElements().forEach(c -> listing.add(c.toString()));
    }

    private void addStatusOf(IpsoOptimizationResults results) {
        listing.add(results.getStatus().getMessage());
    }
}
