/**
 * Copyright (c) 2018, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package eu.itesla_project.iidm.ddb.eurostag_imp_exp;

import com.powsybl.commons.jaxb.JaxbUtil;
import com.powsybl.iidm.network.Branch;
import com.powsybl.iidm.network.Network;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Path;
import java.text.ParseException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * @author Christian Biasuzzi <christian.biasuzzi@techrain.it>
 */
public final class AutomatonA56 {
        /*
        A56 Automata (Loss of Synchronism Protection)

        <lossOfSynchronismProtections>
            <lossOfSynchronismProtection>
                <branch>IIDMID</branch>
                <side>2</side>
                <beat>2</beat>
                <delay>0.5</delay>
                <voltageDip>0.7</voltageDip>
                <baseVoltage>300.0</baseVoltage>
                <timeConstant>10.0</timeConstant>
                <comment>MM</comment>
            </lossOfSynchronismProtection>
        </lossOfSynchronismProtections>
     */

    static Logger LOGGER = LoggerFactory.getLogger(AutomatonA56.class);

    public static final String A56_AUTOMATA_ID = "A56";

    @XmlRootElement(name = "lossOfSynchronismProtection")
    @XmlAccessorType(XmlAccessType.FIELD)
    private static class LossOfSynchronismProtection {
        private String branch; //Eurostag branch identifier (1stnode 2ndnode orderCode)
        private String side; //indicates which bus should be put first in the dta record
        private int beat; //number of beats before activation
        private Double delay; //delay before action (breaker opening, in s)
        private Double voltageDip; //initial voltage proportion (in pu)
        private Double baseVoltage; //base voltage (in kV)
        private Double timeConstant; //time constant for the measurement of the reference voltage (in s)
        private String comment; //two letters indicating the indices of the zons that are disconnected by the protection

        public LossOfSynchronismProtection() {
        }

        public LossOfSynchronismProtection(String branch, String side, int beat, Double delay, Double voltageDip, Double baseVoltage, Double timeConstant, String comment) {
            this.branch = branch;
            this.side = side;
            this.beat = beat;
            this.delay = delay;
            this.voltageDip = voltageDip;
            this.baseVoltage = baseVoltage;
            this.timeConstant = timeConstant;
            this.comment = comment;
        }

        @Override
        public String toString() {
            return "LossOfSynchronismProtection{" +
                    "branch='" + branch + '\'' +
                    ", side=" + side +
                    ", beat=" + beat +
                    ", delay=" + delay +
                    ", voltageDip=" + voltageDip +
                    ", baseVoltage=" + baseVoltage +
                    ", timeConstant=" + timeConstant +
                    ", comment='" + comment + '\'' +
                    '}';
        }
    }

    @XmlRootElement(name = "lossOfSynchronismProtections")
    @XmlAccessorType(XmlAccessType.FIELD)
    private static class LossOfSynchronismProtections {
        @XmlElement(name = "lossOfSynchronismProtection")
        List<LossOfSynchronismProtection> lossOfSynchronismProtections;

        public List<LossOfSynchronismProtection> getLossOfSynchronismProtections() {
            return lossOfSynchronismProtections;
        }

        public void setLossOfSynchronismProtections(List<LossOfSynchronismProtection> lossOfSynchronismProtections) {
            this.lossOfSynchronismProtections = lossOfSynchronismProtections;
        }

        @Override
        public String toString() {
            return "lossOfSynchronismProtections=" + lossOfSynchronismProtections;
        }
    }

    private AutomatonA56() {
    }

    public static EurostagRecord createA56Record(LossOfSynchronismProtection loss, String eurostagBranchId) {
        Objects.requireNonNull(loss);
        Objects.requireNonNull(eurostagBranchId);
        HashMap<String, Object> zm = new HashMap<String, Object>();
        zm.put("BRANCH_ID", eurostagBranchId);
        zm.put("NO_BEATS", loss.beat);
        zm.put("DELAY", loss.delay);
        zm.put("INITIAL_VOLTAGE", loss.voltageDip);
        zm.put("BASE_VOLTAGE", loss.baseVoltage);
        zm.put("TIME_CONSTANT", loss.timeConstant);
        zm.put("COMMENTS", loss.comment);
        EurostagRecord eRecord = new EurostagRecord(A56_AUTOMATA_ID, zm);
        return eRecord;
    }

    public static void writeToDta(Network network, PrintStream out, Map<String, String> iidm2eurostagId, Path defaultAutomatonA56DetailsFile) throws IOException, ParseException {
        Objects.requireNonNull(iidm2eurostagId);
        LossOfSynchronismProtections losses = JaxbUtil.unmarchallFile(LossOfSynchronismProtections.class, defaultAutomatonA56DetailsFile);
        DtaParser.dumpAutomatonHeader(A56_AUTOMATA_ID, false, out);
        losses.getLossOfSynchronismProtections().forEach(loss -> {
            Branch branch = network.getBranch(loss.branch);
            String eurostagId = iidm2eurostagId.get(loss.branch);
            if (eurostagId == null) {
                LOGGER.warn("skipping entry for iidm id {}: eurostag id not found in mapping.", loss.branch);
            } else {
                if (eurostagId.length() != 19) {
                    LOGGER.warn("skipping entry for iidm id {}: unexpected format for eurostag id in mapping.", loss.branch);
                } else {
                    // eurostag's branch id format expected: xxxxxxxx xxxxxxxx x
                    String side1 = eurostagId.substring(0, 8);
                    String side2 = eurostagId.substring(9, 17);
                    String parallelIndex = eurostagId.substring(18);
                    String aBranch = loss.branch;

                    switch (loss.side) {
                        case "1":
                            aBranch = side1 + " " + side2 + " " + parallelIndex;
                            break;
                        case "2":
                            aBranch = side2 + " " + side1 + " " + parallelIndex;
                            break;
                        case "":
                            // side1 is the bus whose base voltage is equal to the protection's base voltage
                            // else (2 buses of the same voltage or no bus of the indicated voltage, print a warning and put it on side 1
                            double baseVoltage1 = branch.getTerminal1().getVoltageLevel().getNominalV();
                            double baseVoltage2 = branch.getTerminal2().getVoltageLevel().getNominalV();
                            if (Double.compare(baseVoltage1, baseVoltage2) != 0) {
                                if (Double.compare(loss.baseVoltage, baseVoltage1) == 0) {
                                    aBranch = side1 + " " + side2 + " " + parallelIndex;
                                } else if (Double.compare(loss.baseVoltage, baseVoltage2) == 0) {
                                    aBranch = side2 + " " + side1 + " " + parallelIndex;
                                } else {
                                    LOGGER.warn("{}; branch bus1 {} (base voltage1 {}) - bus2 {} (base voltage2 {}). Using bus1 {} ", loss, side1, baseVoltage1, side2, baseVoltage2, side1);
                                    aBranch = side1 + " " + side2 + " " + parallelIndex;
                                }
                            } else {
                                LOGGER.warn("{}; branch bus1 {} and bus2 {} have the same base voltage {}. Using bus1 {} ", loss, side1, side2, baseVoltage1, side1);
                                aBranch = side1 + " " + side2 + " " + parallelIndex;
                            }
                            break;
                        default:
                            LOGGER.warn("skipping entry for iidm id {}: unexpected 'side' value {}.", loss.branch, loss.side);
                    }
                    LOGGER.debug("{}; eurostag branch id: {}", loss, aBranch);
                    try {
                        DtaParser.dumpZone(createA56Record(loss, aBranch), out);
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        });
        DtaParser.dumpAutomatonHeader(A56_AUTOMATA_ID, true, out);
    }
}
