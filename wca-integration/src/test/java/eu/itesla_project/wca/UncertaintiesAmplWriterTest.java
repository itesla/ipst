/**
 * Copyright (c) 2017, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package eu.itesla_project.wca;

import static org.junit.Assert.assertEquals;

import java.nio.charset.StandardCharsets;

import org.joda.time.Interval;
import org.junit.Test;

import eu.itesla_project.commons.util.StringToIntMapper;
import eu.itesla_project.iidm.datasource.MemDataSource;
import eu.itesla_project.iidm.export.ampl.AmplSubset;
import eu.itesla_project.iidm.export.ampl.AmplUtil;
import eu.itesla_project.iidm.network.Network;
import eu.itesla_project.iidm.network.test.NetworkTest1Factory;
import eu.itesla_project.modules.wca.Uncertainties;
import eu.itesla_project.modules.wca.UncertaintiesAnalyser;
import eu.itesla_project.wca.uncertainties.UncertaintiesAmplWriter;
import eu.itesla_project.wca.uncertainties.UncertaintiesAnalyserTestImpl;

/**
 *
 * @author Massimo Ferraro <massimo.ferraro@techrain.it>
 */
public class UncertaintiesAmplWriterTest {

    @Test
    public void testWrite() throws Exception {        
        Network network = NetworkTest1Factory.create();
        Interval histoInterval = Interval.parse("2013-01-01T00:00:00+01:00/2013-01-31T23:59:00+01:00");

        UncertaintiesAnalyser uncertaintiesAnalyser = new UncertaintiesAnalyserTestImpl(network);
        Uncertainties uncertainties = uncertaintiesAnalyser.analyse(histoInterval).join();

        MemDataSource dataSource = new MemDataSource();

        StringToIntMapper<AmplSubset> mapper = new StringToIntMapper<>(AmplSubset.class);
        AmplUtil.fillMapper(mapper, network);

        new UncertaintiesAmplWriter(uncertainties, dataSource, mapper).write();

        String fileContent = "#Reduction matrix" + System.lineSeparator()  + 
                             "#\"inj. type\" \"inj. num\" \"var. num\" \"coeff.\"" + System.lineSeparator() +
                             "L 1 1 1.00000" + System.lineSeparator() +
                             "G 1 2 1.00000" + System.lineSeparator();
        assertEquals(fileContent, new String(dataSource.getData(WCAConstants.REDUCTION_MATRIX_FILE_SUFFIX, WCAConstants.TXT_EXT), StandardCharsets.UTF_8));

        fileContent = "#Trust intervals" + System.lineSeparator()  + 
                      "#\"var. num\" \"min\" \"max\"" + System.lineSeparator() +
                      "1 -1.00000 1.00000" + System.lineSeparator() +
                      "2 -90.0000 90.0000" + System.lineSeparator();
        assertEquals(fileContent, new String(dataSource.getData(WCAConstants.TRUST_INTERVAL_FILE_SUFFIX, WCAConstants.TXT_EXT), StandardCharsets.UTF_8));

        fileContent = "#Means" + System.lineSeparator()  + 
                      "#\"inj. type\" \"inj. num\" \"mean\"" + System.lineSeparator() +
                      "L 1 10.0000" + System.lineSeparator() +
                      "G 1 900.000" + System.lineSeparator();
        assertEquals(fileContent, new String(dataSource.getData(WCAConstants.MEANS_FILE_SUFFIX, WCAConstants.TXT_EXT), StandardCharsets.UTF_8));
    }

}
