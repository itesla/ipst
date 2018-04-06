/**
 * Copyright (c) 2016, All partners of the iTesla project (http://www.itesla-project.eu/consortium)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package eu.itesla_project.iidm.eurostag.export;

import com.google.common.io.CharStreams;
import com.powsybl.iidm.network.Bus;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.TopologyKind;
import com.powsybl.iidm.network.VoltageLevel;
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

    private void test(Network network, String reference, LocalDate editDate, EsgSpecialParameters specialParameters) throws IOException {
        StringWriter writer = new StringWriter();
        EsgGeneralParameters parameters = new EsgGeneralParameters();
        parameters.setEditDate(editDate);
        new EurostagEchExport(network).write(writer, parameters, specialParameters);
        writer.close();
        assertEquals(CharStreams.toString(new InputStreamReader(getClass().getResourceAsStream(reference), StandardCharsets.UTF_8)), writer.toString());
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
                .setTargetP(100.0F)
                .setTargetV(400.0F)
                .setMinP(50.0F)
                .setMaxP(150.0F)
                .add();
        EsgSpecialParameters specialParameters = new EsgSpecialParameters();
        test(network, "/eurostag-hvdc-test.ech", LocalDate.parse("2016-01-01"), specialParameters);
    }

    private void addLine(Network network, VoltageLevel vlhv1, VoltageLevel vlhv2, String idLine, float g1, float g2, float b1, float b2) {
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

        float bTest1 = EurostagEchExport.B_EPSILON * 2f;

        //G and B are the same on each side, G are 0
        addLine(network, vlhv1, vlhv2, "L1", 0f, 0f, 0f, 0f);
        addLine(network, vlhv1, vlhv2, "L2", 0f, 0f, bTest1, bTest1);
        //B are not the same, G are 0
        addLine(network, vlhv1, vlhv2, "L3", 0f, 0f, bTest1, 0); // dummy shunt expected in the .ech
        addLine(network, vlhv1, vlhv2, "L4", 0f, 0f, 0, bTest1); // dummy shunt expected in the .ech
        addLine(network, vlhv1, vlhv2, "L5", 0f, 0f, EurostagEchExport.B_EPSILON / 2f, 0);
        addLine(network, vlhv1, vlhv2, "L6", 0f, 0f, 0, EurostagEchExport.B_EPSILON / 2f);
        //B are not the same, G are not 0
        addLine(network, vlhv1, vlhv2, "L7", 1f, 0f, bTest1, 0);
        addLine(network, vlhv1, vlhv2, "L8", 0f, 1f, bTest1, 0);

        EsgSpecialParameters specialParameters = new EsgSpecialParameters();
        test(network, "/eurostag-tutorial-example1_lines.ech", LocalDate.parse("2016-03-01"), specialParameters);
    }

}