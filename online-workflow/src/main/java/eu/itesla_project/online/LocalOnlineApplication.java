/**
 * Copyright (c) 2016, All partners of the iTesla project (http://www.itesla-project.eu/consortium)
 * Copyright (c) 2016, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package eu.itesla_project.online;

import com.csvreader.CsvWriter;
import com.google.common.collect.Sets;
import eu.itesla_project.cases.CaseRepository;
import com.powsybl.commons.Version;
import com.powsybl.computation.ComputationManager;
import com.powsybl.computation.ComputationResourcesStatus;
import com.powsybl.iidm.import_.Importers;
import com.powsybl.iidm.network.Network;
import com.powsybl.loadflow.LoadFlowFactory;
import eu.itesla_project.mcla.ForecastErrorsDataStorageImpl;
import eu.itesla_project.merge.MergeOptimizerFactory;
import eu.itesla_project.modules.contingencies.ContingenciesAndActionsDatabaseClient;
import eu.itesla_project.modules.histo.HistoDbClient;
import eu.itesla_project.modules.mcla.MontecarloSamplerFactory;
import eu.itesla_project.modules.online.*;
import eu.itesla_project.modules.optimizer.CorrectiveControlOptimizerFactory;
import eu.itesla_project.modules.rules.RulesDbClient;
import eu.itesla_project.modules.wca.UncertaintiesAnalyserFactory;
import eu.itesla_project.modules.wca.WCAFactory;
import eu.itesla_project.offline.forecast_errors.ForecastErrorsAnalysis;
import eu.itesla_project.offline.forecast_errors.ForecastErrorsAnalysisConfig;
import eu.itesla_project.offline.forecast_errors.ForecastErrorsAnalysisParameters;
import com.powsybl.security.LimitViolation;
import com.powsybl.security.Security;
import com.powsybl.simulation.SimulatorFactory;
import gnu.trove.list.array.TIntArrayList;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.ISODateTimeFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import javax.management.*;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.ParseException;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * @author Quinary <itesla@quinary.com>
 */
public class LocalOnlineApplication extends NotificationBroadcasterSupport
        implements OnlineApplication, OnlineApplicationListener, LocalOnlineApplicationMBean {

    private static final Logger LOGGER = LoggerFactory.getLogger(LocalOnlineApplication.class);

    private static final int BUSY_CORES_HISTORY_SIZE = 20;

    private OnlineConfig config;

    private final ComputationManager computationManager;

    private ScheduledExecutorService ses;

    private ExecutorService executorService;

    private final boolean enableJmx;

    private final ScheduledFuture<?> future;

    private final TIntArrayList busyCores;

    private final Lock listenersLock = new ReentrantLock();

    private final List<OnlineApplicationListener> listeners = new CopyOnWriteArrayList<>();

    private final AtomicInteger notificationIndex = new AtomicInteger();

    public LocalOnlineApplication(OnlineConfig config, ComputationManager computationManager,
            ScheduledExecutorService ses, ExecutorService executorService, boolean enableJmx)
                    throws IllegalAccessException, InstantiationException, InstanceAlreadyExistsException,
                    MBeanRegistrationException, MalformedObjectNameException, NotCompliantMBeanException, IOException {
        this.config = config;
        this.computationManager = Objects.requireNonNull(computationManager);
        this.ses = Objects.requireNonNull(ses);
        this.executorService = Objects.requireNonNull(executorService);
        LOGGER.info("Version: {}", Version.VERSION);

        this.enableJmx = enableJmx;

        busyCores = new TIntArrayList();
        ComputationResourcesStatus status = computationManager.getResourcesStatus();
        int v = status.getBusyCores();
        for (int i = 0; i < BUSY_CORES_HISTORY_SIZE; i++) {
            busyCores.add(v);
        }

        if (enableJmx) {
            // create and register online application mbean
            ManagementFactory.getPlatformMBeanServer().registerMBean(this, new ObjectName(BEAN_NAME));

        }

        future = ses.scheduleAtFixedRate(() -> {
            try {
                notifyBusyCoresUpdate(false);
            } catch (Throwable t) {
                LOGGER.error(t.toString(), t);
            }
        }, 0, 20, TimeUnit.SECONDS);
    }

    @Override
    public int[] getBusyCores() {
        return busyCores.toArray();
    }

    @Override
    public void notifyListeners() {
        notifyBusyCoresUpdate(true);
    }

    private void notifyBusyCoresUpdate(boolean force) {
        listenersLock.lock();
        try {
            // busy cores

            if (!force) {
                long tim = System.currentTimeMillis();
                if (tim % 2 == 0) {
                    busyCores.insert(0, computationManager.getResourcesStatus().getBusyCores());
                } else {
                    busyCores.insert(0, 1);
                }

                busyCores.removeAt(busyCores.size() - 1); // remove the older
                                                          // one
            }
            if (enableJmx) {
                sendNotification(new AttributeChangeNotification(this, notificationIndex.getAndIncrement(),
                        System.currentTimeMillis(), "Busy cores has changed", BUSY_CORES_ATTRIBUTE, "int[]", null,
                        busyCores.toArray()));
            }

        } finally {
            listenersLock.unlock();
        }
    }

    @Override
    public int getAvailableCores() {
        return computationManager.getResourcesStatus().getAvailableCores();
    }

    @Override
    public String startProcess(String name, String owner, DateTime date, DateTime creationDate,
            OnlineWorkflowStartParameters start, OnlineWorkflowParameters params, DateTime[] basecases, int numThreads)
                    throws Exception {
        Objects.requireNonNull(date);
        Objects.requireNonNull(creationDate);
        Objects.requireNonNull(params);
        Objects.requireNonNull(basecases);
        config = OnlineConfig.load();
        OnlineDb onlineDb = config.getOnlineDbFactoryClass().newInstance().create();
        String processId = DateTimeFormat.forPattern("yyyyMMddHHmmSSS").print(new DateTime());
        LOGGER.info("Starting process: " + processId);
        OnlineProcess proc = new OnlineProcess(processId, name, owner, params.getCaseType().toString(), date, creationDate);

        ExecutorService taskExecutor = Executors.newFixedThreadPool(numThreads);
        List<Callable<Void>> tasks = new ArrayList<>(numThreads);

        for (DateTime basecase : basecases) {
            tasks.add(new Callable() {
                @Override
                public Object call() throws Exception {
                    OnlineWorkflowParameters pars = new OnlineWorkflowParameters(basecase, params.getStates(), params.getHistoInterval(), params.getOfflineWorkflowId(), params.getTimeHorizon(), params.getFeAnalysisId(), params.getRulesPurityThreshold(), params.storeStates(), params.analyseBasecase(), params.validation(), params.getSecurityIndexes(), params.getCaseType(), params.getCountries(), params.isMergeOptimized(), params.getLimitReduction(), params.isHandleViolationsInN(), params.getConstraintMargin());
                    String workflowId = startWorkflow(start, pars);
                    org.joda.time.format.DateTimeFormatter fmt = ISODateTimeFormat.dateTime();
                    String basecaseString = fmt.print(basecase);
                    proc.addWorkflow(basecaseString, workflowId);
                    return workflowId;
                } });

        }
        taskExecutor.invokeAll(tasks);
        taskExecutor.shutdown();

        onlineDb.storeProcess(proc);
        LOGGER.info("End of process: " + processId);
        return processId;
    }

    @Override
    public String startWorkflow(OnlineWorkflowStartParameters start, OnlineWorkflowParameters params) {
        OnlineWorkflow workflow = null;
        HistoDbClient histoDbClient;
        ContingenciesAndActionsDatabaseClient cadbClient;
        RulesDbClient rulesDb;
        WCAFactory wcaFactory;
        LoadFlowFactory loadFlowFactory;
        OnlineDb onlineDb;
        UncertaintiesAnalyserFactory uncertaintiesAnalyserFactory;
        CorrectiveControlOptimizerFactory correctiveControlOptimizerFactory;
        SimulatorFactory simulatorFactory;
        MontecarloSamplerFactory montecarloSamplerFactory;
        CaseRepository caseRepository;
        MergeOptimizerFactory mergeOptimizerFactory;
        RulesFacadeFactory rulesFacadeFactory;
        ForecastErrorsDataStorageImpl feDataStorage;
        try {
            config = OnlineConfig.load();
            histoDbClient = config.getHistoDbClientFactoryClass().newInstance().create();
            cadbClient = config.getContingencyDbClientFactoryClass().newInstance().create();
            rulesDb = config.getRulesDbClientFactoryClass().newInstance().create("rulesdb");
            wcaFactory = config.getWcaFactoryClass().newInstance();
            loadFlowFactory = config.getLoadFlowFactoryClass().newInstance();
            onlineDb = config.getOnlineDbFactoryClass().newInstance().create();
            uncertaintiesAnalyserFactory = config.getUncertaintiesAnalyserFactoryClass().newInstance();
            correctiveControlOptimizerFactory = config.getCorrectiveControlOptimizerFactoryClass().newInstance();
            simulatorFactory = config.getSimulatorFactoryClass().newInstance();
            montecarloSamplerFactory = config.getMontecarloSamplerFactory().newInstance();
            caseRepository = config.getCaseRepositoryFactoryClass().newInstance().create(computationManager);
            mergeOptimizerFactory = config.getMergeOptimizerFactory().newInstance();
            rulesFacadeFactory = config.getRulesFacadeFactory().newInstance();

            feDataStorage = new ForecastErrorsDataStorageImpl(); // TODO ...

        } catch (ClassNotFoundException | IOException | ParseException | InstantiationException
                | IllegalAccessException e1) {

            e1.printStackTrace();
            throw new RuntimeException(e1.getMessage());
        }

        OnlineWorkflowContext oCtx = new OnlineWorkflowContext();
        OnlineWorkflowStartParameters startParams = start != null ? start : OnlineWorkflowStartParameters.loadDefault();
        OnlineWorkflowParameters onlineParams = params != null ? params : OnlineWorkflowParameters.loadDefault();
        OnlineApplicationListener additionalListener = null;
        LOGGER.info("Starting workflow: " + startParams.toString() + "\n" + onlineParams.toString());

        String wfId = null;
        try {
            workflow = startParams.getOnlineWorkflowFactoryClass().newInstance().create(computationManager, cadbClient,
                    histoDbClient, rulesDb, wcaFactory, loadFlowFactory, feDataStorage, onlineDb,
                    uncertaintiesAnalyserFactory, correctiveControlOptimizerFactory, simulatorFactory, caseRepository,
                    montecarloSamplerFactory, mergeOptimizerFactory, rulesFacadeFactory, onlineParams, startParams);
            workflow.addOnlineApplicationListener(this);

            for (OnlineApplicationListener l : listeners) {
                workflow.addOnlineApplicationListener(l);
            }

            if (startParams.getOnlineApplicationListenerFactoryClass() != null) {
                additionalListener = startParams.getOnlineApplicationListenerFactoryClass().newInstance().create();
                workflow.addOnlineApplicationListener(additionalListener);
            }

            workflow.start(oCtx);
            wfId = workflow.getId();
        } catch (Exception e) {
            LOGGER.error(e.toString(), e);
            if (workflow != null) {
                StatusSynthesis st = new StatusSynthesis(workflow.getId(), WorkflowStatusEnum.ERROR);
                onWorkflowUpdate(st);
            }
            throw new RuntimeException(e);
        } finally {

            if (workflow != null) {
                try {
                    workflow.removeOnlineApplicationListener(this);
                } catch (Exception ignored) {
                }

                for (OnlineApplicationListener l : listeners) {
                    try {
                        workflow.removeOnlineApplicationListener(l);
                    } catch (Exception ignored) {
                    }
                }

                if (additionalListener != null) {
                    try {
                        workflow.removeOnlineApplicationListener(additionalListener);
                    } catch (Exception ignored) {
                    }
                }
            }
            workflow = null;
        }
        return wfId;
    }

    @Override
    public void stopWorkflow() {

    }

    @Override
    public void addListener(OnlineApplicationListener l) {
        Objects.requireNonNull(l);
        listeners.add(l);
    }

    @Override
    public void removeListener(OnlineApplicationListener l) {
        Objects.requireNonNull(l);
        listeners.remove(l);
    }

    @Override
    public void close() throws Exception {
        future.cancel(true);

        if (enableJmx) {
            // unregister application mbean
            ManagementFactory.getPlatformMBeanServer().unregisterMBean(new ObjectName(BEAN_NAME));
        }
        synchronized (this) {
            this.notifyAll();
        }
    }

    @Override
    public void onBusyCoresUpdate(int[] busyCores) {

    }

    @Override
    public void onWorkflowStateUpdate(WorkSynthesis status) {
        if (enableJmx) {
            sendNotification(new AttributeChangeNotification(this, notificationIndex.getAndIncrement(),
                    System.currentTimeMillis(), "WorkflowStateUpdate", WORK_STATES_ATTRIBUTE,
                    status.getClass().getName(), null, status));
        }

    }

    @Override
    public void onWorkflowUpdate(StatusSynthesis status) {
        if (enableJmx) {
            sendNotification(new AttributeChangeNotification(this, notificationIndex.getAndIncrement(),
                    System.currentTimeMillis(), "Running", RUNNING_ATTRIBUTE, status.getClass().getName(), null,
                    status));
        }

    }

    @Override
    public void onWcaUpdate(RunningSynthesis wcaRunning) {
        LOGGER.info("SEND onWcaUpdate" + wcaRunning);
        if (enableJmx) {
            sendNotification(new AttributeChangeNotification(this, notificationIndex.getAndIncrement(),
                    System.currentTimeMillis(), "WcaRunning", WCA_RUNNING_ATTRIBUTE, wcaRunning.getClass().getName(),
                    null, wcaRunning));
        }

    }

    @Override
    public void onStatesWithActionsUpdate(ContingencyStatesActionsSynthesis statesActions) {

        LOGGER.info("onStatesWithActionsUpdate received " + statesActions.getClass().getName());
        if (enableJmx) {
            sendNotification(new AttributeChangeNotification(this, notificationIndex.getAndIncrement(),
                    System.currentTimeMillis(), "StateActions", STATES_ACTIONS_ATTRIBUTE,
                    statesActions.getClass().getName(), null, statesActions));
        }

    }

    @Override
    public void onStatesWithIndexesUpdate(ContingencyStatesIndexesSynthesis indexeActions) {
        LOGGER.info("onStatesWithIndexesUpdate received " + indexeActions.getClass().getName());
        if (enableJmx) {
            sendNotification(new AttributeChangeNotification(this, notificationIndex.getAndIncrement(),
                    System.currentTimeMillis(),
                    // "StateActions",
                    "StatesIndexes", STATES_INDEXES_ATTRIBUTE, indexeActions.getClass().getName(), null,
                    indexeActions));
        }

    }

    @Override
    public void onStatesWithSecurityRulesResultsUpdate(IndexSecurityRulesResultsSynthesis indexesResults) {
        LOGGER.info("onStatesWithSecurityRulesResultsUpdate received " + indexesResults.getClass().getName());
        if (enableJmx) {
            sendNotification(new AttributeChangeNotification(this, notificationIndex.getAndIncrement(),
                    System.currentTimeMillis(), "IndexSecurityRulesResults", INDEXES_SECURITY_RULES_ATTRIBUTE,
                    indexesResults.getClass().getName(), null, indexesResults));
        }

    }

    @Override
    public void onWcaContingencies(WcaContingenciesSynthesis wcaContingencies) {
        sendNotification(new AttributeChangeNotification(this, notificationIndex.getAndIncrement(),
                System.currentTimeMillis(), "WcaContingencies", WCA_CONTINGENCIES_ATTRIBUTE,
                wcaContingencies.getClass().getName(), null, wcaContingencies));

    }

    @Override
    public void ping() {

    }

    @Override
    public void onDisconnection() {

    }

    @Override
    public void onConnection() {

    }

    @Override
    public void shutdown() {
        System.out.println("Shutdown !!!");
        try {
            close();
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    @Override
    public void runFeaAnalysis(OnlineWorkflowStartParameters startconfig, ForecastErrorsAnalysisParameters parameters,
            String timeHorizonS) {
        LOGGER.info("Starting fea analysis: " + parameters.toString() + "\n" + timeHorizonS);

        try {
            ForecastErrorsAnalysis feAnalysis = new ForecastErrorsAnalysis(computationManager,
                    ForecastErrorsAnalysisConfig.load(), parameters);
            if ("".equals(timeHorizonS)) {
                feAnalysis.start();
            } else {
                TimeHorizon timeHorizon = TimeHorizon.fromName(timeHorizonS);
                feAnalysis.start(timeHorizon);
            }
            System.out.println("Forecast Errors Analysis Terminated");
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    @Override
    public void onWorkflowEnd(OnlineWorkflowContext context, OnlineDb onlineDb,
            ContingenciesAndActionsDatabaseClient cadbClient, OnlineWorkflowParameters parameters) {
        System.out.println("LocalOnlineApplicationListener onWorkFlowEnd");
    }

    // start td simulations
    private Path getFile(Path folder, String filename) {
        if (folder != null) {
            return Paths.get(folder.toString(), filename);
        }
        return Paths.get(filename);
    }

    private void writeCsvViolations(String basecase, List<LimitViolation> networkViolations, CsvWriter cvsWriter)
            throws IOException {
        for (LimitViolation violation : networkViolations) {
            String[] values = new String[] {basecase, violation.getSubjectId(), violation.getLimitType().name(),
                    Float.toString(violation.getValue()), Float.toString(violation.getLimit()) };
            cvsWriter.writeRecord(values);
        }
        cvsWriter.flush();
    }

    private void writeCsvTDResults(String basecase, Set<String> securityIndexIds,
            Map<String, Boolean> tdSimulationResults, boolean writeHeaders, CsvWriter cvsWriter) throws IOException {
        String[] indexIds = securityIndexIds.toArray(new String[securityIndexIds.size()]);
        Arrays.sort(indexIds);
        if (writeHeaders) {
            String[] resultsHeaders = new String[indexIds.length + 1];
            resultsHeaders[0] = "Basecase";
            int i = 1;
            for (String securityIndexId : indexIds) {
                resultsHeaders[i++] = securityIndexId;
            }
            cvsWriter.writeRecord(resultsHeaders);
            cvsWriter.flush();
        }
        String[] values = new String[indexIds.length + 1];
        values[0] = basecase;
        int i = 1;
        for (String securityIndexId : indexIds) {
            String result = "NA";
            if (tdSimulationResults.containsKey(securityIndexId)) {
                result = tdSimulationResults.get(securityIndexId) ? "OK" : "KO";
            }
            values[i++] = result;
        }
        cvsWriter.writeRecord(values);
        cvsWriter.flush();
    }

    private void runTDSimulations(Path caseFile, String contingenciesIds, Boolean emptyContingency, Path outputFolder)
            throws Exception {
        ContingenciesAndActionsDatabaseClient cadbClient = config.getContingencyDbClientFactoryClass().newInstance()
                .create();
        SimulatorFactory simulatorFactory = config.getSimulatorFactoryClass().newInstance();

        // TODO check nulls
        // in contingenciesIds, we assume a comma separated list of ids ...
        Set<String> contingencyIds = (contingenciesIds != null) ? Sets.newHashSet(contingenciesIds.split(",")) : null;
        Path metricsFile = getFile(outputFolder, "metrics.log");
        Path violationsFile = getFile(outputFolder, "networks-violations.csv");
        Path resultsFile = getFile(outputFolder, "simulation-results.csv");
        try (FileWriter metricsContent = new FileWriter(metricsFile.toFile());
                FileWriter violationsContent = new FileWriter(violationsFile.toFile());
                FileWriter resultsContent = new FileWriter(resultsFile.toFile())) {
            CsvWriter violationsCvsWriter = new CsvWriter(violationsContent, ',');
            CsvWriter resultsCvsWriter = new CsvWriter(resultsContent, ',');
            Set<String> securityIndexIds = new LinkedHashSet<>();
            try {
                String[] violationsHeaders = new String[] {"Basecase", "Equipment", "Type", "Value", "Limit" };
                violationsCvsWriter.writeRecord(violationsHeaders);
                violationsCvsWriter.flush();

                if (Files.isRegularFile(caseFile)) {

                    // load the network
                    Network network = Importers.loadNetwork(caseFile);
                    if (network == null) {
                        throw new RuntimeException("Case '" + caseFile + "' not found");
                    }
                    network.getStateManager().allowStateMultiThreadAccess(true);

                    List<LimitViolation> networkViolations = Security.checkLimits(network);
                    writeCsvViolations(network.getId(), networkViolations, violationsCvsWriter);
                    Map<String, Boolean> tdSimulationResults = Utils.runTDSimulation(network, contingencyIds,
                            emptyContingency, computationManager, simulatorFactory, cadbClient, metricsContent);
                    securityIndexIds.addAll(tdSimulationResults.keySet());
                    writeCsvTDResults(network.getId(), securityIndexIds, tdSimulationResults, true, resultsCvsWriter);
                } else if (Files.isDirectory(caseFile)) {
                    Importers.loadNetworks(caseFile, false, network -> {
                        try {
                            List<LimitViolation> networkViolations = Security.checkLimits(network);
                            writeCsvViolations(network.getId(), networkViolations, violationsCvsWriter);
                            Map<String, Boolean> tdSimulationResults = Utils.runTDSimulation(network, contingencyIds,
                                    emptyContingency, computationManager, simulatorFactory, cadbClient, metricsContent);
                            boolean writeHeaders = false;
                            if (securityIndexIds.isEmpty()) {
                                securityIndexIds.addAll(tdSimulationResults.keySet());
                                writeHeaders = true;
                            }
                            writeCsvTDResults(network.getId(), securityIndexIds, tdSimulationResults, writeHeaders,
                                    resultsCvsWriter);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }, dataSource -> System.out.println("loading case " + dataSource.getBaseName()));
                }
            } catch (IOException ioxcp) {
                ioxcp.printStackTrace();
            } finally {
                violationsCvsWriter.close();
                resultsCvsWriter.close();
            }
        }
    }

    @Override
    public void runTDSimulations(OnlineWorkflowStartParameters startconfig, String caseFile, String contingenciesIds,
            String emptyContingencyS, String outputFolderS) {
        LOGGER.info("Starting td simulations: " + startconfig.toString() + "\n");

        try {
            boolean emptyContingency = Boolean.parseBoolean(emptyContingencyS);
            Path outputFolder = Paths.get(outputFolderS);
            Path inputCaseFile = Paths.get(caseFile);
            runTDSimulations(inputCaseFile, contingenciesIds, emptyContingency, outputFolder);
            System.out.println("TD simulations terminated");
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        } finally {
        }
    }

    // end td simulations

}
