/**
 * Copyright (c) 2016, All partners of the iTesla project (http://www.itesla-project.eu/consortium)
 * Copyright (c) 2017, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package eu.itesla_project.iidm.actions_contingencies.xml;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.Objects;

import javax.xml.bind.JAXBException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

import com.powsybl.commons.config.ConfigurationException;
import com.powsybl.commons.config.ModuleConfig;
import com.powsybl.commons.config.PlatformConfig;
import eu.itesla_project.modules.contingencies.ContingenciesAndActionsDatabaseClient;
import eu.itesla_project.modules.contingencies.ContingenciesAndActionsDatabaseClientFactory;


/**
 * @author Quinary <itesla@quinary.com>
 */
public class XmlFileContingenciesAndActionsDatabaseClientFactory implements
        ContingenciesAndActionsDatabaseClientFactory {

    private static final Logger LOGGER = LoggerFactory.getLogger(XmlFileContingenciesAndActionsDatabaseClientFactory.class);

    @Override
    public ContingenciesAndActionsDatabaseClient create() {
        ModuleConfig config = PlatformConfig.defaultConfig().getModuleConfig("xmlcontingencydb");
        Path xmlFile = config.getPathProperty("xmlFile");
        return create(xmlFile);
    }

    @Override
    public ContingenciesAndActionsDatabaseClient create(Path xmlFile) {
        Objects.requireNonNull(xmlFile);
        try {
            return new XmlFileContingenciesAndActionsDatabaseClient(xmlFile);
        } catch (JAXBException | SAXException | IOException e) {
            LOGGER.error("Error loading input file " + xmlFile, e);
            throw new ConfigurationException(e);
        }
    }

    @Override
    public ContingenciesAndActionsDatabaseClient create(InputStream data) {
        Objects.requireNonNull(data);
        try {
            return new XmlFileContingenciesAndActionsDatabaseClient(data);
        } catch (JAXBException | SAXException | IOException e) {
            LOGGER.error("Error loading input data ", e);
            throw new ConfigurationException(e);
        }
    }
}
