package eu.itesla_project.iidm.actions_contingencies.xml.test;

import static org.junit.Assert.assertNotNull;

import java.io.IOException;
import java.net.URL;

import org.junit.Test;

import eu.itesla_project.iidm.actions_contingencies.xml.XmlFileContingenciesAndActionsDatabaseClientFactory;
import eu.itesla_project.modules.contingencies.ContingenciesAndActionsDatabaseClient;

public class ACXmlClientInputStreamTest {

    @Test
    public void test() throws IOException {

        URL testActionsUrl = getClass().getResource("/test-ac.xml");       
        ContingenciesAndActionsDatabaseClient cadbClient = new XmlFileContingenciesAndActionsDatabaseClientFactory().create(testActionsUrl.openStream());
        assertNotNull(cadbClient);
    }
    
}
