/**
 * Copyright (c) 2017, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package eu.itesla_project.wca;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.itesla_project.commons.io.table.Column;
import eu.itesla_project.commons.io.table.TableFormatter;
import eu.itesla_project.commons.util.StringToIntMapper;
import eu.itesla_project.contingency.Contingency;
import eu.itesla_project.commons.datasource.DataSource;
import eu.itesla_project.commons.datasource.GzFileDataSource;
import eu.itesla_project.iidm.export.Exporters;
import eu.itesla_project.iidm.export.ampl.AmplConstants;
import eu.itesla_project.iidm.export.ampl.AmplSubset;
import eu.itesla_project.iidm.export.ampl.util.AmplDatTableFormatter;
import eu.itesla_project.iidm.network.Generator;
import eu.itesla_project.iidm.network.Load;
import eu.itesla_project.iidm.network.Network;
import eu.itesla_project.modules.wca.WCAClusterNum;
import eu.itesla_project.security.LimitViolation;

/**
 *
 * @author Massimo Ferraro <massimo.ferraro@techrain.it>
 */
public final class WCAUtils {

    private static final Logger LOGGER = LoggerFactory.getLogger(WCAUtils.class);

    private WCAUtils() {
    }

    public static void exportState(Network network, Path folder, int faultNum, int actionNum) {
        Objects.requireNonNull(network);
        Objects.requireNonNull(folder);
        Properties parameters = new Properties();
        parameters.setProperty("iidm.export.xml.indent", "true");
        parameters.setProperty("iidm.export.xml.with-branch-state-variables", "true");
        DataSource dataSource = new GzFileDataSource(folder, network.getId() + "_" + faultNum + "_" + actionNum);
        Exporters.export("XIIDM", network, parameters, dataSource);
    }

    private static Matcher parseOutFile(String prefix, Pattern pattern, Path workingDir) throws IOException {
        Path out = workingDir.resolve(prefix + "_0.out");
        Path outGz = workingDir.resolve(prefix + "_0.out.gz");
        if (Files.exists(out) || Files.exists(outGz)) {
            try (BufferedReader reader = (Files.exists(out)) ? Files.newBufferedReader(out, StandardCharsets.UTF_8)
                                                             : new BufferedReader(new InputStreamReader(new GZIPInputStream(new FileInputStream(outGz.toFile()))))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    Matcher matcher = pattern.matcher(line);
                    if (matcher.matches()) {
                        return matcher;
                    }
                }
            }
        } else {
            LOGGER.error("WCA output file {} or {} not found !!", out.toFile().getAbsolutePath(), outGz.toFile().getAbsolutePath());
        }
        return null;
    }

    private static Map<String, Float> parseUncertaintiesFile(String uncertaintiesFile, Path workingDir) throws IOException {
        Map<String, Float> injections = new HashMap<String, Float>();
        Path out = workingDir.resolve(uncertaintiesFile);
        Path outGz = workingDir.resolve(uncertaintiesFile + ".gz");
        if (Files.exists(out) || Files.exists(outGz)) {
            try (BufferedReader reader = (Files.exists(out)) ? Files.newBufferedReader(out, StandardCharsets.UTF_8)
                                                             : new BufferedReader(new InputStreamReader(new GZIPInputStream(new FileInputStream(outGz.toFile()))))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.trim().isEmpty()) {
                        continue;
                    }
                    String[] tokens = line.split(";");
                    injections.put(tokens[0].replaceAll("^\"|\"$", ""), Float.parseFloat(tokens[1]));
                }
            }
        } else {
            LOGGER.error("WCA injections file {} or {} not found !!", out.toFile().getAbsolutePath(), outGz.toFile().getAbsolutePath());
        }
        return injections;
    }

    private static boolean flowsWithViolations(String flowsFile, Path workingDir) throws IOException {
        Path out = workingDir.resolve(flowsFile);
        Path outGz = workingDir.resolve(flowsFile + ".gz");
        if (Files.exists(out) || Files.exists(outGz)) {
            try (BufferedReader reader = (Files.exists(out)) ? Files.newBufferedReader(out, StandardCharsets.UTF_8)
                                                             : new BufferedReader(new InputStreamReader(new GZIPInputStream(new FileInputStream(outGz.toFile()))))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.trim().isEmpty()) {
                        continue;
                    }
                    String[] tokens = line.split(";");
                    if (Math.abs(Float.parseFloat(tokens[1])) > Math.abs(Float.parseFloat(tokens[2]))) {
                        LOGGER.info("Folder {}: found flow violation on branch {}", workingDir, tokens[0]);
                        return true;
                    }
                }
            }
        } else {
            LOGGER.error("WCA flows file {} or {} not found !!", out.toFile().getAbsolutePath(), outGz.toFile().getAbsolutePath());
        }
        return false;
    }

    private static final Pattern DOMAINS_RESULTS_PATTERN = Pattern.compile(" WCA Result : basic_violation (\\d*) rule_violation (\\d*) preventive_action_index (\\d*)");

    public static WCADomainsResult readDomainsResult(String domainsPrefix, Path workingDir, String uncertaintiesFile) throws IOException {
        Objects.requireNonNull(domainsPrefix);
        Objects.requireNonNull(workingDir);
        Objects.requireNonNull(uncertaintiesFile);
        boolean foundBasicViolations = false;
        boolean rulesViolated = false;
        int preventiveActionIndex = 0;
        Matcher matcher = parseOutFile(domainsPrefix, DOMAINS_RESULTS_PATTERN, workingDir);
        if (matcher != null) {
            int basicViolation = Integer.parseInt(matcher.group(1));
            int ruleViolation = Integer.parseInt(matcher.group(2));
            preventiveActionIndex = Integer.parseInt(matcher.group(3));
            if (basicViolation == 1) {
                foundBasicViolations = true;
            }
            if (ruleViolation == 1) {
                rulesViolated = true;
            }
        }
        return new WCADomainsResult(foundBasicViolations, rulesViolated, preventiveActionIndex, parseUncertaintiesFile(uncertaintiesFile, workingDir));
    }

    private static final Pattern CLUSTER_INDEX_PATTERN = Pattern.compile(" WCA Result : contingency_index (\\d*) contingency_cluster_index (\\d*) curative_action_index (\\d*)");

    public static WCAClustersResult readClustersResult(String clustersPrefix, Path workingDir, String flowsFile, String uncertaintiesFile) throws IOException {
        Objects.requireNonNull(clustersPrefix);
        Objects.requireNonNull(workingDir);
        Objects.requireNonNull(uncertaintiesFile);
        WCAClusterNum clusterNum = WCAClusterNum.UNDEFINED;
        int curativeActionIndex = 0;
        Matcher matcher = parseOutFile(clustersPrefix, CLUSTER_INDEX_PATTERN, workingDir);
        if (matcher != null) {
            clusterNum = WCAClusterNum.fromInt(Integer.parseInt(matcher.group(2)));
            curativeActionIndex = Integer.parseInt(matcher.group(3));
        }
        return new WCAClustersResult(clusterNum, flowsWithViolations(flowsFile, workingDir), curativeActionIndex,
                                     parseUncertaintiesFile(uncertaintiesFile, workingDir));
    }

    public static void writeContingencies(Collection<Contingency> contingencies, DataSource dataSource, StringToIntMapper<AmplSubset> mapper) {
        Objects.requireNonNull(contingencies);
        Objects.requireNonNull(dataSource);
        Objects.requireNonNull(mapper);
        try (TableFormatter formatter = new AmplDatTableFormatter(
                    new OutputStreamWriter(dataSource.newOutputStream(WCAConstants.FAULTS_FILE_SUFFIX, WCAConstants.TXT_EXT, false), StandardCharsets.UTF_8),
                    "Contingencies",
                    AmplConstants.INVALID_FLOAT_VALUE,
                    true,
                    AmplConstants.LOCALE,
                    new Column("num"),
                    new Column("id"))) {
            for (Contingency contingency : contingencies) {
                int contingencyNum = mapper.getInt(AmplSubset.FAULT, contingency.getId());
                formatter.writeCell(contingencyNum)
                         .writeCell(contingency.getId());
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void writeActions(Collection<String> actionIds, DataSource dataSource, StringToIntMapper<AmplSubset> mapper,
                                     String title, AmplSubset amplSubset) {
        Objects.requireNonNull(actionIds);
        Objects.requireNonNull(dataSource);
        Objects.requireNonNull(mapper);
        Objects.requireNonNull(title);
        Objects.requireNonNull(amplSubset);
        try (TableFormatter formatter = new AmplDatTableFormatter(
                    new OutputStreamWriter(dataSource.newOutputStream(WCAConstants.ACTIONS_FILE_SUFFIX, WCAConstants.TXT_EXT, false), StandardCharsets.UTF_8),
                    title,
                    AmplConstants.INVALID_FLOAT_VALUE,
                    true,
                    AmplConstants.LOCALE,
                    new Column("num"),
                    new Column("id"))) {
            for (String actionId : actionIds) {
                int actionNum = mapper.getInt(amplSubset, actionId);
                formatter.writeCell(actionNum)
                         .writeCell(actionId);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void applyInjections(Network network, String stateId, Map<String, Float> injections) {
        Objects.requireNonNull(network);
        Objects.requireNonNull(stateId);
        Objects.requireNonNull(injections);
        String originalStateId = network.getStateManager().getWorkingStateId();
        network.getStateManager().setWorkingState(stateId);
        injections.keySet().forEach(injection -> {
            Load load = network.getLoad(injection);
            if (load != null) {
                float oldP = load.getTerminal().getP();
                LOGGER.debug("Network {}, state {}: incrementing P of load {} from {} to {}",
                             network.getId(), network.getStateManager().getWorkingStateId(), injection, oldP, oldP + injections.get(injection));
                load.getTerminal().setP(oldP + injections.get(injection));
                load.setP0(oldP + injections.get(injection));
            } else {
                Generator generator = network.getGenerator(injection);
                if (generator != null) {
                    float oldP = generator.getTerminal().getP();
                    LOGGER.debug("Network {}, state {}: incrementing P of generator {} from {} to {}",
                                 network.getId(), network.getStateManager().getWorkingStateId(), injection, oldP, oldP + injections.get(injection));
                    generator.getTerminal().setP(oldP + injections.get(injection));
                    generator.setTargetP(-oldP - injections.get(injection));
                } else {
                    LOGGER.error("No load or generator with id {} in network {}: cannot apply the injection", injection, network.getId());
                }

            }
        });
        network.getStateManager().setWorkingState(originalStateId);
    }

    public static boolean containsViolation(List<LimitViolation> violations, LimitViolation violation) {
        Objects.requireNonNull(violations);
        Objects.requireNonNull(violation);
        Optional<LimitViolation> foundLimitViolation = violations
                .stream()
                .filter(limitViolation -> limitViolation.getSubjectId().equals(violation.getSubjectId())
                        && limitViolation.getLimitType().equals(violation.getLimitType()))
                .findAny();
        return foundLimitViolation.isPresent();
    }

}
