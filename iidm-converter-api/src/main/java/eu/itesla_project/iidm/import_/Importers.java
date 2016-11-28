/**
 * Copyright (c) 2016, All partners of the iTesla project (http://www.itesla-project.eu/consortium)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package eu.itesla_project.iidm.import_;

import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import eu.itesla_project.commons.config.MapModuleConfig;
import eu.itesla_project.computation.ComputationManager;
import eu.itesla_project.computation.local.LocalComputationManager;
import eu.itesla_project.iidm.datasource.*;
import eu.itesla_project.iidm.network.Network;
import eu.itesla_project.iidm.parameters.Parameter;
import eu.itesla_project.iidm.parameters.ParameterDefaultValueConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * A utility class to work with IIDM importers.
 *
 * @author Geoffroy Jamgotchian <geoffroy.jamgotchian at rte-france.com>
 */
public class Importers {

    private static final Logger LOGGER = LoggerFactory.getLogger(Importers.class);

    private static final ImportersLoader LOADER = new ImportersServiceLoader();

    private final static Supplier<ImportConfig> CONFIG = Suppliers.memoize(() -> ImportConfig.load());

    private Importers() {
    }

    /**
     * Get all supported import formats.
     */
    public static Collection<String> getFormats() {
        return LOADER.loadImporters().stream().map(Importer::getFormat).collect(Collectors.toList());
    }

    private static Importer wrapImporter(Importer importer, ComputationManager computationManager, ImportConfig config) {
        Objects.requireNonNull(computationManager);
        Objects.requireNonNull(config);
        List<String> postProcessorNames = config.getPostProcessors();
        if (postProcessorNames != null && postProcessorNames.size() > 0) {
            return new ImporterWrapper(importer, computationManager, postProcessorNames);
        }
        return importer;
    }

    public static Collection<Importer> list(ComputationManager computationManager, ImportConfig config) {
        return LOADER.loadImporters().stream()
                .map(importer -> wrapImporter(importer, computationManager, config))
                .collect(Collectors.toList());
    }

    public static Collection<Importer> list() {
        return list(LocalComputationManager.getDefault(), CONFIG.get());
    }

    /**
     * Get an importer.
     *
     * @param format the import format
     * @return the importer if one exists for the given format or
     * <code>null</code> otherwise.
     */
    public static Importer getImporter(String format, ComputationManager computationManager, ImportConfig config) {
        Objects.requireNonNull(format);
        for (Importer importer : LOADER.loadImporters()) {
            if (format.equals(importer.getFormat())) {
                return wrapImporter(importer, computationManager, config);
            }
        }
        return null;
    }

    public static Importer getImporter(String format, ComputationManager computationManager) {
        return getImporter(format, computationManager, CONFIG.get());
    }

    public static Importer getImporter(String format) {
        return getImporter(format, LocalComputationManager.getDefault());
    }

    public static Collection<String> getPostProcessorNames() {
        return LOADER.loadPostProcessors().stream().map(ImportPostProcessor::getName).collect(Collectors.toList());
    }

    private static ImportPostProcessor getPostProcessor(String name) {
        for (ImportPostProcessor ipp : LOADER.loadPostProcessors()) {
            if (ipp.getName().equals(name)) {
                return ipp;
            }
        }
        throw new RuntimeException("Post processor " + name + " not found");
    }

    private static class ImporterWrapper implements Importer {

        private final Importer importer;

        private final ComputationManager computationManager;

        private final List<String> names;

        ImporterWrapper(Importer importer, ComputationManager computationManager, List<String> names) {
            this.importer = importer;
            this.computationManager = computationManager;
            this.names = names;
        }

        public Importer getImporter() {
            return importer;
        }

        @Override
        public String getFormat() {
            return importer.getFormat();
        }

        @Override
        public InputStream get16x16Icon() {
            return importer.get16x16Icon();
        }

        @Override
        public List<Parameter> getParameters() {
            return importer.getParameters();
        }

        @Override
        public String getComment() {
            return importer.getComment();
        }

        @Override
        public boolean exists(ReadOnlyDataSource dataSource) {
            return importer.exists(dataSource);
        }

        @Override
        public Network import_(ReadOnlyDataSource dataSource, Properties parameters) {
            Network network = importer.import_(dataSource, parameters);
            for (String name : names) {
                try {
                    getPostProcessor(name).process(network, computationManager);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
            return network;
        }
    }

    public static Importer addPostProcessors(Importer importer, ComputationManager computationManager, String... names) {
        return new ImporterWrapper(importer, computationManager, Arrays.asList(names));
    }

    public static Importer addPostProcessors(Importer importer, String... names) {
        return new ImporterWrapper(importer, LocalComputationManager.getDefault(), Arrays.asList(names));
    }

    public static Importer setPostProcessors(Importer importer, ComputationManager computationManager, String... names) {
        Importer importer2 = removePostProcessors(importer);
        addPostProcessors(importer2, computationManager, names);
        return importer2;
    }

    public static Importer setPostProcessors(Importer importer, String... names) {
        return setPostProcessors(importer, LocalComputationManager.getDefault(), names);
    }

    public static Importer removePostProcessors(Importer importer) {
        if (importer instanceof ImporterWrapper) {
            return removePostProcessors(((ImporterWrapper) importer).getImporter());
        }
        return importer;
    }

    /**
     * A convenient method to create a model from data in a given format.
     *
     * @param format the import format
     * @param dataSource data source
     * @param parameters some properties to configure the import
     * @param computationManager computation manager to use for default post processors
     * @return the model
     */
    public static Network import_(String format, ReadOnlyDataSource dataSource, Properties parameters, ComputationManager computationManager) {
        Importer importer = getImporter(format, computationManager);
        if (importer == null) {
            throw new RuntimeException("Import format " + format + " not supported");
        }
        return importer.import_(dataSource, parameters);
    }

    public static Network import_(String format, ReadOnlyDataSource dataSource, Properties parameters) {
        return import_(format, dataSource, parameters, LocalComputationManager.getDefault());
    }

    /**
     * A convenient method to create a model from data in a given format.
     *
     * @param format the import format
     * @param directory the directory where input files are
     * @param baseName a base name for all input files
     * @param parameters some properties to configure the import
     * @return the model
     */
    public static Network import_(String format, String directory, String baseName, Properties parameters) {
        return import_(format, new FileDataSource(Paths.get(directory), baseName), parameters);
    }

    public static void importAll(Path dir, Importer importer, boolean parallel, Consumer<Network> consumer) throws IOException, InterruptedException, ExecutionException {
        importAll(dir, importer, parallel, consumer, null);
    }

    private static void doImport(ReadOnlyDataSource dataSource, Importer importer, Consumer<Network> consumer, Consumer<ReadOnlyDataSource> listener) {
        try {
            if (listener != null) {
                listener.accept(dataSource);
            }
            Network network = importer.import_(dataSource, null);
            consumer.accept(network);
        } catch (Exception e) {
            LOGGER.error(e.toString(), e);
        }
    }

    public static void importAll(Path dir, Importer importer, boolean parallel, Consumer<Network> consumer, Consumer<ReadOnlyDataSource> listener) throws IOException, InterruptedException, ExecutionException {
        List<ReadOnlyDataSource> dataSources = new ArrayList<>();
        importAll(dir, importer, dataSources);
        importAll(dir,importer,parallel,consumer,listener,dataSources);
    }

    public static void importAll(Path dir, Importer importer, boolean parallel, Consumer<Network> consumer, Consumer<ReadOnlyDataSource> listener, List<ReadOnlyDataSource> dataSources) throws IOException, InterruptedException, ExecutionException {
        if (parallel) {
            ExecutorService executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
            try {
                List<Future<?>> futures = dataSources.stream()
                        .map(ds -> executor.submit(() -> doImport(ds, importer, consumer, listener)))
                        .collect(Collectors.toList());
                for (Future<?> future : futures) {
                    future.get();
                }
            } finally {
                executor.shutdownNow();
            }
        } else {
            for (ReadOnlyDataSource dataSource : dataSources) {
                doImport(dataSource, importer, consumer, listener);
            }
        }
    }

    public static String getBaseName(Path file) {
        return getBaseName(file.getFileName().toString());
    }

    public static String getBaseName(String fileName) {
        int pos = fileName.indexOf('.'); // find first dot in case of double extension (.xml.gz)
        return pos == -1 ? fileName : fileName.substring(0, pos);
    }

    private static void addDataSource(Path dir, Path file, Importer importer, List<ReadOnlyDataSource> dataSources) {
        String caseBaseName = getBaseName(file);
        ReadOnlyDataSource ds = new GenericReadOnlyDataSource(dir, caseBaseName);
        if (importer.exists(ds)) {
            dataSources.add(ds);
        }
    }

    private static void importAll(Path parent, Importer importer, List<ReadOnlyDataSource> dataSources) throws IOException {
        if (Files.isDirectory(parent)) {
            try (Stream<Path> stream = Files.list(parent)) {
                stream.sorted().forEach(child -> {
                    if (Files.isDirectory(child)) {
                        try {
                            importAll(child, importer, dataSources);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    } else {
                        addDataSource(parent, child, importer, dataSources);
                    }
                });
            }
        } else {
            if (parent.getParent() != null) {
                addDataSource(parent.getParent(), parent, importer, dataSources);
            }
        }
    }

    public static Object readParameter(String format, Properties parameters, Parameter configuredParameter) {
        Object value = null;
        // priority on import parameter
        if (parameters != null) {
            MapModuleConfig moduleConfig = new MapModuleConfig(parameters);
            switch (configuredParameter.getType()) {
                case BOOLEAN:
                    value = moduleConfig.getOptinalBooleanProperty(configuredParameter.getName());
                    break;
                case STRING:
                    value = moduleConfig.getStringProperty(configuredParameter.getName(), null);
                    break;
                case STRING_LIST:
                    value = moduleConfig.getStringListProperty(configuredParameter.getName(), null);
                    break;
                default:
                    throw new AssertionError();
            }
        }
        // if none, use configured paramaters
        if (value == null) {
            value = ParameterDefaultValueConfig.INSTANCE.getValue(format, configuredParameter);
        }
        return value;
    }

    public static ReadOnlyDataSource createReadOnly(Path directory, String fileNameOrBaseName) {
        if (fileNameOrBaseName.endsWith(".zip")) {
            return new ZipFileDataSource(directory, getBaseName(fileNameOrBaseName.substring(0, fileNameOrBaseName.length() - 4)));
        } else if (fileNameOrBaseName.endsWith(".gz")) {
            return new GzFileDataSource(directory, getBaseName(fileNameOrBaseName.substring(0, fileNameOrBaseName.length() - 3)));
        } else if (fileNameOrBaseName.endsWith(".bz2")) {
            return new Bzip2FileDataSource(directory, getBaseName(fileNameOrBaseName.substring(0, fileNameOrBaseName.length() - 4)));
        } else if (fileNameOrBaseName.endsWith("_EQ.xml")) {
            //trim the suffix to handle unzipped CIM files
            String basename=getBaseName(fileNameOrBaseName);
            return new FileDataSource(directory, basename.substring(0,basename.length()-3));
        } else {
            return new FileDataSource(directory, getBaseName(fileNameOrBaseName));
        }
    }

    public static ReadOnlyDataSource createReadOnly(Path file) {
        if (!Files.isRegularFile(file)) {
            throw new RuntimeException("File " + file + " does not exist or is not a regular file");
        }
        return createReadOnly(file.getParent(), file.getFileName().toString());
    }

    public static Network loadNetwork(Path file, ComputationManager computationManager, ImportConfig config, Properties parameters) {
        ReadOnlyDataSource dataSource = createReadOnly(file);
        for (Importer importer : Importers.list(computationManager, config)) {
            if (importer.exists(dataSource)) {
                return importer.import_(dataSource, parameters);
            }
        }
        return null;
    }

    public static Network loadNetwork(Path file) {
        return loadNetwork(file, LocalComputationManager.getDefault(), CONFIG.get(), null);
    }

    public static Network loadNetwork(String file) {
        return loadNetwork(Paths.get(file));
    }

    private static void addReadOnlyDataSource(Path dir, Path file, Importer importer, List<ReadOnlyDataSource> dataSources) {
        ReadOnlyDataSource ds = createReadOnly(dir, file.getFileName().toString());
        if (importer.exists(ds)) {
            dataSources.add(ds);
        }
    }

    private static void importAllReadonlyDatasources(Path parent, Importer importer, List<ReadOnlyDataSource> dataSources) throws IOException {
        if (Files.isDirectory(parent)) {
            try (Stream<Path> stream = Files.list(parent)) {
                stream.sorted().forEach(child -> {
                    if (Files.isDirectory(child)) {
                        try {
                            importAllReadonlyDatasources(child, importer, dataSources);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    } else {
                        addReadOnlyDataSource(parent, child, importer, dataSources);
                    }
                });
            }
        } else {
            if (parent.getParent() != null) {
                addReadOnlyDataSource(parent.getParent(), parent, importer, dataSources);
            }
        }
    }

    public static void loadNetworks(Path dir, boolean parallel, ComputationManager computationManager, ImportConfig config, Consumer<Network> consumer, Consumer<ReadOnlyDataSource> listener) throws IOException, InterruptedException, ExecutionException {
        if (!Files.isDirectory(dir)) {
            throw new RuntimeException("Directory " + dir + " does not exist or is not a regular directory");
        }
        for (Importer importer : Importers.list(computationManager, config)) {
            List<ReadOnlyDataSource> dataSources = new ArrayList<>();
            importAllReadonlyDatasources(dir, importer, dataSources);
            Importers.importAll(dir, importer, parallel, consumer, listener,dataSources);
        }
    }

    public static void loadNetworks(Path dir, boolean parallel, ComputationManager computationManager, ImportConfig config, Consumer<Network> consumer) throws IOException, InterruptedException, ExecutionException {
        loadNetworks(dir, parallel, LocalComputationManager.getDefault(), CONFIG.get(), consumer, null);
    }

    public static void loadNetworks(Path dir, boolean parallel, Consumer<Network> consumer) throws IOException, InterruptedException, ExecutionException {
        loadNetworks(dir, parallel, LocalComputationManager.getDefault(), CONFIG.get(), consumer);
    }

    public static void loadNetworks(Path dir, boolean parallel, Consumer<Network> consumer, Consumer<ReadOnlyDataSource> listener) throws IOException, InterruptedException, ExecutionException {
        loadNetworks(dir, parallel, LocalComputationManager.getDefault(), CONFIG.get(), consumer, listener);
    }

    public static void loadNetworks(Path dir, Consumer<Network> consumer) throws IOException, InterruptedException, ExecutionException {
        loadNetworks(dir, false, LocalComputationManager.getDefault(), CONFIG.get(), consumer);
    }

}
