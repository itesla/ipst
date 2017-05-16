package eu.itesla_project.iidm.ddb.eurostag;

import com.google.common.jimfs.Configuration;
import com.google.common.jimfs.Jimfs;
import eu.itesla_project.commons.io.WorkingDirectory;
import eu.itesla_project.computation.*;
import eu.itesla_project.computation.local.LocalComputationConfig;
import eu.itesla_project.iidm.network.Network;
import eu.itesla_project.iidm.network.test.EurostagTutorialExample1Factory;
import org.junit.Ignore;
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

/**
 *
 */
@Ignore
public class EurostagStepUpTransformerPostProcessorTest {
    /** */
    private static final Logger LOGGER = LoggerFactory.getLogger(EurostagStepUpTransformerPostProcessorTest.class);

    /** */
    final private static String BASE_WORKING_DIR  = "/test";

    @Test
    public void test01() throws Exception {
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

                // assert archive production
                //checkArchive( workingDir, initTopoHistoZipFileName, initTopoHistoFiles);

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

        LocalComputationConfig config = new LocalComputationConfig(workingDir);

        // create a test network
        Network network = EurostagTutorialExample1Factory.create();


        try (ComputationManager computationManager = new MyLocalComputationManager(config)) {
            EurostagStepUpTransformerPostProcessor eurostagStepUpTransformerPostProcessor = new EurostagStepUpTransformerPostProcessor();


            eurostagStepUpTransformerPostProcessor.process(network, computationManager);
        }

    }
}
