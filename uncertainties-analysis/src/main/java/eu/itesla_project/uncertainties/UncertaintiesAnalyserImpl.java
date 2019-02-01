/**
 * Copyright (c) 2017, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package eu.itesla_project.uncertainties;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Table;
import com.powsybl.commons.io.MathUtil;
import com.powsybl.computation.*;
import com.powsybl.iidm.network.Network;
import eu.itesla_project.modules.commons.io.CacheManager;
import eu.itesla_project.modules.histo.*;
import eu.itesla_project.modules.wca.StochasticInjection;
import eu.itesla_project.modules.wca.Uncertainties;
import eu.itesla_project.modules.wca.UncertaintiesAnalyser;
import org.joda.time.Interval;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * @author Geoffroy Jamgotchian <geoffroy.jamgotchian@rte-france.com>
 */
public class UncertaintiesAnalyserImpl implements UncertaintiesAnalyser {

    private static final Logger LOGGER = LoggerFactory.getLogger(UncertaintiesAnalyserImpl.class);

    private interface Scripts {

        String getVersion();

        List<String> getScripts();

        String getMainScript();

        String getConfigScript();

        String getMeanRowName();

        String getPcColumnName();

        String getInfColumnName();

        String getSupColumnName();

    }

    private static class EricMedianeBaisseHausseScripts implements Scripts {

        @Override
        public String getVersion() {
            return "eric_mediane_baisse_hausse";
        }

        @Override
        public List<String> getScripts() {
            return ImmutableList.of("essais_PCA_WP5_5.0.R");
        }

        @Override
        public String getMainScript() {
            return "essais_PCA_WP5_5.0.R";
        }

        @Override
        public String getConfigScript() {
            return null;
        }

        @Override
        public String getMeanRowName() {
            return "1";
        }

        @Override
        public String getPcColumnName() {
            return "";
        }

        @Override
        public String getInfColumnName() {
            return "V1";
        }

        @Override
        public String getSupColumnName() {
            return "V1";
        }

    }

    private static class BluestoneScripts implements Scripts {

        @Override
        public String getVersion() {
            return "bluestone";
        }

        @Override
        public List<String> getScripts() {
            return ImmutableList.of("error_box_v1.1.R",
                    "export_error_box_v1.0.R",
                    "preprocess_data_error_box_v1.0.R");
        }

        @Override
        public String getMainScript() {
            return "export_error_box_v1.0.R";
        }

        @Override
        public String getConfigScript() {
            return null;
        }

        @Override
        public String getMeanRowName() {
            return "1";
        }

        @Override
        public String getPcColumnName() {
            return "PC";
        }

        @Override
        public String getInfColumnName() {
            return "inf";
        }

        @Override
        public String getSupColumnName() {
            return "sup";
        }

    }

    private static class BluestoneV30Scripts implements Scripts {

        @Override
        public String getVersion() {
            return "bluestone_v3.0";
        }

        @Override
        public List<String> getScripts() {
            return ImmutableList.of("export_error_box_clustering.R",
                    "clustering_correlation.R",
                    "disaggregation.R",
                    "error_box.R",
                    "preprocess_data_error_box.R",
                    "type_equipment.R"
            );
        }

        @Override
        public String getMainScript() {
            return "export_error_box_clustering.R";
        }

        @Override
        public String getConfigScript() {
            return null;
        }

        @Override
        public String getMeanRowName() {
            return "1";
        }

        @Override
        public String getPcColumnName() {
            return "PC";
        }

        @Override
        public String getInfColumnName() {
            return "inf";
        }

        @Override
        public String getSupColumnName() {
            return "sup";
        }

    }

    private static class BluestoneV34Scripts implements Scripts {

        @Override
        public String getVersion() {
            return "bluestone_v3.4";
        }

        @Override
        public List<String> getScripts() {
            return ImmutableList.of("clustering_correlation.R",
                    "configuration_error_box.R",
                    "disaggregation.R",
                    "error_box.R",
                    "export_error_box_clustering.R",
                    "preprocess_data_error_box.R",
                    "type_equipment.R"
            );
        }

        @Override
        public String getMainScript() {
            return "export_error_box_clustering.R";
        }

        @Override
        public String getConfigScript() {
            return null;
        }

        @Override
        public String getMeanRowName() {
            return "1";
        }

        @Override
        public String getPcColumnName() {
            return "PC";
        }

        @Override
        public String getInfColumnName() {
            return "inf";
        }

        @Override
        public String getSupColumnName() {
            return "sup";
        }

    }

    private static class BluestoneV35Scripts implements Scripts {

        @Override
        public String getVersion() {
            return "bluestone_v3.5";
        }

        @Override
        public List<String> getScripts() {
            return ImmutableList.of("clustering_correlation.R",
                    "configuration_error_box.R",
                    "disaggregation.R",
                    "error_box.R",
                    "export_error_box_clustering.R",
                    "preprocess_data_error_box.R",
                    "type_equipment.R"
            );
        }

        @Override
        public String getMainScript() {
            return "export_error_box_clustering.R";
        }

        @Override
        public String getConfigScript() {
            return "configuration_error_box.R";
        }

        @Override
        public String getMeanRowName() {
            return "1";
        }

        @Override
        public String getPcColumnName() {
            return "PC";
        }

        @Override
        public String getInfColumnName() {
            return "inf";
        }

        @Override
        public String getSupColumnName() {
            return "sup";
        }

    }

    private static final Scripts SCRIPTS = new BluestoneV35Scripts();

    private static final String FORECAST_DIFF_LOAD_CSV = "forecastsDiff_load.csv";
    private static final String FORECAST_DIFF_GEN_CSV = "forecastsDiff_gen.csv";
    private static final String SN_LOAD_CSV = "snapshots_load.csv";
    private static final String SN_GEN_CSV = "snapshots_gen.csv";
    private static final String MATRICE_CSV = "matrice.csv";
    private static final String VECTEUR_CSV = "vecteur.csv";
    private static final String BORNES_INF_CSV = "bornes_inf.csv";
    private static final String BORNES_SUP_CSV = "bornes_sup.csv";

    private final Network network;

    private final HistoDbClient histoDbClient;

    private final ComputationManager computationManager;

    private UncertaintiesAnalysisConfig config = UncertaintiesAnalysisConfig.load();

    public UncertaintiesAnalyserImpl(Network network,
                                     HistoDbClient histoDbClient,
                                     ComputationManager computationManager) {
        Objects.requireNonNull(network);
        Objects.requireNonNull(histoDbClient);
        Objects.requireNonNull(computationManager);
        this.network = network;
        this.histoDbClient = histoDbClient;
        this.computationManager = computationManager;

        LOGGER.info(config.toString());
    }

    private CacheManager.CacheEntry getInjectionsCacheDir(Interval interval, List<StochasticInjection> networkInjections) {
        return CacheManager.defaultCacheManager().newCacheEntry("uncertainties")
                .withKey(interval.toString())
                .withKeys(networkInjections.stream().map(StochasticInjection::getId).collect(Collectors.toList()))
                .withKey(SCRIPTS.getVersion())
                .withKey(Float.toString(config.getPrctRisk()))
                .build();
    }

    private CacheManager.CacheEntry getMonthlyCacheDir(Interval interval) {
        return CacheManager.defaultCacheManager().newCacheEntry("uncertainties")
                .withKey(interval.toString())
                .withKey("" + network.getCaseDate().year().get() + network.getCaseDate().monthOfYear().get())
                .withKey(SCRIPTS.getVersion())
                .withKey(Float.toString(config.getPrctRisk()))
                .build();
    }

    private static boolean isCacheValid(CacheManager.CacheEntry cacheEntry) {
        return cacheEntry.exists()
                && Files.exists(cacheEntry.toPath().resolve(MATRICE_CSV))
                && Files.exists(cacheEntry.toPath().resolve(VECTEUR_CSV))
                && Files.exists(cacheEntry.toPath().resolve(BORNES_INF_CSV))
                && Files.exists(cacheEntry.toPath().resolve(BORNES_SUP_CSV));
    }

    @Override
    public CompletableFuture<Uncertainties> analyse(Interval interval) throws Exception {

        return computationManager.execute(new ExecutionEnvironment(ImmutableMap.of(), "itesla-r-", config.isDebug()), new AbstractExecutionHandler<Uncertainties>() {

            private List<StochasticInjection> networkInjections;

            private CacheManager.CacheEntry cacheEntry;

            @Override
            public List<CommandExecution> before(Path workingDir) throws IOException {

                List<CommandExecution> commandExecutions = new ArrayList<>();

                LOGGER.info("Uncertainties analysis version {}", SCRIPTS.getVersion());

                networkInjections = StochasticInjection.create(network, false, config.isOnlyIntermittentGeneration(), config.isWithBoundaries(), config.getBoundariesFilter()); // only main cc

                LOGGER.info("{} injections in the network", networkInjections.size());

                if (config.useMonthlyCache()) {
                    cacheEntry = getMonthlyCacheDir(interval);
                } else {
                    cacheEntry = getInjectionsCacheDir(interval, networkInjections);
                }
                cacheEntry.lock();
                try {
                    if (isCacheValid(cacheEntry)) {
                        LOGGER.info("Using uncertainties cached in {}", cacheEntry);

                        // load R output csv files from cache
                        Files.copy(cacheEntry.toPath().resolve(MATRICE_CSV), workingDir.resolve(MATRICE_CSV));
                        Files.copy(cacheEntry.toPath().resolve(VECTEUR_CSV), workingDir.resolve(VECTEUR_CSV));
                        Files.copy(cacheEntry.toPath().resolve(BORNES_INF_CSV), workingDir.resolve(BORNES_INF_CSV));
                        Files.copy(cacheEntry.toPath().resolve(BORNES_SUP_CSV), workingDir.resolve(BORNES_SUP_CSV));
                    } else {
                        // copy R script
                        for (String scriptName : SCRIPTS.getScripts()) {
                            String scriptJarPath = "/R/" + SCRIPTS.getVersion() + "/" + scriptName;
                            Path scriptFile = workingDir.resolve(scriptName);
                            if (scriptName.equals(SCRIPTS.getConfigScript())) {
                                // FIXME vraiment degueulasse comme facon de faire mais pas le temps de faire mieux...
                                boolean found = false;
                                try (BufferedReader reader = new BufferedReader(new InputStreamReader(getClass().getResourceAsStream(scriptJarPath), StandardCharsets.UTF_8));
                                     BufferedWriter writer = Files.newBufferedWriter(scriptFile, StandardCharsets.UTF_8)) {
                                    String line;
                                    while ((line = reader.readLine()) != null) {
                                        if (line.startsWith("prct_risk=")) {
                                            writer.write("prct_risk=" + config.getPrctRisk());
                                            found = true;
                                        } else {
                                            writer.write(line);
                                        }
                                        writer.newLine();
                                    }
                                }
                                if (!found) {
                                    throw new RuntimeException("Assignment of variable prct_risk not found in script " + scriptName);
                                }
                            } else {
                                try (InputStream is = getClass().getResourceAsStream(scriptJarPath)) {
                                    Files.copy(is, scriptFile);
                                }
                            }
                        }

                        Set<HistoDbAttributeId> metaAttributeIds = new LinkedHashSet<>();
                        metaAttributeIds.add(HistoDbMetaAttributeId.datetime);
                        metaAttributeIds.add(HistoDbMetaAttributeId.forecastTime);
                        metaAttributeIds.add(HistoDbMetaAttributeId.horizon);

                        Set<HistoDbAttributeId> loadAttributeIds = new LinkedHashSet<>();
                        loadAttributeIds.addAll(metaAttributeIds);
                        loadAttributeIds.addAll(networkInjections.stream()
                                .filter(inj -> inj.getType() == StochasticInjection.Type.LOAD)
                                .map(inj -> new HistoDbNetworkAttributeId(inj.getId(), HistoDbAttr.P))
                                .collect(Collectors.toList()));
                        List<HistoDbAttributeId> danglingLineAttributeIds = networkInjections.stream()
                                .filter(inj -> inj.getType() == StochasticInjection.Type.DANGLING_LINE)
                                .map(inj -> new HistoDbNetworkAttributeId(inj.getId(), HistoDbAttr.P0))
                                .collect(Collectors.toList());
                        if (danglingLineAttributeIds.size() > 0) {
                            LOGGER.info("Computing uncertainties for following dangling line active power: {}", danglingLineAttributeIds);
                        }
                        loadAttributeIds.addAll(danglingLineAttributeIds);

                        Set<HistoDbAttributeId> genAttributeIds = new LinkedHashSet<>();
                        genAttributeIds.addAll(metaAttributeIds);
                        genAttributeIds.addAll(networkInjections.stream()
                                .filter(inj -> inj.getType() == StochasticInjection.Type.GENERATOR)
                                .map(inj -> new HistoDbNetworkAttributeId(inj.getId(), HistoDbAttr.P))
                                .collect(Collectors.toList()));

                        // copy input CSV files
                        try {
                            try (InputStream is = histoDbClient.queryCsv(HistoQueryType.forecastDiff,
                                    loadAttributeIds,
                                    interval,
                                    HistoDbHorizon.DACF,
                                    true, true)) {
                                Files.copy(is, workingDir.resolve(FORECAST_DIFF_LOAD_CSV));
                            }
                            try (InputStream is = histoDbClient.queryCsv(HistoQueryType.forecastDiff,
                                    genAttributeIds,
                                    interval,
                                    HistoDbHorizon.DACF,
                                    true, true)) {
                                Files.copy(is, workingDir.resolve(FORECAST_DIFF_GEN_CSV));
                            }
                            try (InputStream is = histoDbClient.queryCsv(HistoQueryType.data,
                                    loadAttributeIds,
                                    interval,
                                    HistoDbHorizon.SN,
                                    true, true)) {
                                Files.copy(is, workingDir.resolve(SN_LOAD_CSV));
                            }
                            try (InputStream is = histoDbClient.queryCsv(HistoQueryType.data,
                                    genAttributeIds,
                                    interval,
                                    HistoDbHorizon.SN,
                                    true, true)) {
                                Files.copy(is, workingDir.resolve(SN_GEN_CSV));
                            }
                        } catch (InterruptedException e) {
                            throw new RuntimeException(e);
                        }

                        List<InputFile> inputFiles = new ArrayList<>(4 + SCRIPTS.getScripts().size());
                        for (String scriptName : SCRIPTS.getScripts()) {
                            inputFiles.add(new InputFile(scriptName));
                        }
                        inputFiles.add(new InputFile(FORECAST_DIFF_LOAD_CSV));
                        inputFiles.add(new InputFile(FORECAST_DIFF_GEN_CSV));
                        inputFiles.add(new InputFile(SN_LOAD_CSV));
                        inputFiles.add(new InputFile(SN_GEN_CSV));

                        Command cmd = new SimpleCommandBuilder()
                                .id("R")
                                .program("R")
                                .inputFiles(inputFiles)
                                .outputFiles(new OutputFile(MATRICE_CSV),
                                        new OutputFile(VECTEUR_CSV),
                                        new OutputFile(BORNES_INF_CSV),
                                        new OutputFile(BORNES_SUP_CSV))
                                .args("--no-save", "-f", SCRIPTS.getMainScript())
                                .build();

                        commandExecutions.add(new CommandExecution(cmd, 1, Integer.MAX_VALUE));
                    }

                } catch (Throwable t) {
                    cacheEntry.unlock();
                    throw t;
                }
                return commandExecutions;
            }

            @Override
            public Uncertainties after(Path workingDir, ExecutionReport report) throws IOException {
                report.log();

                try {
                    if (report.getErrors().size() > 0) {
                        throw new RuntimeException("Fail to compute uncertainties with R");
                    }

                    if (!isCacheValid(cacheEntry)) {
                        // save R output csv into the cache
                        cacheEntry.create();
                        Files.copy(workingDir.resolve(MATRICE_CSV), cacheEntry.toPath().resolve(MATRICE_CSV));
                        Files.copy(workingDir.resolve(VECTEUR_CSV), cacheEntry.toPath().resolve(VECTEUR_CSV));
                        Files.copy(workingDir.resolve(BORNES_INF_CSV), cacheEntry.toPath().resolve(BORNES_INF_CSV));
                        Files.copy(workingDir.resolve(BORNES_SUP_CSV), cacheEntry.toPath().resolve(BORNES_SUP_CSV));
                    }
                } finally {
                    cacheEntry.unlock();
                }

                // correlation matrix file format
                // ""     , "AMARG_TG1_WGU_SM_P" , "AMARG_TG3_WGU_SM_P", ..., "ABIDOL31MARSI_EC_P", ...
                // "Dim1" , "-0.0013791198031061",
                // "Dim2" , ...
                // ...

                // mean error vector file format
                // "" , "AMARG_TG1_WGU_SM_P", "AMARG_TG3_WGU_SM_P", ..., "ABIDOL31MARSI_EC_P", ...
                // "1", 2.38568223494511, ...

                // error inf limit file format
                // PC,  inf
                // Dim1, -1735.16696155651
                // ...

                // error sup limit file format
                // PC, sup
                // Dim1, 1735.16696155651
                // ...

                try (Reader transferMatrixReader = Files.newBufferedReader(workingDir.resolve(MATRICE_CSV), StandardCharsets.UTF_8);
                     Reader meanVectorReader = Files.newBufferedReader(workingDir.resolve(VECTEUR_CSV), StandardCharsets.UTF_8);
                     Reader infVectorReader = Files.newBufferedReader(workingDir.resolve(BORNES_INF_CSV), StandardCharsets.UTF_8);
                     Reader supVectorReader = Files.newBufferedReader(workingDir.resolve(BORNES_SUP_CSV), StandardCharsets.UTF_8)) {
                    final Table<String, String, Float> transferMatrix = MathUtil.parseMatrix(transferMatrixReader);
                    Table<String, String, Float> meanVector = MathUtil.parseMatrix(meanVectorReader);
                    Table<String, String, Float> infVector = MathUtil.parseMatrix(infVectorReader);
                    Table<String, String, Float> supVector = MathUtil.parseMatrix(supVectorReader);

                    if (!transferMatrix.columnKeySet().equals(meanVector.columnKeySet())
                            || !transferMatrix.rowKeySet().equals(infVector.rowKeySet())
                            || !transferMatrix.rowKeySet().equals(supVector.rowKeySet())) {
                        throw new RuntimeException("Something goes wrong with output data...");
                    }

                    List<StochasticInjection> analysedInjections = new ArrayList<>();
                    Set<StochasticInjection> unanalysedInjections = new HashSet<>();
                    for (StochasticInjection inj : networkInjections) {
                        if (!transferMatrix.containsColumn(inj.getId() + "_P")) {
                            unanalysedInjections.add(inj);
                        } else {
                            analysedInjections.add(inj);
                        }
                    }

                    LOGGER.debug("{} on {} injections analysed by R script", analysedInjections.size(), networkInjections.size());

                    if (unanalysedInjections.size() > 0) {
                        LOGGER.warn("Detail list of the {} unanalysed injections:", unanalysedInjections.size());
                        for (StochasticInjection inj : unanalysedInjections) {
                            LOGGER.warn("    {}", inj.getId());
                        }
                    }

                    // get the list of reduced variables
                    List<String> reducedVariables = new ArrayList<>(transferMatrix.rowKeySet());

                    LOGGER.info("{} injections reduced to {}", transferMatrix.columnKeySet().size(), reducedVariables.size());

                    Uncertainties uncertainties = new Uncertainties(analysedInjections, reducedVariables.size());

                    for (int i = 0; i < analysedInjections.size(); i++) {
                        StochasticInjection inj = analysedInjections.get(i);
                        for (int j = 0; j < reducedVariables.size(); j++) {
                            String reducedVar = reducedVariables.get(j);
                            Float v = transferMatrix.get(reducedVar, inj.getId() + "_P");
                            if (v == null) {
                                throw new RuntimeException("Missing value in the correlation matrix");
                            }
                            uncertainties.reductionMatrix[i][j] = v;
                        }
                    }

                    for (int i = 0; i < analysedInjections.size(); i++) {
                        StochasticInjection inj = analysedInjections.get(i);
                        Float v = meanVector.get(SCRIPTS.getMeanRowName(), inj.getId() + "_P");
                        if (v == null) {
                            throw new RuntimeException("Missing value in the mean vector");
                        }
                        uncertainties.means[i] = v;
                    }

                    for (int j = 0; j < reducedVariables.size(); j++) {
                        String var = reducedVariables.get(j);
                        Float min = infVector.get(var, SCRIPTS.getInfColumnName());
                        Float max = supVector.get(var, SCRIPTS.getSupColumnName());
                        if (min == null) {
                            throw new RuntimeException("Missing value in the min vector");
                        }
                        if (max == null) {
                            throw new RuntimeException("Missing value in the max vector");
                        }
                        uncertainties.min[j] = min;
                        uncertainties.max[j] = max;
                    }

                    return uncertainties;
                }
            }
        });
    }
}
