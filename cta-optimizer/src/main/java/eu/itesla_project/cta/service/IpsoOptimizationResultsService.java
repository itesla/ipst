/** Copyright (c) 2016, Tractebel (http://www.tractebel-engie.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this file,
 * You can obtain one at http://mozilla.org/MPL/2.0/.
 **/
package eu.itesla_project.cta.service;

import com.google.common.base.Splitter;
import com.google.common.collect.Lists;
import eu.itesla_project.computation.ExecutionReport;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.slf4j.Logger;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

import static com.google.common.base.Preconditions.checkArgument;
import static eu.itesla_project.cta.service.IpsoEquipmentType.GENERATOR;
import static eu.itesla_project.cta.service.IpsoEquipmentType.TFO2W;
import static eu.itesla_project.cta.service.IpsoOptimizationStatus.*;
import static java.util.stream.Collectors.toList;
import static org.slf4j.LoggerFactory.getLogger;
/**
 * @author Yannick Pihan <yannick.pihan at tractebel.engie.com>
 */
final class IpsoOptimizationResultsService {

    private static final Logger LOG = getLogger(IpsoOptimizationResultsService.class);

    protected static final char SEPARATOR = ';';
    protected static final int INDEX_OF_TOLERANCE = 3;
    protected static final double TOLERANCE_ERROR = 0.0001;
    protected static final String CONTROL = "CONTROL";
    protected static final String TAP = "TAP";
    protected static final String GENE = "GENE";
    protected static final String TFO_2 = "TFO2";

    public IpsoOptimizationResults createIpsoOptimizationResults(ExecutionReport executionReport, Path csvPath) {
        checkArgument(csvPath != null, "csvPath must not be null");
        checkArgument(executionReport != null, "executionReport must not be null");

        Path outPath = resolveOutPathFrom(csvPath);

        IpsoOptimizationStatus status;
        IpsoSolution solution = null;
        if (isExecutionFailed(executionReport)) {
            status = EXECUTION_FAILED;
        }
        else if ( isNullOrNotExists(outPath) ) {
            status = IPSO_OUT_FILE_MISSING;
        }
        else if ( isNullOrNotExists(csvPath)) {
            status = IPSO_CSV_FILE_MISSING;
        }
        else {
            solution = createSolutionFromFile(csvPath);
            status = findStatusOf(solution);
        }

        return new IpsoOptimizationResults(status, outPath, csvPath, solution);
    }

    private IpsoOptimizationStatus findStatusOf(IpsoSolution solution) {
        return solution.getObjectiveFunctionValue() < TOLERANCE_ERROR ? SUCCEDED : ERROR;
    }

    private IpsoSolution createSolutionFromFile(Path path) {
        List<IpsoSolutionElement> solutions = Lists.newArrayList();
        List<String> lines = readLinesOf(path);
        if ( lines.isEmpty()) {
            return new IpsoSolution(solutions, 9999.9f);
        }
        else {
            float tolerance = findTolerance(lines);
            solutions.addAll(findGeneratorSolutions(lines));
            solutions.addAll(findTransformerSolutions(lines));
            return new IpsoSolution(solutions, tolerance);
        }
    }

    private boolean isExecutionFailed(ExecutionReport executionReport) {
        return !executionReport.getErrors().isEmpty();
    }

    private List<IpsoSolutionElement> findTransformerSolutions(List<String> lines) {
        return lines.stream()
                .filter(this::lineThatConstainsControl)
                .filter(lineThatContainsTransformerDescription())
                .map(this::createSolutionForTransformer)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(toList());
    }

    private List<IpsoSolutionElement> findGeneratorSolutions(List<String> lines) {
        return lines.stream()
                .filter(this::lineThatConstainsControl)
                .filter(lineThatContainsGeneratorDescription())
                .map(this::createSolutionForGenerator)
                .collect(toList());
    }

    private IpsoSolutionElement createSolutionForGenerator(String line) {
        List<String> lines = splitToList(line);
        String name = lines.get(3);
        String attribute = lines.get(4);
        float setpoint = Float.parseFloat(lines.get(8));
        return new IpsoSolutionElement(GENERATOR, name, attribute, setpoint);
    }

    private Optional<IpsoSolutionElement> createSolutionForTransformer(String line) {
        List<String> lines = splitToList(line);
        String name = lines.get(3);
        String attribute = lines.get(4);
        if (attribute.contains(TAP)) {
            float setpoint = Float.parseFloat(lines.get(8));
            return Optional.of(new IpsoSolutionElement(TFO2W, name, attribute, setpoint));
        }
        else {
            return Optional.empty();
        }
    }

    private Predicate<String> lineThatContainsGeneratorDescription() {
        return line -> splitToList(line).contains(GENE);
    }

    private Predicate<? super String> lineThatContainsTransformerDescription() {
        return line -> splitToList(line).contains(TFO_2);
    }

    private boolean lineThatConstainsControl(String line) {
        return splitToList(line).contains(CONTROL);
    }

    float findTolerance(List<String> lines) {
        Optional<String> lineObjFun = lines.stream()
                .filter(line -> linesThatConstains("OBJFUN", line))
                .findFirst();

        if (lineObjFun.isPresent()) {
            return getFloatValueAt(INDEX_OF_TOLERANCE, lineObjFun);
        }
        else {
            return 9999.0f;
        }
    }

    float getFloatValueAt(int index, Optional<String> lineToParse) {
        return getFloatValueAt(index, lineToParse.get());
    }

    float getFloatValueAt(int index, String lineToParse) {
        return Float.parseFloat(splitToList(lineToParse).get(index));
    }

    boolean linesThatConstains(String match, String line) {
        return splitToList(line).contains(match);
    }

    private List<String> splitToList(String line) {
        return Lists.newArrayList(Splitter.on(SEPARATOR).trimResults().split(line));
    }

    private List<String> readLinesOf(Path resultPath) {
        List<String> lines = Lists.newArrayList();
        File file = new File(resultPath.toUri());
        try {
            lines.addAll(FileUtils.readLines(file, "UTF-8"));
        } catch (IOException e) {
            LOG.error("Cannot read Ipso result file {} - {}", resultPath, e.getMessage());
        }finally {
            return lines;
        }
    }

    boolean isNullOrNotExists(Path path) {
        return path == null || !Files.exists(path);
    }

    /**
     * Resolve 'ipso'.out path form 'ipso'.csv  file path
     * @return Ipso log path .out form Ipso CSV  file path or null if the new path does not exist
     */
    Path resolveOutPathFrom(Path path) {
        return Paths.get(FilenameUtils.removeExtension(path.toString()) + ".out");
    }
}
