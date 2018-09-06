/**
 * Copyright (c) 2016, All partners of the iTesla project (http://www.itesla-project.eu/consortium)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package eu.itesla_project.iidm.eurostag.export;

import com.google.common.io.CharStreams;
import com.powsybl.iidm.network.*;
import com.powsybl.iidm.network.test.EurostagTutorialExample1Factory;
import com.powsybl.iidm.network.test.HvdcTestNetwork;
import com.powsybl.iidm.network.test.SvcTestCaseFactory;
import eu.itesla_project.eurostag.network.EsgGeneralParameters;
import eu.itesla_project.eurostag.network.EsgSpecialParameters;
import org.joda.time.LocalDate;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;

import static org.junit.Assert.assertEquals;

/**
 * @author Geoffroy Jamgotchian <geoffroy.jamgotchian at rte-france.com>
 */
public class EurostagEchExportTest {

    private void test(Network network, String reference, LocalDate editDate, EsgSpecialParameters specialParameters, EurostagEchExportConfig config) throws IOException {
        StringWriter writer = new StringWriter();
        EsgGeneralParameters parameters = new EsgGeneralParameters();
        parameters.setEditDate(editDate);
        new EurostagEchExport(network, config).write(writer, parameters, specialParameters);
        writer.close();
        assertEquals(CharStreams.toString(new InputStreamReader(getClass().getResourceAsStream(reference), StandardCharsets.UTF_8)), writer.toString());
    }

    private void test(Network network, String reference, LocalDate editDate, EsgSpecialParameters specialParameters) throws IOException {
        test(network, reference, editDate, specialParameters, new EurostagEchExportConfig());
    }


    @Test
    public void test() throws IOException {
        Network network = EurostagTutorialExample1Factory.create();
        EsgSpecialParameters specialParameters = new EsgSpecialParameters();
        test(network, "/eurostag-tutorial-example1.ech", LocalDate.parse("2016-03-01"), specialParameters);
    }

    @Test
    public void testNoSpecialParameters() throws IOException {
        Network network = EurostagTutorialExample1Factory.create();
        test(network, "/eurostag-tutorial-example1_no_special.ech", LocalDate.parse("2016-03-01"), null);
    }

    @Test
    public void testSVC() throws IOException {
        Network network = SvcTestCaseFactory.create();
        EsgSpecialParameters specialParameters = new EsgSpecialParameters();
        test(network, "/eurostag-svc-test.ech", LocalDate.parse("2016-01-01"), specialParameters);
    }

    @Test
    public void testHVDC() throws IOException {
        Network network = HvdcTestNetwork.createVsc();
        network.getVoltageLevelStream().findFirst().orElse(null)
                .newGenerator().setId("G1")
                .setConnectableBus("B1")
                .setBus("B1")
                .setVoltageRegulatorOn(true)
                .setTargetP(100.0)
                .setTargetV(400.0)
                .setMinP(50.0)
                .setMaxP(150.0)
                .add();
        EsgSpecialParameters specialParameters = new EsgSpecialParameters();
        test(network, "/eurostag-hvdc-test.ech", LocalDate.parse("2016-01-01"), specialParameters);
    }

    private void addLine(Network network, VoltageLevel vlhv1, VoltageLevel vlhv2, String idLine, double g1, double g2, double b1, double b2) {
        Bus nhv1 = vlhv1.getBusBreakerView().newBus()
                .setId("N1" + idLine)
                .add();
        Bus nhv2 = vlhv2.getBusBreakerView().newBus()
                .setId("N2" + idLine)
                .add();
        network.newLine()
                .setId(idLine)
                .setVoltageLevel1(vlhv1.getId())
                .setBus1(nhv1.getId())
                .setConnectableBus1(nhv1.getId())
                .setVoltageLevel2(vlhv2.getId())
                .setBus2(nhv2.getId())
                .setConnectableBus2(nhv2.getId())
                .setR(3)
                .setX(33)
                .setG1(g1)
                .setB1(b1)
                .setG2(g2)
                .setB2(b2)
                .add();
    }

    @Test
    public void testLines() throws IOException {
        Network network = EurostagTutorialExample1Factory.create();
        VoltageLevel vlhv1 = network.getSubstation("P1").newVoltageLevel()
                .setId("VL1")
                .setNominalV(380)
                .setTopologyKind(TopologyKind.BUS_BREAKER)
                .add();
        VoltageLevel vlhv2 = network.getSubstation("P2").newVoltageLevel()
                .setId("VL2")
                .setNominalV(380)
                .setTopologyKind(TopologyKind.BUS_BREAKER)
                .add();

        double bTest1 = EurostagEchExport.B_EPSILON * 2;

        //G and B are the same on each side, G are 0
        addLine(network, vlhv1, vlhv2, "L1", 0, 0, 0, 0);
        addLine(network, vlhv1, vlhv2, "L2", 0, 0, bTest1, bTest1);
        //B are not the same, G are 0
        addLine(network, vlhv1, vlhv2, "L3", 0, 0, bTest1, 0); // dummy shunt expected in the .ech
        addLine(network, vlhv1, vlhv2, "L4", 0, 0, 0, bTest1); // dummy shunt expected in the .ech
        addLine(network, vlhv1, vlhv2, "L5", 0, 0, EurostagEchExport.B_EPSILON / 2, 0);
        addLine(network, vlhv1, vlhv2, "L6", 0, 0, 0, EurostagEchExport.B_EPSILON / 2);
        //B are not the same, G are not 0
        addLine(network, vlhv1, vlhv2, "L7", 1, 0, bTest1, 0);
        addLine(network, vlhv1, vlhv2, "L8", 0, 1, bTest1, 0);

        EsgSpecialParameters specialParameters = new EsgSpecialParameters();
        test(network, "/eurostag-tutorial-example1_lines.ech", LocalDate.parse("2016-03-01"), specialParameters);
    }


    @Test
    public void testCptRpt() throws IOException {
        Network network = EurostagTutorialExample1Factory.create();
        TwoWindingsTransformer nhv2Nload = network.getTwoWindingsTransformer("NHV2_NLOAD");

        //adds a phase tap changer to the NHV2_NLOAD TwoWindingTransformer (it has already a ratio tap changer)
        //so that both tap changers adjust rho
        nhv2Nload.newPhaseTapChanger()
                .setTapPosition(0)
                .setRegulationTerminal(nhv2Nload.getTerminal2())
                .setRegulationMode(PhaseTapChanger.RegulationMode.FIXED_TAP)
                .setRegulationValue(200)
                .beginStep()
                .setAlpha(-20)
                .setRho(0.96)
                .setR(0)
                .setX(0)
                .setG(0)
                .setB(0)
                .endStep()
                .add();

        EsgSpecialParameters specialParameters = new EsgSpecialParameters();
        test(network, "/eurostag-tutorial-example1_cptrpt.ech", LocalDate.parse("2016-03-01"), specialParameters);
    }

    @Test
    public void testVoltageRegulationException() throws IOException {
        Network network = EurostagTutorialExample1Factory.create();

        //voltage regulator exception:  if specificCompatibility && (g.getTargetP() < 0.0001) && (g.getMinP() > 0.0001)
        // turn off g's voltage regulation
        Generator g = network.getGenerator("GEN");
        g.setMinP(300);
        g.setTargetP(0);

        EsgSpecialParameters specialParameters = new EsgSpecialParameters();
        EurostagEchExportConfig exTemp = new EurostagEchExportConfig();
        EurostagEchExportConfig exportConfig= new EurostagEchExportConfig(exTemp.isNoGeneratorMinMaxQ(), exTemp.isNoSwitch(), exTemp.getForbiddenCharactersString(),
                exTemp.getForbiddenCharactersReplacement(), exTemp.isSvcAsFixedInjectionInLF(), true, exTemp.isExportMainCCOnly());

        test(network, "/eurostag-tutorial-example1_vre.ech", LocalDate.parse("2016-03-01"), specialParameters, exportConfig);
    }

}