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
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.StreamingOutput;

import org.jboss.resteasy.plugins.providers.multipart.InputPart;
import org.jboss.resteasy.plugins.providers.multipart.MultipartFormDataInput;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.powsybl.commons.config.ComponentDefaultConfig;
import com.powsybl.computation.local.LocalComputationManager;
import com.powsybl.contingency.ContingenciesProvider;
import com.powsybl.contingency.ContingenciesProviderFactory;
import com.powsybl.contingency.EmptyContingencyListProvider;
import com.powsybl.iidm.import_.Importers;
import com.powsybl.iidm.network.Network;
import com.powsybl.security.LimitViolationFilter;
import com.powsybl.security.LimitViolationType;
import com.powsybl.security.SecurityAnalysisResult;
import com.powsybl.security.SecurityAnalyzer;
import com.powsybl.security.SecurityAnalyzer.Format;
import com.powsybl.security.converter.SecurityAnalysisResultExporters;

import eu.itesla_project.security.rest.api.SecurityAnalysisService;

/**
 *
 * @author Giovanni Ferrari <giovanni.ferrari at techrain.it>
 */
public class SecurityAnalysisServiceImpl implements SecurityAnalysisService {

    private static final Logger LOGGER = LoggerFactory.getLogger(SecurityAnalysisServiceImpl.class);
    private final ContingenciesProviderFactory contingenciesProviderFactory;

    public SecurityAnalysisServiceImpl() {
        ComponentDefaultConfig defaultConfig = ComponentDefaultConfig.load();
        this.contingenciesProviderFactory = defaultConfig.newFactoryImpl(ContingenciesProviderFactory.class);
    }

    @Override
    public Response analyze(MultipartFormDataInput form) {
        Objects.requireNonNull(form);

        try {
            Map<String, List<InputPart>> formParts = form.getFormDataMap();

            Format format = null;
            List<InputPart> formatParts = formParts.get("format");
            if (formatParts != null) {
                try {
                    String outFormat = getParameter(formatParts);
                    if (outFormat != null) {
                        format = Format.valueOf(outFormat);
                    }
                } catch (IllegalArgumentException ie) {
                    return Response.status(Status.BAD_REQUEST).entity("Wrong format parameter").build();
                }
            }
            if (format == null) {
                return Response.status(Status.BAD_REQUEST).entity("Missing required format parameter").build();
            }
            String limitTypes = null;
            List<InputPart> limitParts = formParts.get("limit-types");
            if (limitParts != null) {
                limitTypes = getParameter(limitParts);
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

            FilePart caseFile = formParts.get("case-file") != null ? getFilePart(formParts.get("case-file")) : null;

            if (caseFile == null) {
                return Response.status(Status.BAD_REQUEST).entity("Missing required case-file parameter").build();
            }
            Network network = Importers.loadNetwork(caseFile.getFilename(), caseFile.getInputStream());

            FilePart contingencies = formParts.get("contingencies-file") != null
                    ? getFilePart(formParts.get("contingencies-file")) : null;


            SecurityAnalysisResult result = analyze(network, contingencies, limitViolationFilter);

            return Response.ok(toStream(result, network, format))
                    .header("Content-Type", format.equals(Format.JSON) ? MediaType.APPLICATION_JSON : "text/csv")
                    .build();
        } catch (IOException e) {
            LOGGER.error("Error", e);
            return Response.serverError().build();
        }
    }

    public SecurityAnalysisResult analyze(Network network, FilePart contingencies, LimitViolationFilter limitViolationFilter) {
        ContingenciesProvider contingenciesProvider = (contingencies != null && contingencies.getInputStream() != null)
                ? contingenciesProviderFactory.create(contingencies.getInputStream()) : new EmptyContingencyListProvider();
        SecurityAnalyzer analyzer = new SecurityAnalyzer(limitViolationFilter, LocalComputationManager.getDefault(), 0);
        return analyzer.analyze(network, contingenciesProvider);
    }

    private StreamingOutput toStream(SecurityAnalysisResult result, Network network, Format format) {
        return new StreamingOutput() {

            @Override
            public void write(OutputStream out) throws IOException, WebApplicationException {
                Objects.requireNonNull(out);
                try (Writer wr = new OutputStreamWriter(out, StandardCharsets.UTF_8)) {
                    SecurityAnalysisResultExporters.export(result, network, wr, format.toString());
                }
            }
        };
    }

    private String getParameter(List<InputPart> parts) {
        try {
            return parts.stream()
                    .filter(Objects::nonNull)
                    .findFirst()
                    .get()
                    .getBodyAsString();
        } catch (IOException e) {
            LOGGER.error("Error reading parameter", e);
        }
        return null;
    }


    private FilePart getFilePart(List<InputPart> parts) throws IOException {
        Objects.requireNonNull(parts);

        for (InputPart inputPart : parts) {
            String disposition = inputPart.getHeaders().getFirst("Content-Disposition");
            if (disposition != null) {
                Optional<String> filenameHeader = Arrays.stream(disposition.split(";"))
                        .filter(n -> n.trim().startsWith("filename")).findFirst();
                if (filenameHeader.isPresent()) {
                    String[] filenameTokens = filenameHeader.get().split("=");
                    if (filenameTokens.length > 1) {
                        String filename = filenameTokens[1].trim().replaceAll("\"", "");
                        if (!filename.isEmpty()) {
                            return new FilePart(filename, inputPart.getBody(InputStream.class, null));
                        }
                    }
                }
            }
        }
        return null;
    }

}
