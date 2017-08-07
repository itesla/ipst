/**
 * Copyright (c) 2017, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package eu.itesla_project.security.rest.api.impl;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
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

import eu.itesla_project.commons.io.WorkingDirectory;
import eu.itesla_project.commons.io.table.CsvTableFormatterFactory;
import eu.itesla_project.security.LimitViolationFilter;
import eu.itesla_project.security.LimitViolationType;
import eu.itesla_project.security.Security;
import eu.itesla_project.security.SecurityAnalysisResult;
import eu.itesla_project.security.SecurityAnalyzer;
import eu.itesla_project.security.SecurityAnalyzer.Format;
import eu.itesla_project.security.json.SecurityAnalysisResultSerializer;
import eu.itesla_project.security.rest.api.SecurityAnalysisService;

/**
 *
 * @author Giovanni Ferrari <giovanni.ferrari at techrain.it>
 */
public class SecurityAnalysisServiceImpl implements SecurityAnalysisService {

    private static final Logger LOGGER = LoggerFactory.getLogger(SecurityAnalysisServiceImpl.class);

    private final SecurityAnalyzer analyzer;

    public SecurityAnalysisServiceImpl(SecurityAnalyzer securityAnalyzer) {
        this.analyzer = Objects.requireNonNull(securityAnalyzer);
    }

    @Override
    public Response analyze(MultipartFormDataInput form) {
        Objects.requireNonNull(form);

        try (WorkingDirectory workdir = new WorkingDirectory(Paths.get(System.getProperty("java.io.tmpdir")), "secws", false)) {
            Map<String, List<InputPart>> formParts = form.getFormDataMap();

            Format format = null;
            List<InputPart> formatParts = formParts.get("format");
            if (formatParts != null) {
                try {
                    String out_format = getParameter(formatParts);
                    if(out_format != null){
                        format = Format.valueOf(out_format);
                    }
                } catch (java.lang.IllegalArgumentException ie) {
                    return Response.status(Status.BAD_REQUEST).entity("Wrong format parameter").build();
                }
            }
            if(format == null)
            {
                return Response.status(Status.BAD_REQUEST).entity("Missing required format parameter").build();
            }
            String limitTypes = null;
            List<InputPart> limitParts = formParts.get("limit-types");
            if (limitParts != null) {
                limitTypes = getParameter(limitParts);
            }

            Set<LimitViolationType> limitViolationTypes = null;
            try {
                limitViolationTypes = (limitTypes != null && !limitTypes.equals("")) ? Arrays
                        .stream(limitTypes.split(",")).map(LimitViolationType::valueOf).collect(Collectors.toSet())
                        : EnumSet.allOf(LimitViolationType.class);
            } catch (java.lang.IllegalArgumentException ie) {
                return Response.status(Status.BAD_REQUEST).entity("Wrong limit-types parameter").build();
            }

            LimitViolationFilter limitViolationFilter = new LimitViolationFilter(limitViolationTypes);

            FilePart caseFile = formParts.get("case-file") != null ? getFilePart(formParts.get("case-file")) : null;

            if (caseFile == null){
                return Response.status(Status.BAD_REQUEST).entity("Missing required case-file parameter").build();
            }

            FilePart contingencies = formParts.get("contingencies-file") != null
                    ? getFilePart( formParts.get("contingencies-file")) : null;

            SecurityAnalysisResult result = analyzer.analyze(caseFile.getFilename(), caseFile.getInputStream(), contingencies != null ? contingencies.getInputStream() : null);
            return Response.ok(toStream(result, limitViolationFilter, format))
                    .header("Content-Type", format.equals(Format.JSON) ? MediaType.APPLICATION_JSON : "text/csv")
                    .build();
        } catch (IOException e) {
            LOGGER.error("Error", e);
            return Response.serverError().build();
        }
    }

    private StreamingOutput toStream(SecurityAnalysisResult result, LimitViolationFilter limitViolationFilter,
            Format format) {
        return new StreamingOutput() {
            
            @Override
            public void write(OutputStream out) throws IOException, WebApplicationException {
                Objects.requireNonNull(out);
                if (format.equals(Format.JSON)) {
                    SecurityAnalysisResultSerializer.write(result, limitViolationFilter, out);
                } else {
                    try( Writer wr = new OutputStreamWriter(out, StandardCharsets.UTF_8)){
                        CsvTableFormatterFactory csvTableFormatterFactory = new CsvTableFormatterFactory();
                        Security.printPreContingencyViolations(result, wr, csvTableFormatterFactory, limitViolationFilter);
                        Security.printPostContingencyViolations(result, wr, csvTableFormatterFactory, limitViolationFilter);
                    }
                }
            }
        };
    }

    private String getParameter(List<InputPart> parts){
        try {
            return parts.stream().filter(Objects::nonNull).findFirst().get().getBodyAsString();
        } catch (IOException e) {
            LOGGER.error("Error reading parameter",e);
        }
        return null;
    }
    
    
    private FilePart getFilePart(List<InputPart> parts) throws IOException{
        Objects.requireNonNull(parts);

        for (InputPart inputPart : parts) {
            String disposition = inputPart.getHeaders().getFirst("Content-Disposition");
            if(disposition != null)
            {
                Optional<String> filename_header = Arrays.asList(disposition.split(";")).stream()
                        .filter(n -> n.trim().startsWith("filename")).findFirst();
                if (filename_header.isPresent()) {
                    String[] filename_tokens = filename_header.get().split("=");
                    if (filename_tokens.length > 1) {
                        String filename = filename_tokens[1].trim().replaceAll("\"", "");
                        if (!filename.equals("")) {
                            return new FilePart(filename, inputPart.getBody(InputStream.class, null));    
                        }
                    }
                }
            }
        }
        return null;
    }
    
    private class FilePart{
        
        private final String filename;
        private final InputStream inputStream;
        
        FilePart(String filename, InputStream content){
            this.filename = filename;
            this.inputStream = content;
        }

        public String getFilename() {
            return filename;
        }
        
        public InputStream getInputStream(){
            return inputStream;
        }
    }

}
