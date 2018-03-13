/**
 * Copyright (c) 2017, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package eu.itesla_project.security.rest.api.impl;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.StreamingOutput;

import org.apache.commons.io.IOUtils;
import org.jboss.resteasy.plugins.providers.multipart.InputPart;
import org.jboss.resteasy.plugins.providers.multipart.MultipartFormDataInput;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.powsybl.action.dsl.ActionDb;
import com.powsybl.action.dsl.ActionDslLoader;
import com.powsybl.action.simulator.ActionSimulator;
import com.powsybl.action.simulator.loadflow.LoadFlowActionSimulator;
import com.powsybl.action.simulator.loadflow.LoadFlowActionSimulatorObserver;
import com.powsybl.action.simulator.tools.AbstractSecurityAnalysisResultBuilder;
import com.powsybl.contingency.ContingenciesProvider;
import com.powsybl.contingency.Contingency;
import com.powsybl.contingency.EmptyContingencyListProvider;
import com.powsybl.iidm.import_.Importers;
import com.powsybl.iidm.network.Network;
import com.powsybl.security.LimitViolationFilter;
import com.powsybl.security.LimitViolationType;
import com.powsybl.security.SecurityAnalysisParameters;
import com.powsybl.security.SecurityAnalysisResult;
import com.powsybl.security.SecurityAnalyzer;
import com.powsybl.security.SecurityAnalyzer.Format;
import com.powsybl.security.converter.SecurityAnalysisResultExporters;
import com.powsybl.security.json.JsonSecurityAnalysisParameters;

import eu.itesla_project.security.rest.api.SecurityAnalysisService;
import eu.itesla_project.security.rest.api.impl.utils.Utils;

/**
 *
 * @author Giovanni Ferrari <giovanni.ferrari at techrain.it>
 */
public class SecurityAnalysisServiceImpl implements SecurityAnalysisService {

    private static final Logger LOGGER = LoggerFactory.getLogger(SecurityAnalysisServiceImpl.class);

    @Override
    public Response analyze(MultipartFormDataInput form) {
        Objects.requireNonNull(form);

        try {
            Map<String, List<InputPart>> formParts = form.getFormDataMap();

            Format format = Utils.getFormat(formParts);
            if (format == null) {
                return Response.status(Status.BAD_REQUEST).entity("Missing required format parameter").build();
            }
            String limitTypes = null;
            List<InputPart> limitParts = formParts.get("limit-types");
            if (limitParts != null) {
                limitTypes = Utils.getParameter(limitParts);
            }

            Set<LimitViolationType> limitViolationTypes;
            try {
                limitViolationTypes = (limitTypes != null && !limitTypes.equals("")) ? Arrays
                        .stream(limitTypes.split(",")).map(LimitViolationType::valueOf).collect(Collectors.toSet())
                        : EnumSet.allOf(LimitViolationType.class);
            } catch (IllegalArgumentException ie) {
                return Response.status(Status.BAD_REQUEST).entity("Wrong limit-types parameter").build();
            }

            LimitViolationFilter limitViolationFilter = new LimitViolationFilter(limitViolationTypes);

            FilePart caseFile = formParts.get("case-file") != null ? Utils.getFilePart(formParts.get("case-file"))
                    : null;

            if (caseFile == null) {
                return Response.status(Status.BAD_REQUEST).entity("Missing required case-file parameter").build();
            }
            Network network = Importers.loadNetwork(caseFile.getFilename(), caseFile.getInputStream());

            FilePart contingencies = formParts.get("contingencies-file") != null
                    ? Utils.getFilePart(formParts.get("contingencies-file"))
                    : null;

            FilePart parametersFile = formParts.get("parameters-file") != null
                    ? Utils.getFilePart(formParts.get("parameters-file"))
                    : null;

            SecurityAnalysisParameters parameters = SecurityAnalysisParameters.load();
            if (parametersFile != null) {
                JsonSecurityAnalysisParameters.update(parameters, parametersFile.getInputStream());
            }
            SecurityAnalysisResult result = analyze(network, contingencies, limitViolationFilter, parameters);

            return Response.ok(toStream(result, network, format))
                    .header("Content-Type", format.equals(Format.JSON) ? MediaType.APPLICATION_JSON : "text/csv")
                    .build();
        } catch (IOException e) {
            LOGGER.error("Error", e);
            return Response.serverError().build();
        }
    }

    public SecurityAnalysisResult analyze(Network network, FilePart contingencies,
            LimitViolationFilter limitViolationFilter, SecurityAnalysisParameters parameters) {
        ContingenciesProvider contingenciesProvider = (contingencies != null && contingencies.getInputStream() != null)
                ? Utils.getContingenciesProviderFactory().create(contingencies.getInputStream())
                : new EmptyContingencyListProvider();
        SecurityAnalyzer analyzer = new SecurityAnalyzer(limitViolationFilter, Utils.getLocalComputationManager(), 0);
        return analyzer.analyze(network, contingenciesProvider, parameters);
    }

    private StreamingOutput toStream(SecurityAnalysisResult result, Network network, Format format) {
        return new StreamingOutput() {

            @Override
            public void write(OutputStream out) throws IOException {
                Objects.requireNonNull(out);
                try (Writer wr = new OutputStreamWriter(out, StandardCharsets.UTF_8)) {
                    SecurityAnalysisResultExporters.export(result, wr, format.toString());
                }
            }
        };
    }

    @Override
    public Response actionSimulator(MultipartFormDataInput form) {
        Objects.requireNonNull(form);
        try {
            Map<String, List<InputPart>> formParts = form.getFormDataMap();
            Format format = Utils.getFormat(formParts);

            if (format == null) {
                return Response.status(Status.BAD_REQUEST).entity("Missing required format parameter").build();
            }

            FilePart caseFile = formParts.get("case-file") != null ? Utils.getFilePart(formParts.get("case-file"))
                    : null;
            if (caseFile == null) {
                return Response.status(Status.BAD_REQUEST).entity("Missing required case-file parameter").build();
            }

            FilePart dslFile = formParts.get("dsl-file") != null ? Utils.getFilePart(formParts.get("dsl-file")) : null;
            if (dslFile == null) {
                return Response.status(Status.BAD_REQUEST).entity("Missing required dsl-file parameter").build();
            }

            Network network = Importers.loadNetwork(caseFile.getFilename(), caseFile.getInputStream());

            List<String> contingencies = Collections.emptyList();

            StringWriter writer = new StringWriter();
            IOUtils.copy(dslFile.getInputStream(), writer, StandardCharsets.UTF_8);
            String dslAsString = writer.toString();

            // load actions from Groovy DSL
            ActionDb actionDb = new ActionDslLoader(dslAsString).load(network);

            if (contingencies.isEmpty()) {
                contingencies = actionDb.getContingencies().stream().map(Contingency::getId)
                        .collect(Collectors.toList());
            }

            List<LoadFlowActionSimulatorObserver> observers = new ArrayList<>();
            AbstractSecurityAnalysisResultBuilderImpl loadFlowActionSimulatorObserver = new AbstractSecurityAnalysisResultBuilderImpl();
            observers.add(loadFlowActionSimulatorObserver);

            FilePart parametersFile = formParts.get("parameters-file") != null
                    ? Utils.getFilePart(formParts.get("parameters-file"))
                    : null;
            InputStream parametersInputStream = null;
            if (parametersFile != null) {
                parametersInputStream = parametersFile.getInputStream();
            }
            // action simulator
            //TODO call LoadFlowActionSimulator with parametersInputStream
            ActionSimulator actionSimulator = new LoadFlowActionSimulator(network, Utils.getLocalComputationManager(),
                    Utils.getActionSimulatorConfig(), observers);

            // start simulator
            actionSimulator.start(actionDb, contingencies);

            SecurityAnalysisResult securityAnalysisResult = loadFlowActionSimulatorObserver.getResult();

            return Response.ok(toStream(securityAnalysisResult, network, format))
                    .header("Content-Type", format.equals(Format.JSON) ? MediaType.APPLICATION_JSON : "text/csv")
                    .build();

        } catch (Exception e) {
            return Response.status(Status.INTERNAL_SERVER_ERROR).entity("Internal server error").build();
        }
    }

    private static class AbstractSecurityAnalysisResultBuilderImpl extends AbstractSecurityAnalysisResultBuilder {

        private SecurityAnalysisResult result;

        @Override
        public void onFinalStateResult(SecurityAnalysisResult result) {
            this.setResult(result);
        }

        public SecurityAnalysisResult getResult() {
            return result;
        }

        public void setResult(SecurityAnalysisResult result) {
            this.result = result;
        }

    }

}
