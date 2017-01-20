package eu.itesla_project.wca;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.zip.GZIPInputStream;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import com.google.common.io.CharStreams;

import eu.itesla_project.iidm.network.Network;
import eu.itesla_project.iidm.network.test.NetworkTest1Factory;

public class WCAUtilsTest {
    
    @Rule
    public TemporaryFolder testFolder = new TemporaryFolder();
    
    @Test
    public void testExportState() throws IOException, URISyntaxException {
        Path folder = testFolder.newFolder().toPath();
        Network network = NetworkTest1Factory.create();
        network.setCaseDate(new DateTime(1483228800000l).withZone(DateTimeZone.UTC));
        WCAUtils.exportState(network, folder, 0, 0);
        File exportedState = Paths.get(folder.toString(), network.getId() + "_0_0.xiidm.gz").toFile();
        assertTrue(exportedState.exists());
        File expectedState = new File(getClass().getResource("/network1_0_0.xiidm.gz").toURI());
        assertEquals(CharStreams.toString(new InputStreamReader(new GZIPInputStream(new FileInputStream(expectedState)))), 
                CharStreams.toString(new InputStreamReader(new GZIPInputStream(new FileInputStream(exportedState)))));
    }

}
