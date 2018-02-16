package eu.itesla_project.security.rest.api.impl.utils;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import org.jboss.resteasy.plugins.providers.multipart.InputPart;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.powsybl.action.simulator.loadflow.LoadFlowActionSimulatorConfig;
import com.powsybl.commons.PowsyblException;
import com.powsybl.commons.config.ComponentDefaultConfig;
import com.powsybl.computation.local.LocalComputationManager;
import com.powsybl.contingency.ContingenciesProviderFactory;
import com.powsybl.security.SecurityAnalyzer.Format;

import eu.itesla_project.security.rest.api.impl.FilePart;

public final class Utils {

    private static ComponentDefaultConfig componentDefaultConfig;
    private static LoadFlowActionSimulatorConfig actionSimulatorConfig;
    private static ContingenciesProviderFactory contingenciesProviderFactory;
    private static LocalComputationManager localComputationManager;
    private static final Logger LOGGER = LoggerFactory.getLogger(Utils.class);

    private Utils() {
        super();
    }

    public static void init() {
        componentDefaultConfig = ComponentDefaultConfig.load();
        actionSimulatorConfig = LoadFlowActionSimulatorConfig.load();
        contingenciesProviderFactory = componentDefaultConfig.newFactoryImpl(ContingenciesProviderFactory.class);
        try {
            localComputationManager = new LocalComputationManager();
        } catch (IOException e) {
            throw new PowsyblException(e);
        }
    }

    public static LocalComputationManager getLocalComputationManager() {
        try {
            if (localComputationManager == null) {
                localComputationManager = new LocalComputationManager();
            }
        } catch (IOException e) {
            throw new PowsyblException(e);
        }
        return localComputationManager;
    }

    public static Format getFormat(Map<String, List<InputPart>> formParts) {
        Format format = null;
        List<InputPart> formatParts = formParts.get("format");
        if (formatParts != null) {
            String outFormat = getParameter(formatParts);
            if (outFormat != null) {
                try {
                    format = Format.valueOf(outFormat);
                } catch (Exception e) {
                    format = null;
                }
            }
        }
        return format;
    }

    public static FilePart getFilePart(List<InputPart> parts) throws IOException {
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

    public static String getParameter(List<InputPart> parts) {
        try {
            return parts.stream().filter(Objects::nonNull).findFirst().get().getBodyAsString();
        } catch (IOException e) {
            LOGGER.error("Error reading parameter", e);
        }
        return null;
    }

    public static ComponentDefaultConfig getComponentDefaultConfig() {
        if (componentDefaultConfig == null) {
            componentDefaultConfig = ComponentDefaultConfig.load();
        }
        return componentDefaultConfig;
    }

    public static LoadFlowActionSimulatorConfig getActionSimulatorConfig() {
        if (actionSimulatorConfig == null) {
            actionSimulatorConfig = LoadFlowActionSimulatorConfig.load();
        }
        return actionSimulatorConfig;
    }

    public static ContingenciesProviderFactory getContingenciesProviderFactory() {
        if (contingenciesProviderFactory == null) {
            if (componentDefaultConfig == null) {
                componentDefaultConfig = ComponentDefaultConfig.load();
            }
            contingenciesProviderFactory = componentDefaultConfig.newFactoryImpl(ContingenciesProviderFactory.class);
        }
        return contingenciesProviderFactory;
    }

}
