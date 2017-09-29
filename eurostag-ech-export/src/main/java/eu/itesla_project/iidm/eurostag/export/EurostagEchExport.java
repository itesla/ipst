/**
 * Copyright (c) 2016, All partners of the iTesla project (http://www.itesla-project.eu/consortium)
 * Copyright (c) 2017, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package eu.itesla_project.iidm.eurostag.export;

import com.google.common.base.Strings;
import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import eu.itesla_project.commons.ITeslaException;
import eu.itesla_project.eurostag.network.*;
import eu.itesla_project.eurostag.network.io.EsgWriter;
import eu.itesla_project.iidm.network.*;
import eu.itesla_project.iidm.network.util.Identifiables;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/**
 * @author Geoffroy Jamgotchian <geoffroy.jamgotchian at rte-france.com>
 */
public class EurostagEchExport {

    private static final Logger LOGGER = LoggerFactory.getLogger(EurostagEchExport.class);

    /**
     * epsilon value for conductance
     */
    public static final float G_EPSILON = 0.00001f;

    /**
     * epsilon value for susceptance
     */
    public static final float B_EPSILON = 0.00001f;

    private static final String XNODE_V_PROPERTY = "xnode_v";
    private static final String XNODE_ANGLE_PROPERTY = "xnode_angle";

    private final Network network;
    private final EurostagEchExportConfig config;
    private final BranchParallelIndexes parallelIndexes;
    private final EurostagDictionary dictionary;
    private final EurostagFakeNodes fakeNodes;

    public EurostagEchExport(Network network, EurostagEchExportConfig config, BranchParallelIndexes parallelIndexes, EurostagDictionary dictionary, EurostagFakeNodes fakeNodes) {
        this.network = Objects.requireNonNull(network);
        this.config = Objects.requireNonNull(config);
        this.parallelIndexes = Objects.requireNonNull(parallelIndexes);
        this.dictionary = Objects.requireNonNull(dictionary);
        this.fakeNodes = Objects.requireNonNull(fakeNodes);
    }

    public EurostagEchExport(Network network, EurostagEchExportConfig config) {
        this.network = Objects.requireNonNull(network);
        this.config = config;
        this.fakeNodes = EurostagFakeNodes.build(network, config);
        this.parallelIndexes = BranchParallelIndexes.build(network, config, fakeNodes);
        this.dictionary = EurostagDictionary.create(network, parallelIndexes, config, fakeNodes);
    }

    public EurostagEchExport(Network network) {
        this(network, new EurostagEchExportConfig());
    }

    private void createAreas(EsgNetwork esgNetwork) {
        esgNetwork.addArea(new EsgArea(new Esg2charName(EchUtil.FAKE_AREA), EsgArea.Type.AC));
        for (Country c : network.getCountries()) {
            esgNetwork.addArea(new EsgArea(new Esg2charName(c.toString()), EsgArea.Type.AC));
        }

        if (network.getHvdcLineCount() > 0) {
            esgNetwork.addArea(new EsgArea(new Esg2charName("DC"), EsgArea.Type.DC));
        }
    }

    private EsgNode createNode(String busId, String countryIsoCode, float nominalV, float v, float angle, boolean slackBus) {
        return new EsgNode(new Esg2charName(countryIsoCode),
                new Esg8charName(dictionary.getEsgId(busId)),
                nominalV,
                Float.isNaN(v) ? 1f : v / nominalV,
                Float.isNaN(angle) ? 0f : angle,
                slackBus);
    }

    private EsgNode createNode(String busId, VoltageLevel vl, float v, float angle, boolean slackBus) {
        return createNode(busId, vl.getSubstation().getCountry().name(), vl.getNominalV(), v, angle, slackBus);
    }

    private void createNodes(EsgNetwork esgNetwork) {
        fakeNodes.referencedEsgIdsAsStream().forEach(esgId -> {
            VoltageLevel vlevel = fakeNodes.getVoltageLevelByEsgId(esgId);
            float nominalV = ((vlevel != null) ? vlevel.getNominalV() : 380f);
            esgNetwork.addNode(createNode(esgId, EchUtil.FAKE_AREA, nominalV, nominalV, 0f, false));
        });

        Bus sb = EchUtil.selectSlackbus(network, config);
        if (sb == null) {
            throw new RuntimeException("Slack bus not found");
        }
        LOGGER.debug("Slack bus: {} ({})", sb, sb.getVoltageLevel().getId());
        for (Bus b : Identifiables.sort(EchUtil.getBuses(network, config))) {
            esgNetwork.addNode(createNode(b.getId(), b.getVoltageLevel(), b.getV(), b.getAngle(), sb == b));
        }
        for (DanglingLine dl : Identifiables.sort(network.getDanglingLines())) {
            Properties properties = dl.getProperties();
            String strV = properties.getProperty(XNODE_V_PROPERTY);
            String strAngle = properties.getProperty(XNODE_ANGLE_PROPERTY);
            float v = strV != null ? Float.parseFloat(strV) : Float.NaN;
            float angle = strAngle != null ? Float.parseFloat(strAngle) : Float.NaN;
            esgNetwork.addNode(createNode(EchUtil.getBusId(dl), dl.getTerminal().getVoltageLevel(), v, angle, false));
        }
    }

    private static EsgBranchConnectionStatus getStatus(ConnectionBus bus1, ConnectionBus bus2) {
        if (!bus1.isConnected() && !bus2.isConnected()) {
            return EsgBranchConnectionStatus.OPEN_AT_BOTH_SIDES;
        } else if (bus1.isConnected() && bus2.isConnected()) {
            return EsgBranchConnectionStatus.CLOSED_AT_BOTH_SIDE;
        } else {
            return bus1.isConnected() ? EsgBranchConnectionStatus.OPEN_AT_RECEIVING_SIDE
                    : EsgBranchConnectionStatus.OPEN_AT_SENDING_SIDE;
        }
    }

    private void createCouplingDevices(EsgNetwork esgNetwork) {
        for (VoltageLevel vl : Identifiables.sort(network.getVoltageLevels())) {
            for (Switch sw : Identifiables.sort(EchUtil.getSwitches(vl, config))) {
                Bus bus1 = EchUtil.getBus1(vl, sw.getId(), config);
                Bus bus2 = EchUtil.getBus2(vl, sw.getId(), config);
                esgNetwork.addCouplingDevice(new EsgCouplingDevice(new EsgBranchName(new Esg8charName(dictionary.getEsgId(bus1.getId())),
                        new Esg8charName(dictionary.getEsgId(bus2.getId())),
                        parallelIndexes.getParallelIndex(sw.getId())),
                        sw.isOpen() ? EsgCouplingDevice.ConnectionStatus.OPEN : EsgCouplingDevice.ConnectionStatus.CLOSED));
            }
        }
    }

    private EsgLine createLine(String id, ConnectionBus bus1, ConnectionBus bus2, float nominalV, float r, float x, float g,
                               float b, EsgGeneralParameters parameters) {
        EsgBranchConnectionStatus status = getStatus(bus1, bus2);
        float rate = parameters.getSnref();
        float vnom2 = (float) Math.pow(nominalV, 2);
        float rb = r * parameters.getSnref() / vnom2;
        float rxb = x * parameters.getSnref() / vnom2;
        float gs = g / parameters.getSnref() * vnom2;
        float bs = b / parameters.getSnref() * vnom2;
        return new EsgLine(new EsgBranchName(new Esg8charName(dictionary.getEsgId(bus1.getId())),
                new Esg8charName(dictionary.getEsgId(bus2.getId())),
                parallelIndexes.getParallelIndex(id)),
                status, rb, rxb, gs, bs, rate);
    }

    private EsgDissymmetricalBranch createDissymmetricalBranch(String id, ConnectionBus bus1, ConnectionBus bus2,
                                                               float nominalV, float r, float x, float g1, float b1, float g2, float b2,
                                                               EsgGeneralParameters parameters) {
        EsgBranchConnectionStatus status = getStatus(bus1, bus2);
        float rate = parameters.getSnref();
        float vnom2 = (float) Math.pow(nominalV, 2);
        float rb = (r * parameters.getSnref()) / vnom2;
        float rxb = (x * parameters.getSnref()) / vnom2;
        float gs1 = (g1 / parameters.getSnref()) * vnom2;
        float bs1 = (b1 / parameters.getSnref()) * vnom2;
        float gs2 = (g2 / parameters.getSnref()) * vnom2;
        float bs2 = (b2 / parameters.getSnref()) * vnom2;
        return new EsgDissymmetricalBranch(new EsgBranchName(new Esg8charName(dictionary.getEsgId(bus1.getId())),
                new Esg8charName(dictionary.getEsgId(bus2.getId())),
                parallelIndexes.getParallelIndex(id)),
                status, rb, rxb, gs1, bs1, rate, rb, rxb, gs2, bs2);
    }

    private void createLines(EsgNetwork esgNetwork, EsgGeneralParameters parameters) {
        for (Line l : Identifiables.sort(network.getLines())) {
            ConnectionBus bus1 = ConnectionBus.fromTerminal(l.getTerminal1(), config, fakeNodes);
            ConnectionBus bus2 = ConnectionBus.fromTerminal(l.getTerminal2(), config, fakeNodes);
            // if the admittance are the same in the both side of PI line model
            if (Math.abs(l.getG1() - l.getG2()) < G_EPSILON && Math.abs(l.getB1() - l.getB2()) < B_EPSILON) {
                //...create a simple line
                esgNetwork.addLine(createLine(l.getId(), bus1, bus2, l.getTerminal1().getVoltageLevel().getNominalV(),
                        l.getR(), l.getX(), l.getG1(), l.getB1(), parameters));
            } else {
                EsgBranchConnectionStatus status = getStatus(bus1, bus2);
                if (status.equals(EsgBranchConnectionStatus.CLOSED_AT_BOTH_SIDE)) {
                    // create a dissymmetrical branch
                    esgNetwork.addDissymmetricalBranch(createDissymmetricalBranch(l.getId(), bus1, bus2, l.getTerminal1().getVoltageLevel().getNominalV(),
                            l.getR(), l.getX(), l.getG1(), l.getB1(), l.getG2(), l.getB2(), parameters));
                } else {
                    // half connected dissymmetrical branches are not allowed: remove the dissymmetry (by averaging B1 and B2, G1 and G2) and create a simple line
                    // This is an approximation: the best electrotechnical solution would require an additional fake node and a coupling on each disconnected end of the DyssimmetricalBranch.
                    LOGGER.warn("line {}: half connected dissymmetrical branches are not allowed; removes the dissymmetry by averaging line's B1 {} and B2 {} , G1 {} and  G2 {}", l, l.getB1(), l.getB2(), l.getG1(), l.getG2());
                    esgNetwork.addLine(createLine(l.getId(), bus1, bus2, l.getTerminal1().getVoltageLevel().getNominalV(),
                            l.getR(), l.getX(), (l.getG1() + l.getG2()) / 2, (l.getB1() + l.getB2()) / 2, parameters));
                }
            }
        }
        for (DanglingLine dl : Identifiables.sort(network.getDanglingLines())) {
            ConnectionBus bus1 = ConnectionBus.fromTerminal(dl.getTerminal(), config, fakeNodes);
            ConnectionBus bus2 = new ConnectionBus(true, EchUtil.getBusId(dl));
            esgNetwork.addLine(createLine(dl.getId(), bus1, bus2, dl.getTerminal().getVoltageLevel().getNominalV(),
                    dl.getR(), dl.getX(), dl.getG() / 2, dl.getB() / 2, parameters));
        }
    }

    private EsgDetailedTwoWindingTransformer.Tap createTap(TwoWindingsTransformer twt, int iplo, float rho, float dr, float dx,
                                                           float dephas, float rate, EsgGeneralParameters parameters) {
        float nomiU2 = twt.getTerminal2().getVoltageLevel().getNominalV();
        float rho_i = twt.getRatedU2() / twt.getRatedU1() * rho;
        float uno1 = nomiU2 / rho_i;
        float uno2 = nomiU2;
        float r = twt.getR() * (1 + dr / 100.0f);
        float x = twt.getX() * (1 + dx / 100.0f);

        //...mTrans.getR() = Get the nominal series resistance specified in Ω at the secondary voltage side.
        float zb2 = (float) (Math.pow(nomiU2, 2) / parameters.getSnref());
        float rpu2 = r / zb2;  //...total line resistance  [p.u.](Base snref)
        float xpu2 = x / zb2;  //...total line reactance   [p.u.](Base snref)

        //...leakage impedance [%] (base rate)
        float ucc;
        if (xpu2 < 0) {
            ucc = xpu2 * 100f * rate / parameters.getSnref();
        } else {
            float zpu2 = (float) Math.hypot(rpu2, xpu2);
            ucc = zpu2 * 100f * rate / parameters.getSnref();
        }

        return new EsgDetailedTwoWindingTransformer.Tap(iplo, dephas, uno1, uno2, ucc);
    }


    private void createAdditionalBank(EsgNetwork esgNetwork, TwoWindingsTransformer twt, Terminal terminal, String nodeName, Set<String> additionalBanksIds) {
        float rcapba = 0.0f;
        if (-twt.getB() < 0) {
            rcapba = twt.getB() * (float) Math.pow(terminal.getVoltageLevel().getNominalV(), 2) / (config.isSpecificCompatibility() ? 2 : 1);
        }
        float plosba = 0.0f;
        if (twt.getG() < 0) {
            plosba = twt.getG() * (float) Math.pow(terminal.getVoltageLevel().getNominalV(), 2) / (config.isSpecificCompatibility() ? 2 : 1);
        }
        if ((Math.abs(plosba) > G_EPSILON) || (rcapba > B_EPSILON)) {
            //simple new bank naming: 5 first letters of the node name, 7th letter of the node name, 'C', order code
            String nnodeName = Strings.padEnd(nodeName, 8, ' ');
            String newBankNamePrefix = nnodeName.substring(0, 5) + nnodeName.charAt(6) + 'C';
            String newBankName = newBankNamePrefix + '0';
            int counter = 1;
            while (additionalBanksIds.contains(newBankName)) {
                String newCounter = Integer.toString(counter++);
                if (newCounter.length() > 1) {
                    throw new RuntimeException("Renaming error " + nodeName + " -> " + newBankName);
                }
                newBankName = newBankNamePrefix + newCounter;
            }
            additionalBanksIds.add(newBankName);
            LOGGER.info("create additional bank: {}, node: {}", newBankName, nodeName);
            esgNetwork.addCapacitorsOrReactorBanks(new EsgCapacitorOrReactorBank(new Esg8charName(newBankName), new Esg8charName(nodeName), 1, plosba, rcapba, 1, EsgCapacitorOrReactorBank.RegulatingMode.NOT_REGULATING));
        }

    }

    private void createTransformers(EsgNetwork esgNetwork, EsgGeneralParameters parameters) {
        Set<String> additionalBanksIds = new HashSet<>();

        for (TwoWindingsTransformer twt : Identifiables.sort(network.getTwoWindingsTransformers())) {
            ConnectionBus bus1 = ConnectionBus.fromTerminal(twt.getTerminal1(), config, fakeNodes);
            ConnectionBus bus2 = ConnectionBus.fromTerminal(twt.getTerminal2(), config, fakeNodes);

            EsgBranchConnectionStatus status = getStatus(bus1, bus2);

            //...IIDM gives no rate value. we take rate = 100 MVA But we have to find the corresponding pcu, pfer ...
            float rate = 100.f;

            //**************************
            //*** LOSSES COMPUTATION *** (Record 1)
            //**************************

            float nomiU2 = twt.getTerminal2().getVoltageLevel().getNominalV();

            //...mTrans.getR() = Get the nominal series resistance specified in Ω at the secondary voltage side.
            float Rpu2 = (twt.getR() * parameters.getSnref()) / nomiU2 / nomiU2;  //...total line resistance  [p.u.](Base snref)
            float Gpu2 = (twt.getG() / parameters.getSnref()) * nomiU2 * nomiU2;  //...semi shunt conductance [p.u.](Base snref)
            float Bpu2 = (twt.getB() / parameters.getSnref()) * nomiU2 * nomiU2;  //...semi shunt susceptance [p.u.](Base snref)

            //...changing base snref -> base rate to compute losses
            float pcu = Rpu2 * rate * 100f / parameters.getSnref();                  //...base rate (100F -> %)
            float pfer = 10000f * ((float) Math.sqrt(Gpu2) / rate) * (parameters.getSnref() / 100f);  //...base rate
            float modgb = (float) Math.sqrt(Math.pow(Gpu2, 2.f) + Math.pow(Bpu2, 2.f));
            float cmagn = 10000 * (modgb / rate) * (parameters.getSnref() / 100f);  //...magnetizing current [% base rate]
            float esat = 1.f;

            //***************************
            // *** TAP TRANSFORMATION *** (Record 2)
            //***************************

            EsgDetailedTwoWindingTransformer.RegulatingMode regulatingMode = EsgDetailedTwoWindingTransformer.RegulatingMode.NOT_REGULATING;
            Esg8charName zbusr = null; //...regulated node name (if empty, no tap change)
            float voltr = Float.NaN;
            int ktpnom; //...nominal tap number is not available in IIDM. Take th median plot by default
            int ktap8;  //...initial tap position (tap number) (Ex: 10)
            List<EsgDetailedTwoWindingTransformer.Tap> taps = new ArrayList<>();

            RatioTapChanger rtc = twt.getRatioTapChanger();
            PhaseTapChanger ptc = twt.getPhaseTapChanger();
            if (rtc != null && ptc == null) {
                if (rtc.isRegulating()) {
                    ConnectionBus regulatingBus = ConnectionBus.fromTerminal(rtc.getRegulationTerminal(), config, null);
                    if (regulatingBus.getId() != null) {
                        regulatingMode = EsgDetailedTwoWindingTransformer.RegulatingMode.VOLTAGE;
                        zbusr = new Esg8charName(dictionary.getEsgId(regulatingBus.getId()));
                    }
                }
                voltr = rtc.getTargetV();
                ktap8 = rtc.getTapPosition() - rtc.getLowTapPosition() + 1;
                ktpnom = rtc.getStepCount() / 2 + 1;
                for (int p = rtc.getLowTapPosition(); p <= rtc.getHighTapPosition(); p++) {
                    int iplo = p - rtc.getLowTapPosition() + 1;
                    taps.add(createTap(twt, iplo, rtc.getStep(p).getRho(), rtc.getStep(p).getR(), rtc.getStep(p).getX(), 0f, rate, parameters));
                }

            } else if (ptc != null && rtc == null) {
                if (ptc.getRegulationMode() == PhaseTapChanger.RegulationMode.CURRENT_LIMITER && ptc.isRegulating()) {
                    String regulbus = EchUtil.getBus(ptc.getRegulationTerminal(), config).getId();
                    if (regulbus.equals(bus1.getId())) {
                        regulatingMode = EsgDetailedTwoWindingTransformer.RegulatingMode.ACTIVE_FLUX_SIDE_1;
                    }
                    if (regulbus.equals(bus2.getId())) {
                        regulatingMode = EsgDetailedTwoWindingTransformer.RegulatingMode.ACTIVE_FLUX_SIDE_2;
                    }
                    if (regulatingMode == EsgDetailedTwoWindingTransformer.RegulatingMode.NOT_REGULATING) {
                        throw new ITeslaException("Phase transformer " + twt.getId() + " has an unknown regulated node");
                    }
                }
                ktap8 = ptc.getTapPosition() - ptc.getLowTapPosition() + 1;
                ktpnom = ptc.getStepCount() / 2 + 1;
                for (int p = ptc.getLowTapPosition(); p <= ptc.getHighTapPosition(); p++) {
                    int iplo = p - ptc.getLowTapPosition() + 1;
                    taps.add(createTap(twt, iplo, ptc.getStep(p).getRho(), ptc.getStep(p).getR(), ptc.getStep(p).getX(), ptc.getStep(p).getAlpha(), rate, parameters));
                }
            } else if (rtc == null && ptc == null) {
                ktap8 = 1;
                ktpnom = 1;
                taps.add(createTap(twt, 1, 1f, 0f, 0f, 0f, rate, parameters));
            } else {
                throw new RuntimeException("Transformer " + twt.getId() + "  with voltage and phase tap changer not yet supported");
            }

            // trick to handle the fact that Eurostag model allows only the impedance to change and not the resistance.
            // As an approximation, the resistance is fixed to the value it has for the initial step,
            // but discrepancies will occur if the step is changed.
            if ((ptc != null) || (rtc != null)) {
                float dr = (rtc != null) ? rtc.getStep(rtc.getTapPosition()).getR() : ptc.getStep(ptc.getTapPosition()).getR();
                float tap_adjusted_r = twt.getR() * (1 + dr / 100.0f);
                float rpu2_adjusted = (tap_adjusted_r * parameters.getSnref()) / nomiU2 / nomiU2;
                pcu = rpu2_adjusted * rate * 100f / parameters.getSnref();

                float dg = (rtc != null) ? rtc.getStep(rtc.getTapPosition()).getG() : ptc.getStep(ptc.getTapPosition()).getG();
                float tap_adjusted_g = twt.getG() * (1 + dg / 100.0f);
                float gpu2_adjusted = (tap_adjusted_g / parameters.getSnref()) * nomiU2 * nomiU2;
                pfer = 10000f * ((float) Math.sqrt(gpu2_adjusted) / rate) * (parameters.getSnref() / 100f);

                float db = (rtc != null) ? rtc.getStep(rtc.getTapPosition()).getB() : ptc.getStep(ptc.getTapPosition()).getB();
                float tap_adjusted_b = twt.getB() * (1 + db / 100.0f);
                float bpu2_adjusted = (tap_adjusted_b / parameters.getSnref()) * nomiU2 * nomiU2;
                modgb = (float) Math.sqrt(Math.pow(gpu2_adjusted, 2.f) + Math.pow(bpu2_adjusted, 2.f));
                cmagn = 10000 * (modgb / rate) * (parameters.getSnref() / 100f);
            }

            float pregmin = Float.NaN; //...?
            float pregmax = Float.NaN; //...?

            //handling of the cases where cmagn should be negative and where pfer should be negative
            if ((-twt.getB() < 0) || (twt.getG() < 0) || (config.isSpecificCompatibility())) {
                createAdditionalBank(esgNetwork, twt, twt.getTerminal1(), dictionary.getEsgId(bus1.getId()), additionalBanksIds);
                if (config.isSpecificCompatibility()) {
                    createAdditionalBank(esgNetwork, twt, twt.getTerminal2(), dictionary.getEsgId(bus2.getId()), additionalBanksIds);
                }
                if (twt.getG() < 0) {
                    pfer = 0.0f;
                }
                if (-twt.getB() < 0) {
                    cmagn = pfer;
                }
            }


            EsgDetailedTwoWindingTransformer esgTransfo = new EsgDetailedTwoWindingTransformer(
                    new EsgBranchName(new Esg8charName(dictionary.getEsgId(bus1.getId())),
                            new Esg8charName(dictionary.getEsgId(bus2.getId())),
                            parallelIndexes.getParallelIndex(twt.getId())),
                    status,
                    cmagn,
                    rate,
                    pcu,
                    pfer,
                    esat,
                    ktpnom,
                    ktap8,
                    zbusr,
                    voltr,
                    pregmin,
                    pregmax,
                    regulatingMode);

            //***************************
            // *** TAP TRANSFORMATION *** (Record 3)
            //***************************

            esgTransfo.getTaps().addAll(taps);

            esgNetwork.addDetailedTwoWindingTransformer(esgTransfo);
        }

        for (ThreeWindingsTransformer twt : network.getThreeWindingsTransformers()) {
            throw new AssertionError("TODO");
        }
    }

    private EsgLoad createLoad(ConnectionBus bus, String loadId, float p0, float q0) {
        EsgConnectionStatus status = bus.isConnected() ? EsgConnectionStatus.CONNECTED : EsgConnectionStatus.NOT_CONNECTED;
        return new EsgLoad(status, new Esg8charName(dictionary.getEsgId(loadId)),
                new Esg8charName(dictionary.getEsgId(bus.getId())),
                0f, 0f, p0, 0f, 0f, q0);
    }

    private void createLoads(EsgNetwork esgNetwork) {
        for (Load l : Identifiables.sort(network.getLoads())) {
            ConnectionBus bus = ConnectionBus.fromTerminal(l.getTerminal(), config, fakeNodes);
            esgNetwork.addLoad(createLoad(bus, l.getId(), l.getP0(), l.getQ0()));
        }
        for (DanglingLine dl : Identifiables.sort(network.getDanglingLines())) {
            ConnectionBus bus = new ConnectionBus(true, EchUtil.getBusId(dl));
            esgNetwork.addLoad(createLoad(bus, EchUtil.getLoadId(dl), dl.getP0(), dl.getQ0()));
        }
    }

    private void createGenerators(EsgNetwork esgNetwork) {
        for (Generator g : Identifiables.sort(network.getGenerators())) {
            ConnectionBus bus = ConnectionBus.fromTerminal(g.getTerminal(), config, fakeNodes);

            EsgConnectionStatus status = bus.isConnected() ? EsgConnectionStatus.CONNECTED : EsgConnectionStatus.NOT_CONNECTED;
            float pgen = g.getTargetP();
            float qgen = g.getTargetQ();
            float pgmin = g.getMinP();
            float pgmax = g.getMaxP();
            boolean isQminQmaxInverted = g.getReactiveLimits().getMinQ(pgen) > g.getReactiveLimits().getMaxQ(pgen);
            if (isQminQmaxInverted) {
                LOGGER.warn("inverted qmin {} and qmax {} values for generator {}", g.getReactiveLimits().getMinQ(pgen), g.getReactiveLimits().getMaxQ(pgen), g.getId());
            }
            // in case qmin and qmax are inverted, take out the unit from the voltage regulation if it has a target Q
            // and open widely the Q interval
            float qgmin = (config.isNoGeneratorMinMaxQ() || isQminQmaxInverted) ? -9999 : g.getReactiveLimits().getMinQ(pgen);
            float qgmax = (config.isNoGeneratorMinMaxQ() || isQminQmaxInverted) ? 9999 : g.getReactiveLimits().getMaxQ(pgen);
            EsgRegulatingMode mode = (isQminQmaxInverted && !Float.isNaN(qgen)) ? EsgRegulatingMode.NOT_REGULATING :
                    (g.isVoltageRegulatorOn() && g.getTargetV() >= 0.1 ? EsgRegulatingMode.REGULATING : EsgRegulatingMode.NOT_REGULATING);
            float vregge = (isQminQmaxInverted && !Float.isNaN(qgen)) ? Float.NaN : (g.isVoltageRegulatorOn() ? g.getTargetV() : Float.NaN);
            float qgensh = 1.f;

            //fails, when noSwitch is true !!
            //Bus regulatingBus = g.getRegulatingTerminal().getBusBreakerView().getConnectableBus();
            ConnectionBus regulatingBus = ConnectionBus.fromTerminal(g.getRegulatingTerminal(), config, fakeNodes);

            try {
                esgNetwork.addGenerator(new EsgGenerator(new Esg8charName(dictionary.getEsgId(g.getId())),
                        new Esg8charName(dictionary.getEsgId(bus.getId())),
                        pgmin, pgen, pgmax, qgmin, qgen, qgmax, mode, vregge,
                        new Esg8charName(dictionary.getEsgId(regulatingBus.getId())),
                        qgensh, status));
            } catch (Throwable t) {
                t.printStackTrace();
            }
        }
    }

    private void createBanks(EsgNetwork esgNetwork) {
        for (ShuntCompensator sc : Identifiables.sort(network.getShunts())) {
            ConnectionBus bus = ConnectionBus.fromTerminal(sc.getTerminal(), config, fakeNodes);

            //...number of steps in service
            int ieleba = bus.isConnected() ? sc.getCurrentSectionCount() : 0; // not really correct, because it can be connected with zero section, EUROSTAG should be modified...
            float plosba = 0.f; // no active lost in the iidm shunt compensator
            float vnom = sc.getTerminal().getVoltageLevel().getNominalV();
            float rcapba = vnom * vnom * sc.getbPerSection();
            int imaxba = sc.getMaximumSectionCount();
            EsgCapacitorOrReactorBank.RegulatingMode xregba = EsgCapacitorOrReactorBank.RegulatingMode.NOT_REGULATING;
            esgNetwork.addCapacitorsOrReactorBanks(new EsgCapacitorOrReactorBank(new Esg8charName(dictionary.getEsgId(sc.getId())),
                    new Esg8charName(dictionary.getEsgId(bus.getId())),
                    ieleba, plosba, rcapba, imaxba, xregba));
        }
    }

    private void createStaticVarCompensators(EsgNetwork esgNetwork) {
        for (StaticVarCompensator svc : Identifiables.sort(network.getStaticVarCompensators())) {
            ConnectionBus bus = ConnectionBus.fromTerminal(svc.getTerminal(), config, fakeNodes);

            Esg8charName znamsvc = new Esg8charName(dictionary.getEsgId(svc.getId()));
            EsgConnectionStatus xsvcst = bus.isConnected() ? EsgConnectionStatus.CONNECTED : EsgConnectionStatus.NOT_CONNECTED;
            Esg8charName znodsvc = new Esg8charName(dictionary.getEsgId(bus.getId()));
            float factor = (float) Math.pow(svc.getTerminal().getVoltageLevel().getNominalV(), 2);
            float bmin = (config.isSvcAsFixedInjectionInLF() == false) ? svc.getBmin() * factor : -9999999; // [Mvar]
            float binit = (config.isSvcAsFixedInjectionInLF() == false) ? svc.getReactivePowerSetPoint() : -svc.getTerminal().getQ() * (float) Math.pow(svc.getTerminal().getVoltageLevel().getNominalV() / EchUtil.getBus(svc.getTerminal(), config).getV(), 2); // [Mvar]
            float bmax = (config.isSvcAsFixedInjectionInLF() == false) ? svc.getBmax() * factor : 9999999; // [Mvar]
            EsgRegulatingMode xregsvc = ((svc.getRegulationMode() == StaticVarCompensator.RegulationMode.VOLTAGE) && (!config.isSvcAsFixedInjectionInLF())) ? EsgRegulatingMode.REGULATING : EsgRegulatingMode.NOT_REGULATING;
            float vregsvc = svc.getVoltageSetPoint();
            float qsvsch = 1.0f;
            esgNetwork.addStaticVarCompensator(
                    new EsgStaticVarCompensator(znamsvc, xsvcst, znodsvc, bmin, binit, bmax, xregsvc, vregsvc, qsvsch));
        }
    }

    //simple cut-name mapping for DC node names
    //prefixes esg id with DC_
    private String getDCNodeName(String iidmId, int idlen, BiMap<String, String> dcNodesEsgNames) {
        if (dcNodesEsgNames.containsKey(iidmId)) {
            return dcNodesEsgNames.get(iidmId);
        } else {
            String esgId = "DC_" + (iidmId.length() > idlen ? iidmId.substring(0, idlen)
                    : Strings.padEnd(iidmId, idlen, ' '));
            //deal with forbidden characters
//            if (regex != null) {
//                esgId = esgId.replaceAll(regex, repl);
//            }
            int counter = 0;
            while (dcNodesEsgNames.inverse().containsKey(esgId)) {
                String counterStr = Integer.toString(counter++);
                if (counterStr.length() > idlen) {
                    throw new RuntimeException("Renaming fatal error " + iidmId + " -> " + esgId);
                }
                esgId = esgId.substring(0, idlen - counterStr.length()) + counterStr;
            }
            dcNodesEsgNames.put(iidmId, esgId);
            return esgId;
        }
    }

    private void createDCNodes(EsgNetwork esgNetwork, BiMap<String, String> dcNodesEsgNames) {
        //creates 2 DC nodes, for each hvdc line (one node per converter station)
        for (HvdcLine hvdcLine : Identifiables.sort(network.getHvdcLines())) {
            Esg8charName hvdcNodeName1 = new Esg8charName(getDCNodeName(hvdcLine.getConverterStation1().getId(), 5, dcNodesEsgNames));
            Esg8charName hvdcNodeName2 = new Esg8charName(getDCNodeName(hvdcLine.getConverterStation2().getId(), 5, dcNodesEsgNames));
            esgNetwork.addDCNode(new EsgDCNode(new Esg2charName("DC"), hvdcNodeName1, hvdcLine.getNominalV(), 1));
            esgNetwork.addDCNode(new EsgDCNode(new Esg2charName("DC"), hvdcNodeName2, hvdcLine.getNominalV(), 1));

            //create a dc link, representing the hvdc line
            esgNetwork.addDCLink(new EsgDCLink(hvdcNodeName1, hvdcNodeName2, '1', hvdcLine.getR(), EsgDCLink.LinkStatus.ON));
        }
    }

    private boolean isPMode(HvdcConverterStation vscConv, HvdcLine hvdcLine) {
        Objects.requireNonNull(vscConv);
        Objects.requireNonNull(hvdcLine);
        HvdcConverterStation side1Conv = hvdcLine.getConverterStation1();
        HvdcConverterStation side2Conv = hvdcLine.getConverterStation2();
        if ((hvdcLine.getConvertersMode().equals(HvdcLine.ConvertersMode.SIDE_1_RECTIFIER_SIDE_2_INVERTER))
                && (vscConv.getId().equals(side1Conv.getId()))) {
            return true;
        }
        if ((hvdcLine.getConvertersMode().equals(HvdcLine.ConvertersMode.SIDE_1_INVERTER_SIDE_2_RECTIFIER))
                && (vscConv.getId().equals(side2Conv.getId()))) {
            return true;
        }
        return false;
    }

    private float zeroIfNanOrValue(float value) {
        return Float.isNaN(value) ? 0 : value;
    }

    private void createACDCVscConverters(EsgNetwork esgNetwork, BiMap<String, String> dcNodesEsgNames) {
        for (VscConverterStation vscConv : Identifiables.sort(network.getVscConverterStations())) {
            //hvdc line connected to this converter station
            HvdcLine hline = network.getHvdcLineStream().filter(l -> ((vscConv.getId().equals(l.getConverterStation1().getId())) || (vscConv.getId().equals(l.getConverterStation2().getId())))).findFirst().orElse(null);
            Objects.requireNonNull(hline, "no hvdc line connected to VscConverterStation " + vscConv.getId());
            boolean isPmode = isPMode(vscConv, hline);

            Esg8charName znamsvc = new Esg8charName(dictionary.getEsgId(vscConv.getId())); // converter station ID
            Esg8charName dcNode1 = new Esg8charName(getDCNodeName(vscConv.getId(), 5, dcNodesEsgNames)); // sending DC node name
            Esg8charName dcNode2 = new Esg8charName("GROUND"); // receiving DC node name; is it always GROUND?
            Esg8charName acNode = new Esg8charName(dictionary.getEsgId(ConnectionBus.fromTerminal(vscConv.getTerminal(), config, fakeNodes).getId())); // AC node name
            EsgACDCVscConverter.ConverterState xstate = EsgACDCVscConverter.ConverterState.ON; // converter state ' ' ON; 'S' OFF
            EsgACDCVscConverter.DCControlMode xregl = isPmode ? EsgACDCVscConverter.DCControlMode.AC_ACTIVE_POWER : EsgACDCVscConverter.DCControlMode.DC_VOLTAGE; // DC control mode 'P' AC_ACTIVE_POWER; 'V' DC_VOLTAGE
            //AC control mode assumed to be "AC reactive power"(Q)
            EsgACDCVscConverter.ACControlMode xoper = EsgACDCVscConverter.ACControlMode.AC_REACTIVE_POWER; // AC control mode 'V' AC_VOLTAGE; 'Q' AC_REACTIVE_POWER; 'A' AC_POWER_FACTOR
            float rrdc = 0; // resistance [Ohms]
            float rxdc = 16; // reactance [Ohms]

            float pac = zeroIfNanOrValue(hline.getActivePowerSetpoint()); // AC active power setpoint [MW]. Only if DC control mode is 'P'
            pac = isPmode ? pac : -pac; //change sign in case of Q mode side
            float pvd = hline.getNominalV(); // DC voltage setpoint [MW]. Only if DC control mode is 'V'
            float pre = vscConv.getReactivePowerSetpoint(); // AC reactive power setpoint [Mvar]. Only if AC control mode is 'Q'
            if ((Float.isNaN(pre)) || (vscConv.isVoltageRegulatorOn())) {
                float terminalQ = vscConv.getTerminal().getQ();
                if (Float.isNaN(terminalQ)) {
                    pre = zeroIfNanOrValue(vscConv.getReactivePowerSetpoint());
                } else {
                    pre = terminalQ;
                }
            }
            pre = isPmode ? -pre : pre; // change sign in case of P mode side
            float pco = Float.NaN; // AC power factor setpoint. Only if AC control mode is 'A'
            float qvscsh = 1; // Reactive sharing cofficient [%]. Only if AC control mode is 'V'
            float pvscmin = -hline.getMaxP(); // Minimum AC active power [MW]
            float pvscmax = hline.getMaxP(); // Maximum AC active power [MW]
            float qvscmin = vscConv.getReactiveLimits().getMinQ(0); // Minimum reactive power injected on AC node [kV]
            float qvscmax = vscConv.getReactiveLimits().getMaxQ(0); // Maximum reactive power injected on AC node [kV]
            float vsb0 = vscConv.getLossFactor(); // Losses coefficient Beta0 [MW]
            float vsb1 = 0; // Losses coefficient Beta1 [kW]
            float vsb2 = 0; // Losses coefficient Beta2 [Ohms]

            Bus connectedBus = vscConv.getTerminal().getBusBreakerView().getConnectableBus();
            if (connectedBus == null) {
                connectedBus = vscConv.getTerminal().getBusView().getConnectableBus();
                if (connectedBus == null) {
                    throw new RuntimeException("VSCConverter " + vscConv.getId() + " : connected bus not found!");
                }
            }
            float mvm = connectedBus.getV() / connectedBus.getVoltageLevel().getNominalV(); // Initial AC modulated voltage magnitude [p.u.]
            float mva = connectedBus.getAngle(); // Initial AC modulated voltage angle [deg]
            float pva = connectedBus.getV(); // AC voltage setpoint [kV]. Only if AC control mode is 'V'

            esgNetwork.addACDCVscConverter(
                    new EsgACDCVscConverter(
                            znamsvc,
                            dcNode1,
                            dcNode2,
                            acNode,
                            xstate,
                            xregl,
                            xoper,
                            rrdc,
                            rxdc,
                            pac,
                            pvd,
                            pva,
                            pre,
                            pco,
                            qvscsh,
                            pvscmin,
                            pvscmax,
                            qvscmin,
                            qvscmax,
                            vsb0,
                            vsb1,
                            vsb2,
                            mvm,
                            mva));
        }
    }

    public EsgNetwork createNetwork(EsgGeneralParameters parameters) {

        EsgNetwork esgNetwork = new EsgNetwork();

        // areas
        createAreas(esgNetwork);

        // coupling devices
        createCouplingDevices(esgNetwork);

        // lines
        createLines(esgNetwork, parameters);

        // transformers
        createTransformers(esgNetwork, parameters);

        // loads
        createLoads(esgNetwork);

        // generators
        createGenerators(esgNetwork);

        // shunts
        createBanks(esgNetwork);

        // static VAR compensators
        createStaticVarCompensators(esgNetwork);

        //DC nodes mapping
        BiMap<String, String> dcNodesEsgNames = HashBiMap.create();

        // DC Nodes and links
        createDCNodes(esgNetwork, dcNodesEsgNames);

        // ACDC VSC Converters
        createACDCVscConverters(esgNetwork, dcNodesEsgNames);

        // nodes
        createNodes(esgNetwork);

        return esgNetwork;
    }

    private EsgSpecialParameters createEsgSpecialParameters(EurostagEchExportConfig config) {
        return config.isSpecificCompatibility() ? null : new EsgSpecialParameters();
    }

    public void write(Writer writer, EsgGeneralParameters parameters, EsgSpecialParameters specialParameters) throws IOException {
        EsgNetwork esgNetwork = createNetwork(parameters);
        new EsgWriter(esgNetwork, parameters, specialParameters).write(writer, network.getId() + "/" + network.getStateManager().getWorkingStateId());
    }

    public void write(Writer writer) throws IOException {
        write(writer, new EsgGeneralParameters(), createEsgSpecialParameters(config));
    }

    public void write(Path file, EsgGeneralParameters parameters, EsgSpecialParameters specialParameters) throws IOException {
        try (Writer writer = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
            write(writer, parameters, specialParameters);
        }
    }

    public void write(Path file) throws IOException {
        write(file, new EsgGeneralParameters(), createEsgSpecialParameters(config));
    }

}
