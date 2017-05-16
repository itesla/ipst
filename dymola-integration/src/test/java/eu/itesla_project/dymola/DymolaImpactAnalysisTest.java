package eu.itesla_project.dymola;

import com.google.common.jimfs.Configuration;
import com.google.common.jimfs.Jimfs;
import eu.itesla_project.commons.io.WorkingDirectory;
import eu.itesla_project.computation.*;
import eu.itesla_project.computation.local.LocalComputationConfig;
import eu.itesla_project.contingency.mock.ContingenciesProviderFactoryMock;
import eu.itesla_project.iidm.network.*;
import eu.itesla_project.iidm.network.test.EurostagTutorialExample1Factory;
import net.java.truevfs.comp.zip.ZipFile;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.ForkJoinPool;

import static org.junit.Assert.assertTrue;



/**
 *
 */
@Ignore
public class DymolaImpactAnalysisTest {


    /** */
    private static final Logger LOGGER = LoggerFactory.getLogger(DymolaImpactAnalysisTest.class);
    final private static String BASE_WORKING_DIR  = "/test";

    /**
     * return true if archive contain the expected files
     * @param workingDir working directory
     * @param zipFileName archive
     * @param expectedFileList expected files inside archive
     */
    private void checkArchive(Path workingDir, String zipFileName, String[] expectedFileList) {
        //
        LOGGER.debug(" archive: " + zipFileName);

        assertTrue(String.format("missing archive %s/%s", workingDir, zipFileName), Files.exists(workingDir.resolve(zipFileName)));

        //
        try (ZipFile zipFile = new ZipFile(workingDir.resolve(zipFileName))) {
            if (expectedFileList!=null) {
                for(String fileName : expectedFileList) {
                    net.java.truevfs.comp.zip.ZipEntry ze = zipFile.entry(fileName);
                    assertTrue(String.format("in archive %s/%s missing file %s ", workingDir, zipFileName, fileName), ze != null && ze.getSize() > 0);
                }
            } else {
                LOGGER.debug(" archive 2: " + zipFileName+ " "+zipFile.size());
                Enumeration enumEntries = zipFile.entries();
                while (enumEntries.hasMoreElements()) {
                    net.java.truevfs.comp.zip.ZipEntry ze = (net.java.truevfs.comp.zip.ZipEntry) enumEntries.nextElement();
                    LOGGER.debug("    - " + ze.getName()+ " "+ze.getSize());
                }
            }

        } catch(IOException e) {
            LOGGER.error(e.getMessage());
        }

    }

    @Test
    public void test01() throws Exception {
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
                List<ExecutionError> errors = new ArrayList<>();

                // abort badly execution
                errors.add(new ExecutionError(commandExecutionList.get(0).getCommand(), 0, 0));

                Command command = commandExecutionList.get(0).getCommand();
                for (InputFile file : command.getInputFiles(Integer.toString(0))) {
                    Path path = workingDir.resolve(file.getName());
                    if (!Files.exists(path)) {
                        // if not check if the file exists in the common directory
                        path = commonDir.toPath().resolve(file.getName());
                        if (!Files.exists(path)) {
                            throw new RuntimeException("Input file '" + file.getName() + "' not found in the working and common directory");
                        }
                    }
                }


                    // assert archive production
                //checkArchive( commonDir.toPath(), initHistoZipFileName, initHistoFiles);
                //checkArchive( commonDir.toPath(), initModelZipFileName, initModelFiles);
                //checkArchive( commonDir.toPath(), initTopoModelZipFileName, initTopoModelFiles);

                //
                //checkArchive( workingDir, networkAllFileName, networkAllFiles);

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

        // create a test network
        Network network = EurostagTutorialExample1Factory.create();





        //
        LOGGER.debug("merging");
        try (ComputationManager computationManager = new MyLocalComputationManager(config)) {

                DymolaConfig dc = new DymolaConfig(
                        null,
                        null,
            2*60, 2*60,
            false,
                        null,
                        null,
                        null,
                        "/tmp",
            true,
                        null,
                        null,
                        null);



                DymolaImpactAnalysis dymolaImpactAnalysis = new DymolaImpactAnalysis(
                    network,
                    computationManager,
                    0,
                    new ContingenciesProviderFactoryMock().create(),
                        dc
                );

            //
            //dymolaImpactAnalysis.init(parameters, topologyContext);

            // call otimizer
            dymolaImpactAnalysis.run( new DymolaState("InitialState"));

        } catch(RuntimeException re) {
            re.printStackTrace();
        }


    }


}
