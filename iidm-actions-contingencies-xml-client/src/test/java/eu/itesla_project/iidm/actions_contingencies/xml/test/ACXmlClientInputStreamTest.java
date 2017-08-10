package eu.itesla_project.iidm.actions_contingencies.xml.test;

import static org.junit.Assert.assertNotNull;

import java.io.IOException;

import org.junit.Test;

import eu.itesla_project.iidm.actions_contingencies.xml.XmlFileContingenciesAndActionsDatabaseClientFactory;
import eu.itesla_project.modules.contingencies.ContingenciesAndActionsDatabaseClient;

public class ACXmlClientInputStreamTest {

    @Test
    public void test() throws IOException {

        ContingenciesAndActionsDatabaseClient cadbClient = new XmlFileContingenciesAndActionsDatabaseClientFactory()
                .create(getClass().getResourceAsStream("/test-ac.xml"));
        assertNotNull(cadbClient);
    }

}
