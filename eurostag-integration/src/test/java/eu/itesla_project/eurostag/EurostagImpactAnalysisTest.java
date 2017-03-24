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
import net.java.truevfs.comp.zip.ZipEntry;
import net.java.truevfs.comp.zip.ZipFile;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.ForkJoinPool;

import static org.junit.Assert.assertTrue;

/**
 *
 */
public class EurostagImpactAnalysisTest {


    /** */
    private static final Logger LOGGER = LoggerFactory.getLogger(EurostagImpactAnalysisTest.class);

    /** */
    final private static String BASE_WORKING_DIR  = "/test";

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

    /**
     * return true if archive contain the expected files
     * @param workingDir
     * @param zipFileName
     * @param expectedFileList
     */
    private void checkArchive(Path workingDir, String zipFileName, String[] expectedFileList) {
        //
        LOGGER.debug(" archive: " + zipFileName);

        assertTrue(String.format("missing archive %s/%s", workingDir, zipFileName), Files.exists(workingDir.resolve(zipFileName)));

        //
        try (ZipFile zipFile = new ZipFile(workingDir.resolve(zipFileName))) {
            if (expectedFileList!=null) {
                    for(String fileName : expectedFileList) {
                        ZipEntry ze = zipFile.entry(fileName);
                        assertTrue(String.format("in archive %s/%s missing file %s ", workingDir, zipFileName, fileName), ze != null && ze.getSize() > 0);
                    }
            } else {
                LOGGER.debug(" archive 2: " + zipFileName+ " "+zipFile.size());
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
        final String initHistoZipFileName = "init-histo.zip";
        final String[] initHistoFiles = new String[]{
                "ampl_histo_loads.txt",
                "ampl_histo_generators.txt",
                "ampl_histo_branches.txt",
                "ampl_histo_substations.txt"
        };


        /**
         * mock ComputationManager
         */
        class MyLocalComputationManager implements ComputationManager {

            private final LocalComputationConfig config;

            private final WorkingDirectory commonDir;

            public MyLocalComputationManager(LocalComputationConfig config) throws IOException {
                this.config = Objects.requireNonNull(config);
                //make sure the localdir exists
                Files.createDirectories(config.getLocalDir());
                commonDir = new WorkingDirectory(config.getLocalDir(), "itesla_common_", false);
                LOGGER.info(config.toString());
            }

            @Override
            public String getVersion() {
                return "none (local mode)";
            }

            @Override
            public Path getLocalDir() {
                return config.getLocalDir();
            }

            @Override
            public OutputStream newCommonFile(String fileName) throws IOException {
                return Files.newOutputStream(commonDir.toPath().resolve(fileName));
            }

            @Override
            public CommandExecutor newCommandExecutor(Map<String, String> env, String workingDirPrefix, boolean debug) throws Exception {
                Objects.requireNonNull(env);
                Objects.requireNonNull(workingDirPrefix);

                WorkingDirectory workingDir = new WorkingDirectory(config.getLocalDir(), workingDirPrefix, debug);

                return new CommandExecutor() {
                    @Override
                    public Path getWorkingDir() {
                        return workingDir.toPath();
                    }

                    @Override
                    public void start(CommandExecution execution, ExecutionListener listener) throws Exception {
                    }

                    @Override
                    public ExecutionReport start(CommandExecution execution) throws Exception {
                        try {
                            return execute(workingDir.toPath(), Arrays.asList(execution));
                        } finally {
                        }
                    }

                    @Override
                    public void close() throws Exception {
                        workingDir.close();
                    }
                };
            }

            private ExecutionReport execute(Path workingDir, List<CommandExecution> commandExecutionList) throws IOException, InterruptedException {

                //
                System.out.println("-------------------------");
                Files.list(workingDir).filter(Files::isRegularFile).forEach(file -> {
                    try {
                        System.out.println(String.format("%s (%db)", file, Files.readAllBytes(file).length));
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });

                Files.list(commonDir.toPath()).filter(Files::isRegularFile).forEach(file -> {
                    try {
                        System.out.println(String.format("%s (%db)", file, Files.readAllBytes(file).length));
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });
                System.out.println("-------------------------");

                //
                List<ExecutionError> errors = new ArrayList<>();

                // abort badly execution
                errors.add(new ExecutionError(commandExecutionList.get(0).getCommand(), 0, 0));

                // assert archive production
                //checkArchive( workingDir, "sim_pre_fault.sac.gz", null);
                checkArchive( commonDir.toPath(), "eurostag-all-scenarios.zip", null);
                checkArchive( commonDir.toPath(),"eurostag-limits.zip", null);
                checkArchive( commonDir.toPath(), "wp43-all-configs.zip", null);

                //
                return new ExecutionReport(errors);
            }

            @Override
            public <R> CompletableFuture<R> execute(ExecutionEnvironment environment, ExecutionHandler<R> handler) {
                return null;
            }

            @Override
            public ComputationResourcesStatus getResourcesStatus() {
                return null; //status;
            }

            @Override
            public Executor getExecutor() {
                return ForkJoinPool.commonPool();
            }

            @Override
            public void close() throws IOException {
                commonDir.close();
            }

        }

        // create a virtual file system
        FileSystem fs = Jimfs.newFileSystem(Configuration.unix());

        // create working directory
        Path workingDir = fs.getPath(BASE_WORKING_DIR);
        Files.createDirectory(workingDir);

        // create debug dir
        Path debugFolder = fs.getPath("/debug");
        Files.createDirectory(debugFolder);

        //
        LocalComputationConfig config = new LocalComputationConfig(workingDir);

        //
        LOGGER.debug("merging");
        try (ComputationManager computationManager = new MyLocalComputationManager(config)) {

            EurostagConfig ec = new EurostagConfig();

            EurostagImpactAnalysis eurostagImpactAnalysis = new EurostagImpactAnalysis(
                    network,
                    computationManager,
                    0,
                    new ContingenciesProvider() {
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
                    },
                    ec
            );

            //
            SimulationParameters simulationParameters = new SimulationParameters(
                    0.1D, 0.2D, 0.5D, 0.01D, 0.21D
            );

            Map<String, Object> m = new HashMap<String, Object>();
            EurostagEchExportConfig eurostagEchExportConfig = new EurostagEchExportConfig();

            m.put("dictionary", EurostagDictionary.create(network, BranchParallelIndexes.build(network, eurostagEchExportConfig), eurostagEchExportConfig));

            eurostagImpactAnalysis.init(simulationParameters, m);

            // call otimizer
            eurostagImpactAnalysis.run(new EurostagState("InitialState", new byte[1], null));

        } catch(RuntimeException re) {
            re.printStackTrace();
        }

    }



}
