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

import com.powsybl.security.SecurityAnalyzer.Format;

import eu.itesla_project.security.rest.api.impl.FilePart;

public final class Utils {

    private Utils() {
        super();
    }

    private static final Logger LOGGER = LoggerFactory.getLogger(Utils.class);

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
}
