package eu.itesla_project.eurostag;

import com.google.common.jimfs.Configuration;
import com.google.common.jimfs.Jimfs;
import eu.itesla_project.commons.io.WorkingDirectory;
import eu.itesla_project.computation.*;
import eu.itesla_project.computation.local.LocalComputationConfig;
import eu.itesla_project.contingency.ContingenciesProvider;
import eu.itesla_project.contingency.Contingency;
import eu.itesla_project.contingency.ContingencyElement;
import eu.itesla_project.contingency.ContingencyElementType;
import eu.itesla_project.contingency.tasks.ModificationTask;
import eu.itesla_project.iidm.eurostag.export.BranchParallelIndexes;
import eu.itesla_project.iidm.eurostag.export.EurostagDictionary;
import eu.itesla_project.iidm.eurostag.export.EurostagEchExportConfig;
import eu.itesla_project.iidm.network.*;
import eu.itesla_project.iidm.network.test.EurostagTutorialExample1Factory;
import eu.itesla_project.simulation.SimulationParameters;
import net.java.truevfs.access.TPath;
import net.java.truevfs.comp.zip.ZipEntry;
import net.java.truevfs.comp.zip.ZipFile;
import net.java.truevfs.comp.zip.ZipOutputStream;
import org.joda.time.Interval;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.ForkJoinPool;
import java.util.function.Function;

import static org.junit.Assert.assertTrue;

/**
 */
public class EurostagScenarioTest {


    /** */
    private static final Logger LOGGER = LoggerFactory.getLogger(EurostagScenarioTest.class);

    /** */
    final private static String BASE_WORKING_DIR  = "/test";

    /** */
    String FAULT_SEQ_FILE_NAME = "sim_fault_" + Command.EXECUTION_NUMBER_PATTERN + ".seq";


    /**
     *
     * @throws IOException
     */
    @Before
    public void setUp() throws IOException {
    }

    /**
     *
     * @throws IOException
     */
    @After
    public void tearDown() throws IOException {
    }

    private void checkArchive(Path workingDir, String zipFileName, String[] expectedFileList) {
        //
        LOGGER.debug(" check archive: " + zipFileName);

        assertTrue(String.format("missing archive %s/%s", workingDir, zipFileName), Files.exists(workingDir.resolve(zipFileName)));

        //
        try (ZipFile zipFile = new ZipFile(workingDir.resolve(zipFileName))) {
            if (expectedFileList!=null) {
                for(String fileName : expectedFileList) {
                    ZipEntry ze = zipFile.entry(fileName);
                    assertTrue(String.format("in archive %s/%s missing file %s ", workingDir, zipFileName, fileName), ze != null && ze.getSize() > 0);
                }
            } else {
                Enumeration enumEntries = zipFile.entries();
                while (enumEntries.hasMoreElements()) {
                    ZipEntry ze = (ZipEntry) enumEntries.nextElement();
                    LOGGER.debug("    - " + ze.getName()+ " "+ze.getSize());
                }
            }

        } catch(IOException e) {
            LOGGER.error(e.getMessage());
        }
    }


    /**
     *
     * @throws Exception
     */
    @Test
    public void test01() throws Exception {

        // create a test network
        Network network = EurostagTutorialExample1Factory.create();

        // expected content for the historic archive
        final String testZipFileName = "test.zip";
        final String[] testFiles = new String[]{
                "/sim_fault_0.seq"
        };

        // create a virtual file system
        FileSystem fs = Jimfs.newFileSystem(Configuration.unix());

        // create working directory
        Path workingDir = fs.getPath(BASE_WORKING_DIR);
        Files.createDirectory(workingDir);

        //
        SimulationParameters simulationParameters = new SimulationParameters(
                0.1D, 0.2D, 0.5D, 0.01D, 0.21D
        );

        EurostagEchExportConfig eurostagEchExportConfig = new EurostagEchExportConfig();

        EurostagConfig ec = new EurostagConfig();
        EurostagScenario eurostagScenario = new EurostagScenario(simulationParameters, ec);

        List<Contingency> l = new ArrayList<>();

        Map<String, EurostagDictionary> m = new HashMap<String, EurostagDictionary>();
        m.put("dictionary", EurostagDictionary.create(network, BranchParallelIndexes.build(network, eurostagEchExportConfig), eurostagEchExportConfig));



        ContingenciesProvider contingenciesProvider =new ContingenciesProvider() {
            @Override
            public List<Contingency> getContingencies(Network network) {
                List<Contingency> l = new ArrayList<>();
                l.add(new Contingency() {
                    @Override
                    public String getId() {
                        return null;
                    }

                    @Override
                    public Collection<ContingencyElement> getElements() {
                        List<ContingencyElement> l2 = new ArrayList<>();
                        l2.add(new ContingencyElement() {
                            @Override
                            public String getId() {
                                return "GEN";
                            }

                            @Override
                            public ContingencyElementType getType() {
                                return ContingencyElementType.GENERATOR;
                            }

                            @Override
                            public ModificationTask toTask() {
                                return null;
                            }
                        });
                        return l2;
                    }

                    @Override
                    public ModificationTask toTask() {
                        return null;
                    }
                });
                return l;
            }
        };

        //
        try (OutputStream o = Files.newOutputStream(workingDir.resolve("test.zip"));
             OutputStream os = new ZipOutputStream( o )
        ) {
            eurostagScenario.writeFaultSeqArchive(
                    os,
                    contingenciesProvider.getContingencies(network),
                    network,
                    m.get("dictionary"),
                    faultNum -> FAULT_SEQ_FILE_NAME.replace(Command.EXECUTION_NUMBER_PATTERN, Integer.toString(faultNum))
            );
        }

        //
        checkArchive( workingDir, "test.zip", testFiles);
        //checkArchive( workingDir, "test.zip", null);

    }


}
